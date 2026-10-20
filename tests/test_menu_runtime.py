"""Render and exercise menus in an isolated offline Fabric client (requires a display)."""
import json, os, shutil, subprocess, tempfile, zipfile
from pathlib import Path
BASE=Path(__file__).resolve().parents[1]
PRISM=Path('/home/jeremy/.var/app/org.prismlauncher.PrismLauncher/data/PrismLauncher')
JAVA=PRISM/'java/java-runtime-epsilon/bin'
libraries=[]
for metadata in ['net.minecraft/26.2','net.fabricmc.fabric-loader/0.19.5','org.lwjgl3/3.4.1']:
    data=json.loads((PRISM/'meta'/(metadata+'.json')).read_text())
    for lib in data.get('libraries',[])+([data['mainJar']] if 'mainJar' in data else []):
        group,artifact,version,*classifier=lib['name'].split(':')
        suffix='-'+classifier[0] if classifier else ''
        path=PRISM/'libraries'/group.replace('.','/')/artifact/version/f'{artifact}-{version}{suffix}.jar'
        if path.exists(): libraries.append(str(path))
classpath=':'.join(dict.fromkeys(libraries))
output=BASE/'artifacts/menu-preview'
output.mkdir(parents=True,exist_ok=True)
for generated in [output/'PASS.txt',*output.glob('screenshots/*.png')]: generated.unlink(missing_ok=True)
with tempfile.TemporaryDirectory(prefix='toolkit-menu-smoke-') as tmp:
    root=Path(tmp);classes=root/'classes';classes.mkdir();mods=root/'mods';mods.mkdir()
    toolkit=BASE/'build/skyblock-toolkit-1.0.0.jar'
    shutil.copy2(toolkit,mods/toolkit.name)
    shutil.copy2(PRISM/'instances/Complex-Skyblock-2/.minecraft/mods/fabric-api-0.160.0+26.2.jar',mods)
    with zipfile.ZipFile(mods/'fabric-api-0.160.0+26.2.jar') as z:
        for name in z.namelist():
            if name.startswith('META-INF/jars/') and name.endswith('.jar'): z.extract(name,root)
    compilepath=classpath+':'+':'.join(str(p) for p in (root/'META-INF/jars').glob('*.jar'))
    subprocess.run([str(JAVA/'javac'),'-proc:none','-cp',str(toolkit)+':'+compilepath,'-d',str(classes),str(BASE/'tests/MenuRuntimeSmoke.java')],check=True)
    with zipfile.ZipFile(mods/'menu-smoke.jar','w') as z:
        z.writestr('fabric.mod.json',json.dumps({'schemaVersion':1,'id':'toolkitmenusmoke','version':'1.0.0','environment':'client','entrypoints':{'client':['com.skyblocktoolkit.MenuRuntimeSmoke']},'depends':{'skyblocktoolkit':'*'}}))
        for p in classes.rglob('*.class'):z.write(p,p.relative_to(classes))
    (root/'options.txt').write_text('onboardAccessibility:false\nshowMusicToast:false\n')
    with (output/'runtime.log').open('w') as log:
        result=subprocess.run([str(JAVA/'java'),'-Xmx1G','-Dtoolkit.preview='+str(output),'-Dfabric.gameVersion=26.2','-cp',classpath,'net.fabricmc.loader.impl.launch.knot.KnotClient','--gameDir',str(root),'--assetsDir',str(PRISM/'assets'),'--assetIndex','32','--version','26.2','--accessToken','0','--username','ToolkitPreview','--width','1280','--height','800'],cwd=root,env={**os.environ,'ALSOFT_DRIVERS':'null'},stdout=log,stderr=subprocess.STDOUT,timeout=120)
    if result.returncode: print((output/'runtime.log').read_text()[-7000:]);raise SystemExit(result.returncode)
    print((output/'PASS.txt').read_text())
