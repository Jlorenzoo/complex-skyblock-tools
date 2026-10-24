#!/usr/bin/env python3
"""Build clean teammate downloads, without reading a player's launcher profile."""
import argparse
import hashlib
import json
from pathlib import Path
import shutil
import subprocess
import zipfile

from fabric_26_2 import manifest, fetch_mod
from voicechat import control_options

ROOT = Path(__file__).resolve().parents[1]
JAR_NAME = 'skyblock-toolkit-1.1.0+mc26.2.jar'
README = '''Skyblock Toolkit 1.1.0 — Minecraft 26.2 / Fabric

Requires Java 25. Use a separate Minecraft 26.2 instance.

Prism Launcher: Add Instance > Import > select Complex-Skyblock-Prism-1.1.0+mc26.2.zip.
Modrinth App or Prism: import Complex-Skyblock-1.1.0+mc26.2.mrpack.
Existing Fabric profile: install Fabric Loader 0.19.5 and the six dependencies
listed in fabric-26.2.json, then put skyblock-toolkit-1.1.0+mc26.2.jar in mods.
Do not install this jar or these dependencies into Minecraft 1.21.11.

Includes Fabric API, Simple Voice Chat, Litematica, MaLiLib, ChatShot and YACL.
Clear Harvest crop visibility textures are built into the toolkit and enabled.
Right Shift opens Toolkit; M opens Litematica. V opens voice chat setup.
Voice: Caps Lock push-to-talk, Right Alt whisper, F8 mute, N disable, H hide icons.
Select your own microphone/speakers and complete voice onboarding yourself.
Add skyblock.mc-complex.com in Multiplayer to join Complex Skyblock.

Clean defaults only; no accounts, server lists, worlds, chat, personal settings,
earnings history, microphone choices or credentials are included.
See THIRD-PARTY.md for dependency credits. Toolkit source is in toolkit-source.
'''


def verify_toolkit(path):
    with zipfile.ZipFile(path) as archive:
        meta = json.loads(archive.read('fabric.mod.json'))
        if (archive.testzip() is not None or meta['id'] != 'skyblocktoolkit'
                or meta['version'] != '1.1.0' or meta['depends']['minecraft'] != '~26.2'
                or meta['depends']['java'] != '>=25'):
            raise ValueError('Expected the Minecraft 26.2 / Java 25 toolkit jar')


def source_files():
    # Git's ignore rules exclude private workstation files and generated output.
    names = subprocess.check_output(['git', 'ls-files', '--cached', '--others', '--exclude-standard', '-z'], cwd=ROOT).decode().split('\0')
    return {name: ROOT / name for name in names if name and (ROOT / name).is_file()}


def package(output, toolkit, sources=None):
    verify_toolkit(toolkit)
    spec = manifest()
    dependencies = {mod['filename']: fetch_mod(mod) for mod in spec['mods']}
    output.mkdir(parents=True, exist_ok=True)
    public_jar = output / JAR_NAME
    shutil.copyfile(toolkit, public_jar)
    (output / 'INSTALL-26.2.txt').write_text(README, encoding='utf-8')
    (output / 'fabric-26.2.json').write_text(json.dumps(spec, indent=2) + '\n')
    sources = source_files() if sources is None else sources
    common = {'READ-ME.txt': README.encode(), 'THIRD-PARTY.md': (ROOT / 'THIRD-PARTY.md').read_bytes()}
    common.update({'toolkit-source/' + name: path.read_bytes() for name, path in sources.items()})
    options = control_options('resourcePacks:["vanilla","skyblocktoolkit:clear_harvest"]\n', minecraft='26.2').encode()
    files = {'mods/' + JAR_NAME: toolkit.read_bytes(), 'options.txt': options}

    prism = output / 'Complex-Skyblock-Prism-1.1.0+mc26.2.zip'
    prism_files = {**common, **{'.minecraft/' + name: data for name, data in files.items()},
                   **{'.minecraft/mods/' + name: path.read_bytes() for name, path in dependencies.items()},
                   'instance.cfg': b'[General]\nConfigVersion=1.3\nInstanceType=OneSix\nname=Complex Skyblock Toolkit (26.2)\nOverrideJavaLocation=false\n',
                   'mmc-pack.json': json.dumps({'formatVersion': 1, 'components': [
                       {'uid': 'net.minecraft', 'version': spec['minecraft'], 'important': True},
                       {'uid': 'net.fabricmc.fabric-loader', 'version': spec['loader']}]}).encode()}
    write_archive(prism, prism_files)

    index = {'formatVersion': 1, 'game': 'minecraft', 'versionId': '1.1.0+mc26.2',
             'name': 'Complex Skyblock Toolkit (26.2)',
             'dependencies': {'minecraft': spec['minecraft'], 'fabric-loader': spec['loader']}, 'files': []}
    for mod in spec['mods']:
        data = dependencies[mod['filename']].read_bytes()
        index['files'].append({'path': 'mods/' + mod['filename'],
            'hashes': {'sha1': hashlib.sha1(data).hexdigest(), 'sha512': mod['sha512']},
            'env': {'client': 'required', 'server': 'unsupported'}, 'downloads': [mod['url']], 'fileSize': len(data)})
    mrpack = output / 'Complex-Skyblock-1.1.0+mc26.2.mrpack'
    write_archive(mrpack, {'modrinth.index.json': json.dumps(index, indent=2).encode(),
        **{'overrides/' + name: data for name, data in {**common, **files}.items()}})
    checksum_files = [public_jar, prism, mrpack, output / 'INSTALL-26.2.txt', output / 'fabric-26.2.json']
    (output / 'SHA256SUMS.txt').write_text(''.join(hashlib.sha256(p.read_bytes()).hexdigest() + '  ' + p.name + '\n' for p in checksum_files))
    print('Created and byte-verified 26.2 jar, Prism ZIP and Modrinth pack in ' + str(output))


def write_archive(path, files):
    temporary = path.with_suffix('.tmp')
    with zipfile.ZipFile(temporary, 'w', compression=zipfile.ZIP_DEFLATED) as archive:
        for name, data in sorted(files.items()):
            if name.startswith('/') or '..' in Path(name).parts:
                raise ValueError('Unsafe archive path: ' + name)
            archive.writestr(name, data)
    with zipfile.ZipFile(temporary) as archive:
        if archive.testzip() or any(archive.read(name) != data for name, data in files.items()):
            raise ValueError('Pack verification failed')
    temporary.replace(path)


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--output', type=Path, default=ROOT / 'dist/fabric-26.2')
    parser.add_argument('--source', type=Path, default=ROOT / 'build/skyblock-toolkit-1.1.0.jar')
    args = parser.parse_args()
    package(args.output, args.source)
