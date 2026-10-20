"""Exercise and capture upstream menu runtime tests in an isolated Windows client."""
from pathlib import Path
import json, os, shutil, subprocess, sys, tempfile, zipfile
root = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(root))
import build
toolkit = root / 'build/skyblock-toolkit-1.0.0.jar'
install = Path(build.MC_JAR).parents[2]
output = root / 'artifacts/menu-preview'
output.mkdir(parents=True, exist_ok=True)
(output / 'PASS.txt').unlink(missing_ok=True)
for screenshot in (output / 'screenshots').glob('*.png'):
    screenshot.unlink()
libraries = [p for p in build.LOCAL['library_jars'] if 'natives-' not in p or p.endswith('-natives-windows.jar')]
classpath = os.pathsep.join([build.MC_JAR, *libraries])
java = str(Path(build.JAVAC).with_name('java.exe'))
with tempfile.TemporaryDirectory(prefix='toolkit-timer-menu-') as tmp:
    game = Path(tmp); mods = game / 'mods'; mods.mkdir(); classes = game / 'classes'; classes.mkdir()
    shutil.copy2(toolkit, mods / toolkit.name)
    shutil.copy2(build.FAPI_JAR, mods / Path(build.FAPI_JAR).name)
    with zipfile.ZipFile(build.FAPI_JAR) as z:
        for name in z.namelist():
            if name.startswith('META-INF/jars/') and name.endswith('.jar'): z.extract(name, game)
    compilepath = os.pathsep.join([str(toolkit), classpath, *map(str, (game / 'META-INF/jars').glob('*.jar'))])
    args = ['-encoding', 'UTF-8', '-proc:none', '-cp', compilepath, '-d', str(classes), str(root / 'tests/MenuRuntimeSmoke.java')]
    argfile = game / 'compile.args'
    argfile.write_text('\n'.join('"' + a.replace('\\', '/') + '"' for a in args), encoding='utf-8')
    subprocess.run([build.JAVAC, '@' + str(argfile)], check=True)
    with zipfile.ZipFile(mods / 'menu-smoke.jar', 'w') as z:
        z.writestr('fabric.mod.json', json.dumps({'schemaVersion': 1, 'id': 'toolkitmenusmoke', 'version': '1.0.0',
            'environment': 'client', 'entrypoints': {'client': ['com.skyblocktoolkit.MenuRuntimeSmoke']}, 'depends': {'skyblocktoolkit': '*'}}))
        for p in classes.rglob('*.class'): z.write(p, p.relative_to(classes))
    (game / 'options.txt').write_text('onboardAccessibility:false\nshowMusicToast:false\nmaxFps:30\nrenderDistance:2\npauseOnLostFocus:false\n')
    version = json.loads((install / 'versions/26.2/26.2.json').read_text())
    args = ['-Xmx1G', '--enable-native-access=ALL-UNNAMED', '-Dtoolkit.preview=' + str(output),
            '-Dfabric.gameVersion=26.2', '-Dfabric.log.disableAnsi=true', '-cp', classpath,
            'net.fabricmc.loader.impl.launch.knot.KnotClient', '--gameDir', str(game),
            '--assetsDir', str(install / 'assets'), '--assetIndex', version['assetIndex']['id'],
            '--version', '26.2', '--accessToken', '0', '--username', 'ToolkitPreview',
            '--uuid', '00000000-0000-0000-0000-000000000001', '--userType', 'legacy', '--width', '1280', '--height', '800']
    argfile = game / 'launch.args'
    argfile.write_text('\n'.join('"' + a.replace('\\', '/') + '"' for a in args), encoding='utf-8')
    with (output / 'runtime.log').open('w', encoding='utf-8') as log:
        result = subprocess.run([java, '@' + str(argfile)], cwd=game, env={**os.environ, 'ALSOFT_DRIVERS': 'null'},
                                stdout=log, stderr=subprocess.STDOUT, timeout=120, creationflags=subprocess.CREATE_NO_WINDOW)
    if result.returncode:
        print((output / 'runtime.log').read_text(encoding='utf-8')[-9000:])
        raise SystemExit(result.returncode)
    print((output / 'PASS.txt').read_text())
