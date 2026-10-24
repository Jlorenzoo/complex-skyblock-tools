"""Portable, checksum-verified dependencies for the shared Minecraft 26.2 sources."""
from concurrent.futures import ThreadPoolExecutor
import hashlib
import json
import os
from pathlib import Path
import platform
import shutil
import urllib.request

ROOT = Path(__file__).resolve().parents[1]
CACHE = ROOT / '.local/fabric-26.2'


def manifest():
    return json.loads((ROOT / 'modpack/fabric-26.2.json').read_text())


def fetch(url, path, checksum, algorithm='sha1', existing=None):
    """Verify cached and downloaded bytes; reuse launcher files only when identical."""
    for candidate in (path, existing):
        if candidate and candidate.is_file():
            if hashlib.new(algorithm, candidate.read_bytes()).hexdigest() == checksum:
                return candidate
    path.parent.mkdir(parents=True, exist_ok=True)
    with urllib.request.urlopen(url, timeout=90) as response:
        data = response.read()
    if hashlib.new(algorithm, data).hexdigest() != checksum:
        raise ValueError('Dependency checksum mismatch: ' + path.name)
    temporary = path.with_suffix(path.suffix + '.download')
    temporary.write_bytes(data)
    temporary.replace(path)
    return path


def fetch_mod(spec):
    return fetch(spec['url'], CACHE / 'mods' / spec['filename'], spec['sha512'], 'sha512')


def allowed(library):
    current = {'Windows': 'windows', 'Darwin': 'osx'}.get(platform.system(), 'linux')
    result = not library.get('rules')
    for rule in library.get('rules', []):
        os_rule = rule.get('os', {})
        if os_rule.get('name', current) == current:
            import re
            if 'arch' in os_rule and not re.search(os_rule['arch'], platform.machine()):
                continue
            if 'version' in os_rule and not re.search(os_rule['version'], platform.version()):
                continue
            if rule.get('features'):
                continue
            result = rule['action'] == 'allow'
    return result


def prepare(local):
    spec = manifest()
    descriptor = fetch(spec['vanilla']['url'], CACHE / 'versions/26.2/26.2.json', spec['vanilla']['sha1'])
    version = json.loads(descriptor.read_text())
    client = version['downloads']['client']
    existing_client = Path(local['minecraft_jar']) if local.get('minecraft_jar') else None
    minecraft = fetch(client['url'], CACHE / 'versions/26.2/26.2.jar', client['sha1'], existing=existing_client)
    with urllib.request.urlopen(f"https://meta.fabricmc.net/v2/versions/loader/26.2/{spec['loader']}/profile/json", timeout=30) as response:
        loader = json.load(response)
    existing_libraries = Path(local['libraries_dir']) if local.get('libraries_dir') else None

    def library_file(library):
        artifact = library.get('downloads', {}).get('artifact')
        if artifact:
            relative, url, checksum = artifact['path'], artifact['url'], artifact['sha1']
        else:
            group, name, revision = library['name'].split(':')[:3]
            relative = f"{group.replace('.', '/')}/{name}/{revision}/{name}-{revision}.jar"
            url = library['url'] + relative
            with urllib.request.urlopen(url + '.sha1', timeout=30) as response:
                checksum = response.read().decode().strip().split()[0]
        return fetch(url, CACHE / 'libraries' / relative, checksum,
                     existing=existing_libraries / relative if existing_libraries else None)

    libraries = [lib for lib in version['libraries'] + loader['libraries'] if allowed(lib)]
    with ThreadPoolExecutor(max_workers=8) as pool:
        paths = list(pool.map(library_file, libraries))
    loader_jar = next(p for p in paths if p.name == f"fabric-loader-{spec['loader']}.jar")
    api = fetch_mod(next(m for m in spec['mods'] if m['project'] == 'fabric-api'))
    javac = os.environ.get('JAVAC') or local.get('javac')
    if not javac or not Path(javac).is_file():
        java_home = os.environ.get('JAVA_HOME')
        javac = str(Path(java_home) / 'bin' / ('javac.exe' if os.name == 'nt' else 'javac')) if java_home else shutil.which('javac')
    if not javac:
        raise RuntimeError('Install JDK 25 and set JAVA_HOME or JAVAC.')
    return {'minecraft_jar': str(minecraft), 'fabric_api_jar': str(api), 'loader_jar': str(loader_jar),
            'javac': javac, 'libraries_dir': str(CACHE / 'libraries'), 'library_jars': list(map(str, paths))}
