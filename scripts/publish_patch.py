"""Publish a verified build where existing in-game updaters can discover it.

Called only after CI verification on main. Minecraft lines have separate tags so
one failed compatibility build cannot advertise a missing or incompatible jar.
"""
import argparse
import hashlib
import json
from pathlib import Path
import subprocess
import zipfile
from build_version import build_version


def publish(minecraft):
    version = build_version()
    root = Path(__file__).resolve().parents[1]
    source = (root / f'versions/{minecraft}/build/libs/skyblock-toolkit-{version}+mc{minecraft}.jar'
              if minecraft == '1.21.11' else root / f'build/skyblock-toolkit-{version}.jar')
    with zipfile.ZipFile(source) as archive:
        metadata = json.loads(archive.read('fabric.mod.json'))
        if (metadata['id'] != 'skyblocktoolkit' or metadata['version'] != version
                or metadata['depends']['minecraft'] != '~' + minecraft or archive.testzip()):
            raise ValueError('Release jar metadata or integrity mismatch')
    output = root / 'dist' / f'patch-{minecraft}'
    output.mkdir(parents=True, exist_ok=True)
    asset = output / f'skyblock-toolkit-{version}+mc{minecraft}.jar'
    asset.write_bytes(source.read_bytes())
    checksum = output / f'SHA256SUMS-mc{minecraft}.txt'
    checksum.write_text(hashlib.sha256(asset.read_bytes()).hexdigest() + '  ' + asset.name + '\n')
    commit = subprocess.check_output(['git', 'rev-parse', 'HEAD'], cwd=root, text=True).strip()
    tag = f'v{version}-mc{minecraft}'
    existing = subprocess.run(['gh', 'release', 'view', tag, '--json', 'isDraft'], cwd=root, capture_output=True, text=True)
    if existing.returncode == 0:
        if not json.loads(existing.stdout)['isDraft']:
            raise RuntimeError('This patch is already public; refusing to overwrite its assets')
    else:
        subprocess.run(['gh', 'release', 'create', tag, '--draft', '--target', commit,
                        '--title', f'Skyblock Toolkit {version} (Minecraft {minecraft})',
                        '--notes', f'Automatic patch for Minecraft {minecraft}.\n\nSource commit: {commit}\n\n'
                        'Includes all toolkit changes in this commit. Use the in-game update button, then restart Minecraft. '
                        'Player settings and history are preserved.'], cwd=root, check=True)
    subprocess.run(['gh', 'release', 'upload', tag, str(asset), str(checksum), '--clobber'], cwd=root, check=True)
    # Clients ignore drafts, so the jar and checksum are present before advertising.
    subprocess.run(['gh', 'release', 'edit', tag, '--draft=false', '--latest=' + ('true' if minecraft == '26.2' else 'false')], cwd=root, check=True)


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--minecraft', required=True, choices=('1.21.11', '26.2'))
    publish(parser.parse_args().minecraft)
