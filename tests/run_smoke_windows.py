"""Run upstream's real Fabric audio/mixin smoke test in a disposable profile."""
from pathlib import Path
import argparse, json, os, shutil, subprocess, sys, tempfile, zipfile
sys.stdout.reconfigure(errors='backslashreplace')
root = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(root))
import build
parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--loader', help='Exercise a specific Fabric Loader version')
parser.add_argument('--source', type=Path, default=root / 'build/skyblock-toolkit-1.1.0.jar')
parser.add_argument('--expect-loader-rejection', action='store_true', help='Require a clear version mismatch instead of a mixin crash')
cli = parser.parse_args()
build.configure_26_2(loader_version=cli.loader)
toolkit = cli.source.resolve()
libraries = build.LOCAL['library_jars']  # Already filtered for this OS by prepare().
classpath = os.pathsep.join([build.MC_JAR, *libraries])
java = str(Path(build.JAVAC).with_name('java.exe' if os.name == 'nt' else 'java'))
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
    output = log.read_text(encoding='utf-8')
    print(output[-7000:])
    if cli.expect_loader_rejection:
        if result.returncode and '0.19.5' in output and 'fabricloader' in output.lower() and 'ClassCastException' not in output:
            print('PASS: outdated loader rejected with the required version, before mixin transformation')
            raise SystemExit(0)
        raise SystemExit('Expected a clear Fabric Loader 0.19.5 dependency rejection')
    raise SystemExit(result.returncode)
