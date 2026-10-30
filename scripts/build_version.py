"""One public patch number per commit, shared by local and CI builds.

Full Git history is required: shallow clones would silently reuse old versions.
The major/minor line comes from fabric.mod.json; the patch is the commit count.
"""
import json
from pathlib import Path
import subprocess

ROOT = Path(__file__).resolve().parents[1]


def build_version():
    if subprocess.check_output(['git', 'rev-parse', '--is-shallow-repository'], cwd=ROOT, text=True).strip() != 'false':
        raise RuntimeError('Fetch full Git history before building: git fetch --unshallow')
    base = json.loads((ROOT / 'src/main/resources/fabric.mod.json').read_text())['version']
    major, minor, _ = base.split('.')
    patch = subprocess.check_output(['git', 'rev-list', '--count', 'HEAD'], cwd=ROOT, text=True).strip()
    return f'{major}.{minor}.{int(patch)}'


if __name__ == '__main__':
    print(build_version())
