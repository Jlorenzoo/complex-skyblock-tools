"""Compile and run the eight self-contained Java regression suites after build.py."""
from pathlib import Path
import os
import subprocess
import sys
import tempfile

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))
import build

with tempfile.TemporaryDirectory() as tmp:
    classes = Path(tmp) / "classes"
    classes.mkdir()
    libraries = build.LOCAL.get("library_jars", []) or list(Path(build.LIBRARIES_DIR).rglob("*.jar"))
    cp = os.pathsep.join(map(str, [ROOT / "build/classes", build.MC_JAR, *libraries]))
    sources = sorted((ROOT / "tests").glob("*Test.java"))
    arguments = ["-encoding", "UTF-8", "-proc:none", "-cp", cp, "-d", str(classes), *map(str, sources)]
    argfile = Path(tmp) / "javac.args"
    argfile.write_text("\n".join('"' + arg.replace('\\', '/') + '"' for arg in arguments), encoding="utf-8")
    subprocess.run([build.JAVAC, "@" + str(argfile)], check=True)
    java = str(Path(build.JAVAC).with_name("java.exe" if os.name == "nt" else "java"))
    for source in sources:
        if source.stem == "ShopReplayTest":
            continue  # Requires private transaction history, which is not shipped.
        name = source.stem
        text = source.read_text(encoding="utf-8")
        if text.startswith("package "):
            name = text.split(";", 1)[0].removeprefix("package ").strip() + "." + name
        args = [str(Path(tmp) / "history.json")] if source.stem == "BoosterHistoryTest" else []
        subprocess.run([java, "-cp", os.pathsep.join([str(classes), cp]), name, *args], check=True)
