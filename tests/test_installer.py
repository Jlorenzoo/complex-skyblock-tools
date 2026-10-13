"""Exercise replacement/backups in a disposable folder, never the live profile."""
from pathlib import Path
import ast,hashlib,shutil,subprocess,sys,tempfile
BASE=Path(__file__).resolve().parents[1]
INSTALLER=BASE/'scripts/install.py'
LIVE=Path('/home/jeremy/Documents/curseforge/minecraft/Instances/Complex Skyblock/mods')
with tempfile.TemporaryDirectory() as tmp:
    root=Path(tmp);mods=root/'mods';mods.mkdir()
    copied=[]
    for pattern in ['money-booster-hud-*.jar','fishing-pings-*.jar','chattextselect-*.jar','fabric-api-*.jar']:
        for p in LIVE.glob(pattern):
            shutil.copy2(p,mods/p.name);copied.append(p.name)
    before={p.name:hashlib.sha256(p.read_bytes()).hexdigest() for p in mods.glob('*.jar')}
    shutil.copy2(BASE/'build/skyblock-toolkit-1.0.0.jar',root/'skyblock-toolkit-1.0.0.jar')
    tree=ast.parse(INSTALLER.read_text())
    for node in tree.body:
        if isinstance(node,ast.Assign) and any(isinstance(t,ast.Name) and t.id=='MODS' for t in node.targets):node.value=ast.Call(func=ast.Name(id='Path',ctx=ast.Load()),args=[ast.Constant(str(mods))],keywords=[])
        if isinstance(node,ast.FunctionDef) and node.name=='active_clients':node.body=[ast.Return(value=ast.List(elts=[],ctx=ast.Load()))]
    script=root/'installer.py';script.write_text(ast.unparse(ast.fix_missing_locations(tree)))
    subprocess.run([sys.executable,str(script)],check=True)
    assert sorted(p.name for p in mods.glob('*.jar'))==['fabric-api-0.160.0+26.2.jar','skyblock-toolkit-1.0.0.jar']
    assert hashlib.sha256((mods/'fabric-api-0.160.0+26.2.jar').read_bytes()).hexdigest()==before['fabric-api-0.160.0+26.2.jar']
    backups=list(mods.glob('*.bak.*'));assert len(backups)==len(before)-1
    for old in backups:assert hashlib.sha256(old.read_bytes()).hexdigest()==before[old.name.split('.bak.')[0]]
    subprocess.run([sys.executable,str(script)],check=True)
    assert len(list(mods.glob('*.bak.*')))==len(backups)
    print('PASS: integrated installation, unchanged Fabric API, byte-identical backups and idempotent reinstall in a disposable folder')
