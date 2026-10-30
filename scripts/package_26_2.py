#!/usr/bin/env python3
"""Build clean teammate downloads, without reading a player's launcher profile."""
import argparse
import hashlib
import json
from pathlib import Path
import shutil
import subprocess
import zipfile

from build_version import build_version
from fabric_26_2 import manifest, fetch_mod
from voicechat import control_options

ROOT = Path(__file__).resolve().parents[1]
JAR_NAME = f'skyblock-toolkit-{build_version()}+mc26.2.jar'
README = '''Skyblock Toolkit - Minecraft 26.2

Prism Launcher: Add Instance > Import > choose Full-Pack-Prism-26.2.zip.
Modrinth App: create an instance by importing Full-Pack-Modrinth-26.2.mrpack.
Then launch the new instance using your own Minecraft account.

Already use Fabric? Put just the toolkit jar in your mods folder with Minecraft
closed. You need Fabric Loader 0.19.5 or newer, Fabric API for 26.2 and Java 25.
In the Modrinth App, update Fabric Loader in the instance settings if needed.

Right Shift opens Toolkit. M opens Litematica. V sets up voice chat.
Hold Caps Lock to talk; F8 mutes your microphone.
Add skyblock.mc-complex.com in Multiplayer to play on Complex Skyblock.

The full pack includes the required mods. No personal player data is included.
Credits are in THIRD-PARTY.md; source code is in toolkit-source.
'''


def verify_toolkit(path):
    with zipfile.ZipFile(path) as archive:
        meta = json.loads(archive.read('fabric.mod.json'))
        if (archive.testzip() is not None or meta['id'] != 'skyblocktoolkit'
                or meta['version'] != build_version() or meta['depends']['minecraft'] != '~26.2'
                or meta['depends']['java'] != '>=25' or meta['depends']['fabricloader'] != '>=0.19.5'):
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

    prism = output / 'Full-Pack-Prism-26.2.zip'
    prism_files = {**common, **{'.minecraft/' + name: data for name, data in files.items()},
                   **{'.minecraft/mods/' + name: path.read_bytes() for name, path in dependencies.items()},
                   'instance.cfg': b'[General]\nConfigVersion=1.3\nInstanceType=OneSix\nname=Complex Skyblock Toolkit (26.2)\nOverrideJavaLocation=false\n',
                   'mmc-pack.json': json.dumps({'formatVersion': 1, 'components': [
                       {'uid': 'net.minecraft', 'version': spec['minecraft'], 'important': True},
                       {'uid': 'net.fabricmc.fabric-loader', 'version': spec['loader']}]}).encode()}
    write_archive(prism, prism_files)

    index = {'formatVersion': 1, 'game': 'minecraft', 'versionId': f'{build_version()}+mc26.2',
             'name': 'Complex Skyblock Toolkit (26.2)',
             'dependencies': {'minecraft': spec['minecraft'], 'fabric-loader': spec['loader']}, 'files': []}
    for mod in spec['mods']:
        data = dependencies[mod['filename']].read_bytes()
        index['files'].append({'path': 'mods/' + mod['filename'],
            'hashes': {'sha1': hashlib.sha1(data).hexdigest(), 'sha512': mod['sha512']},
            'env': {'client': 'required', 'server': 'unsupported'}, 'downloads': [mod['url']], 'fileSize': len(data)})
    mrpack = output / 'Full-Pack-Modrinth-26.2.mrpack'
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
    parser.add_argument('--source', type=Path, default=ROOT / f'build/skyblock-toolkit-{build_version()}.jar')
    args = parser.parse_args()
    package(args.output, args.source)
