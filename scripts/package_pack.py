#!/usr/bin/env python3
"""Unified packaging script for Prism and Modrinth packs across supported Minecraft versions."""
import argparse
from concurrent.futures import ThreadPoolExecutor
import hashlib
import json
from pathlib import Path
from build_version import build_version
import shutil
import subprocess
import urllib.request
import zipfile

from voicechat import control_options

ROOT = Path(__file__).resolve().parents[1]
CACHE = ROOT / '.local/modpack-cache'


def manifest(minecraft):
    manifest_path = ROOT / f'modpack/fabric-{minecraft}.json'
    if not manifest_path.is_file():
        raise FileNotFoundError(f'Missing manifest for Minecraft {minecraft}: {manifest_path}')
    return json.loads(manifest_path.read_text(encoding='utf-8'))


def fetch_file(url, path, checksum, algorithm='sha512'):
    if path.is_file() and hashlib.new(algorithm, path.read_bytes()).hexdigest() == checksum:
        return path
    path.parent.mkdir(parents=True, exist_ok=True)
    req = urllib.request.Request(url, headers={'User-Agent': 'SkyblockToolkit-Packager/1.0'})
    with urllib.request.urlopen(req, timeout=90) as resp:
        data = resp.read()
    if hashlib.new(algorithm, data).hexdigest() != checksum:
        raise ValueError(f'Checksum mismatch for {path.name}')
    temp = path.with_suffix(path.suffix + '.download')
    temp.write_bytes(data)
    temp.replace(path)
    return path


def fetch_dependencies(spec):
    cache_dir = CACHE / spec['minecraft'] / 'mods'
    results = {}
    with ThreadPoolExecutor(max_workers=6) as pool:
        futures = {
            pool.submit(fetch_file, mod['url'], cache_dir / mod['filename'], mod['sha512'], 'sha512'): mod['filename']
            for mod in spec['mods']
        }
        for future in futures:
            filename = futures[future]
            results[filename] = future.result()
    return results


def make_readme(minecraft, loader, java_ver):
    return f"""Skyblock Toolkit - Minecraft {minecraft}

Prism Launcher: Add Instance > Import > choose Full-Pack-Prism-{minecraft}.zip.
Modrinth App: Add Instance > Import > choose Full-Pack-Modrinth-{minecraft}.mrpack.
Then launch the new instance using your own Minecraft account.

Already use Fabric? Put just the toolkit jar in your mods folder with Minecraft closed.
Required: Fabric Loader {loader} or newer, Fabric API for {minecraft}, and Java {java_ver}.

Right Shift opens Toolkit. M opens Litematica. V sets up voice chat.
Hold Caps Lock to talk; F8 mutes your microphone.
Add skyblock.mc-complex.com in Multiplayer to play on Complex Skyblock.

The full pack includes the required mods. No personal player data is included.
Credits are in THIRD-PARTY.md; source code is in toolkit-source.
"""


def source_files():
    try:
        names = subprocess.check_output(
            ['git', 'ls-files', '--cached', '--others', '--exclude-standard', '-z'],
            cwd=ROOT
        ).decode().split('\0')
        return {name: ROOT / name for name in names if name and (ROOT / name).is_file()}
    except Exception:
        files = {}
        for p in (ROOT / 'src').rglob('*'):
            if p.is_file():
                files[p.relative_to(ROOT).as_posix()] = p
        return files


def write_archive(path, files):
    temp = path.with_suffix('.tmp')
    with zipfile.ZipFile(temp, 'w', compression=zipfile.ZIP_DEFLATED) as archive:
        for name, data in sorted(files.items()):
            if name.startswith('/') or '..' in Path(name).parts:
                raise ValueError(f'Unsafe archive path: {name}')
            archive.writestr(name, data)
    with zipfile.ZipFile(temp) as archive:
        if archive.testzip() or any(archive.read(name) != data for name, data in files.items()):
            raise ValueError(f'Pack verification failed for {path}')
    temp.replace(path)


def package(minecraft, toolkit_jar=None, output_dir=None):
    spec = manifest(minecraft)
    out = (output_dir or (ROOT / 'dist' / f'fabric-{minecraft}')).resolve()
    out.mkdir(parents=True, exist_ok=True)

    version = build_version()
    jar_name = f"skyblock-toolkit-{version}+mc{minecraft}.jar"
    if toolkit_jar is None:
        if minecraft == '26.2':
            candidate = ROOT / f'build/skyblock-toolkit-{version}.jar'
        else:
            candidate = ROOT / f'versions/{minecraft}/build/libs/{jar_name}'
        if candidate.is_file():
            toolkit_jar = candidate

    if toolkit_jar and toolkit_jar.is_file():
        with zipfile.ZipFile(toolkit_jar) as archive:
            metadata = json.loads(archive.read('fabric.mod.json'))
            if metadata['id'] != 'skyblocktoolkit' or metadata['depends']['minecraft'] != '~' + minecraft or archive.testzip():
                raise ValueError('Toolkit jar does not match the requested Minecraft pack')
            version = metadata['version']
            jar_name = f"skyblock-toolkit-{version}+mc{minecraft}.jar"

    public_jar = out / jar_name
    if toolkit_jar and toolkit_jar.is_file():
        shutil.copyfile(toolkit_jar, public_jar)

    readme_text = make_readme(spec['minecraft'], spec['loader'], spec['java'])
    (out / f'INSTALL-{minecraft}.txt').write_text(readme_text, encoding='utf-8')
    (out / f'fabric-{minecraft}.json').write_text(json.dumps(spec, indent=2) + '\n', encoding='utf-8')

    dependencies = fetch_dependencies(spec)
    sources = source_files()

    common = {
        'READ-ME.txt': readme_text.encode('utf-8'),
        'THIRD-PARTY.md': (ROOT / 'THIRD-PARTY.md').read_bytes() if (ROOT / 'THIRD-PARTY.md').is_file() else b'',
    }
    common.update({'toolkit-source/' + name: path.read_bytes() for name, path in sources.items()})

    options = control_options('resourcePacks:["vanilla","skyblocktoolkit:clear_harvest"]\n', minecraft=minecraft).encode('utf-8')
    files = {'options.txt': options}
    if public_jar.is_file():
        files['mods/' + jar_name] = public_jar.read_bytes()

    # 1. Prism ZIP
    prism_zip = out / f'Full-Pack-Prism-{minecraft}.zip'
    prism_files = {
        **common,
        **{'.minecraft/' + name: data for name, data in files.items()},
        **{'.minecraft/mods/' + name: path.read_bytes() for name, path in dependencies.items()},
        'instance.cfg': f'[General]\nConfigVersion=1.3\nInstanceType=OneSix\nname=Complex Skyblock Toolkit ({minecraft})\nOverrideJavaLocation=false\n'.encode('utf-8'),
        'mmc-pack.json': json.dumps({
            'formatVersion': 1,
            'components': [
                {'uid': 'net.minecraft', 'version': spec['minecraft'], 'important': True},
                {'uid': 'net.fabricmc.fabric-loader', 'version': spec['loader']}
            ]
        }, indent=2).encode('utf-8')
    }
    write_archive(prism_zip, prism_files)

    # 2. Modrinth MRPACK
    mrpack_file = out / f'Full-Pack-Modrinth-{minecraft}.mrpack'
    index = {
        'formatVersion': 1,
        'game': 'minecraft',
        'versionId': f'{version}+mc{minecraft}',
        'name': f'Complex Skyblock Toolkit ({minecraft})',
        'dependencies': {
            'minecraft': spec['minecraft'],
            'fabric-loader': spec['loader']
        },
        'files': []
    }
    for mod in spec['mods']:
        mod_bytes = dependencies[mod['filename']].read_bytes()
        index['files'].append({
            'path': 'mods/' + mod['filename'],
            'hashes': {
                'sha1': hashlib.sha1(mod_bytes).hexdigest(),
                'sha512': mod['sha512']
            },
            'env': {'client': 'required', 'server': 'unsupported'},
            'downloads': [mod['url']],
            'fileSize': len(mod_bytes)
        })

    mrpack_files = {
        'modrinth.index.json': json.dumps(index, indent=2).encode('utf-8'),
        **{'overrides/' + name: data for name, data in {**common, **files}.items()}
    }
    write_archive(mrpack_file, mrpack_files)

    checksum_targets = [prism_zip, mrpack_file, out / f'INSTALL-{minecraft}.txt', out / f'fabric-{minecraft}.json']
    if public_jar.is_file():
        checksum_targets.insert(0, public_jar)
    (out / 'SHA256SUMS.txt').write_text(
        ''.join(hashlib.sha256(p.read_bytes()).hexdigest() + '  ' + p.name + '\n' for p in checksum_targets),
        encoding='utf-8'
    )
    print(f'Created and byte-verified {minecraft} packages in {out}')
    return out


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--minecraft', default='1.21.11', help='Minecraft version (e.g. 1.21.11, 26.2, 1.20.4, 1.20.1)')
    parser.add_argument('--toolkit-jar', type=Path, default=None)
    parser.add_argument('--output', type=Path, default=None)
    args = parser.parse_args()
    package(args.minecraft, args.toolkit_jar, args.output)
