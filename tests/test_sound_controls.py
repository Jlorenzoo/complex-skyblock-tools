"""Verify audio mixins with real Fabric in a disposable, offline client directory."""
import json
import os
from pathlib import Path
import shutil
import subprocess
import tempfile
import zipfile

BASE = Path(__file__).resolve().parents[1]
PRISM = Path(os.environ.get('PRISM_DATA', Path.home() / '.var/app/org.prismlauncher.PrismLauncher/data/PrismLauncher'))
JAVA = PRISM / 'java/java-runtime-epsilon/bin'
libraries = []
for metadata in ['net.minecraft/26.2', 'net.fabricmc.fabric-loader/0.19.5', 'org.lwjgl3/3.4.1']:
    data = json.loads((PRISM / 'meta' / (metadata + '.json')).read_text())
    for lib in data.get('libraries', []) + ([data['mainJar']] if 'mainJar' in data else []):
        group, artifact, version, *classifier = lib['name'].split(':')
        suffix = '-' + classifier[0] if classifier else ''
        path = PRISM / 'libraries' / group.replace('.', '/') / artifact / version / f'{artifact}-{version}{suffix}.jar'
        if path.exists():
            libraries.append(str(path))
classpath = ':'.join(dict.fromkeys(libraries))

with tempfile.TemporaryDirectory(prefix='toolkit-sound-smoke-') as tmp:
    root = Path(tmp)
    classes = root / 'classes'
    classes.mkdir()
    mods = root / 'mods'
    mods.mkdir()
    toolkit = BASE / 'build/skyblock-toolkit-1.0.0.jar'
    shutil.copy2(toolkit, mods / toolkit.name)
    shutil.copy2(PRISM / 'instances/Complex-Skyblock-2/.minecraft/mods/fabric-api-0.160.0+26.2.jar', mods)
    subprocess.run([str(JAVA / 'javac'), '-proc:none', '-cp', str(toolkit) + ':' + classpath,
                    '-d', str(classes), str(BASE / 'tests/SoundControlsSmoke.java')], check=True)
    with zipfile.ZipFile(mods / 'sound-smoke.jar', 'w') as z:
        z.writestr('fabric.mod.json', json.dumps({
            'schemaVersion': 1, 'id': 'toolkitsoundsmoke', 'version': '1.0.0',
            'environment': 'client', 'entrypoints': {'preLaunch': ['com.skyblocktoolkit.SoundControlsSmoke']},
            'depends': {'skyblocktoolkit': '*'},
        }))
        for p in classes.rglob('*.class'):
            z.write(p, p.relative_to(classes))
    subprocess.run([str(JAVA / 'java'), '-Djava.awt.headless=true', '-Dfabric.gameVersion=26.2',
                    '-cp', classpath, 'net.fabricmc.loader.impl.launch.knot.KnotClient',
                    '--gameDir', str(root), '--version', '26.2'], cwd=root,
                   env={**os.environ, 'ALSOFT_DRIVERS': 'null'}, check=True, timeout=60)
