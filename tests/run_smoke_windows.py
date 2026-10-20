"""Run upstream's real Fabric audio/mixin smoke test in a disposable profile."""
from pathlib import Path
import json, os, shutil, subprocess, sys, tempfile, zipfile
root = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(root))
import build
toolkit = root / 'build/skyblock-toolkit-1.1.0.jar'
libraries = [p for p in build.LOCAL['library_jars']
             if 'natives-' not in p or p.endswith('-natives-windows.jar')]
classpath = os.pathsep.join([build.MC_JAR, *libraries])
java = str(Path(build.JAVAC).with_name('java.exe'))
with tempfile.TemporaryDirectory(prefix='toolkit-update-smoke-') as tmp:
    game = Path(tmp)
    mods = game / 'mods'; mods.mkdir()
    classes = game / 'classes'; classes.mkdir()
    shutil.copy2(toolkit, mods / toolkit.name)
    shutil.copy2(build.FAPI_JAR, mods / Path(build.FAPI_JAR).name)
    args = ['-encoding', 'UTF-8', '-proc:none', '-cp', str(toolkit) + os.pathsep + classpath,
            '-d', str(classes), str(root / 'tests/SoundControlsSmoke.java')]
    argfile = game / 'compile.args'
    argfile.write_text('\n'.join('"' + a.replace('\\', '/') + '"' for a in args), encoding='utf-8')
    subprocess.run([build.JAVAC, '@' + str(argfile)], check=True)
    with zipfile.ZipFile(mods / 'sound-smoke.jar', 'w') as z:
        z.writestr('fabric.mod.json', json.dumps({
            'schemaVersion': 1, 'id': 'toolkitsoundsmoke', 'version': '1.0.0',
            'environment': 'client',
            'entrypoints': {'preLaunch': ['com.skyblocktoolkit.SoundControlsSmoke']},
            'depends': {'skyblocktoolkit': '*'}}))
        for p in classes.rglob('*.class'):
            z.write(p, p.relative_to(classes))
    args = ['-Xmx512m', '-Djava.awt.headless=true', '-Dfabric.gameVersion=26.2',
            '-Dfabric.log.disableAnsi=true', '-cp', classpath,
            'net.fabricmc.loader.impl.launch.knot.KnotClient', '--gameDir', str(game), '--version', '26.2']
    argfile = game / 'run.args'
    argfile.write_text('\n'.join('"' + a.replace('\\', '/') + '"' for a in args), encoding='utf-8')
    log = root / 'fabric-smoke.log'
    with log.open('w', encoding='utf-8') as output:
        result = subprocess.run([java, '@' + str(argfile)], cwd=game,
                                env={**os.environ, 'ALSOFT_DRIVERS': 'null'},
                                stdout=output, stderr=subprocess.STDOUT, timeout=60)
    print(log.read_text(encoding='utf-8')[-7000:])
    raise SystemExit(result.returncode)
