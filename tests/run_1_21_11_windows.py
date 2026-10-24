"""Exercise the distributed 1.21.11 jar in an isolated, offline Windows client.

Requires an installed vanilla 1.21.11 client/assets and Java 21. Builds the smoke
mod first with gradlew build remapMenuSmoke; no live mods/settings are modified.
"""
from pathlib import Path
import hashlib
import json
import os
import shutil
import subprocess
import sys
import tempfile
import urllib.request
import argparse

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "scripts"))
from local_paths import settings

VERSION = "1.21.11"
LOADER = "0.18.4"
API = "0.141.6+1.21.11"


def fetch(url, path, sha1=None):
    if not path.exists():
        path.parent.mkdir(parents=True, exist_ok=True)
        with urllib.request.urlopen(url, timeout=60) as response:
            data = response.read()
        if sha1 and hashlib.sha1(data).hexdigest() != sha1:
            raise RuntimeError("Checksum mismatch: " + path.name)
        path.write_bytes(data)
    if sha1 and hashlib.sha1(path.read_bytes()).hexdigest() != sha1:
        raise RuntimeError("Checksum mismatch: " + path.name)
    return path


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--profile', type=Path, help='Copy real settings and every pack mod into the disposable runtime')
    cli = parser.parse_args()
    local = settings()
    install = Path(os.environ.get("MINECRAFT_INSTALL", str(Path(local["minecraft_jar"]).parents[2])))
    version = json.loads((install / f"versions/{VERSION}/{VERSION}.json").read_text())
    output = ROOT / ("artifacts/fabric-1.21.11-pack" if cli.profile else "artifacts/fabric-1.21.11")
    output.mkdir(parents=True, exist_ok=True)
    (output / "PASS.txt").unlink(missing_ok=True)
    for screenshot in (output / "screenshots").glob("*.png"):
        screenshot.unlink()
    cache = output / "dependencies"
    with urllib.request.urlopen(f"https://meta.fabricmc.net/v2/versions/loader/{VERSION}/{LOADER}/profile/json", timeout=30) as response:
        profile = json.load(response)
    libraries = []
    for library in version["libraries"] + profile["libraries"]:
        allowed = not library.get("rules")
        for rule in library.get("rules", []):
            if rule.get("os", {}).get("name", "windows") == "windows":
                allowed = rule["action"] == "allow"
        if not allowed:
            continue
        artifact = library.get("downloads", {}).get("artifact")
        if artifact:
            relative, url, checksum = artifact["path"], artifact["url"], artifact.get("sha1")
        else:
            group, name, revision = library["name"].split(":")[:3]
            relative = f"{group.replace('.', '/')}/{name}/{revision}/{name}-{revision}.jar"
            url = library["url"] + relative
            with urllib.request.urlopen(url + ".sha1", timeout=30) as response:
                checksum = response.read().decode().strip().split()[0]
        installed = install / "libraries" / relative
        libraries.append(fetch(url, installed if installed.exists() else cache / relative, checksum))
    client = fetch(version["downloads"]["client"]["url"], cache / "minecraft-1.21.11.jar", version["downloads"]["client"]["sha1"])
    api_url = f"https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/{API}/fabric-api-{API}.jar"
    with urllib.request.urlopen(api_url + ".sha1", timeout=30) as response:
        checksum = response.read().decode().strip().split()[0]
    api = fetch(api_url, cache / f"fabric-api-{API}.jar", checksum)
    java = Path(os.environ.get("JAVA_21", str(install / "java/java-runtime-delta/bin/java.exe")))
    toolkit = ROOT / "versions/1.21.11/build/libs/skyblock-toolkit-1.1.0+mc1.21.11.jar"
    smoke = ROOT / "versions/1.21.11/build/libs/skyblock-toolkit-1.1.0-menu-smoke.jar"
    with tempfile.TemporaryDirectory(prefix="toolkit-1.21.11-") as temp:
        game = Path(temp)
        mods = game / "mods"
        mods.mkdir()
        migration_args = []
        if cli.profile:
            for folder in ('config', 'resourcepacks'):
                if (cli.profile / folder).exists():
                    shutil.copytree(cli.profile / folder, game / folder)
            for mod in (cli.profile / 'mods').glob('*.jar'):
                shutil.copy2(mod, mods / mod.name)
            shutil.copy2(cli.profile / 'options.txt', game / 'options.txt')
            migration_args = ['-Dtoolkit.migrationOptions=' + str(cli.profile / 'options.txt')]
        for mod in (toolkit, smoke, api):
            shutil.copy2(mod, mods / mod.name)
        if not cli.profile:
            (game / "options.txt").write_text("onboardAccessibility:false\nshowMusicToast:false\nmaxFps:30\nrenderDistance:2\npauseOnLostFocus:false\n")
        args = ["-Xmx1G", *migration_args, "-Dtoolkit.preview=" + str(output), "-Dfabric.log.disableAnsi=true",
                "-cp", os.pathsep.join(map(str, [*libraries, client])), profile["mainClass"],
                "--gameDir", str(game), "--assetsDir", str(install / "assets"),
                "--assetIndex", version["assetIndex"]["id"], "--version", VERSION,
                "--accessToken", "0", "--username", "ToolkitPreview",
                "--uuid", "00000000-0000-0000-0000-000000000001", "--userType", "legacy",
                "--width", "1280", "--height", "800"]
        argfile = game / "launch.args"
        argfile.write_text("\n".join('"' + arg.replace('\\', '/') + '"' for arg in args), encoding="utf-8")
        with (output / "runtime.log").open("w", encoding="utf-8") as log:
            result = subprocess.run([str(java), "@" + str(argfile)], cwd=game,
                env={**os.environ, "ALSOFT_DRIVERS": "null"}, stdout=log, stderr=subprocess.STDOUT,
                timeout=180, creationflags=subprocess.CREATE_NO_WINDOW)
        if result.returncode:
            print((output / "runtime.log").read_text(encoding="utf-8")[-12000:])
            raise SystemExit(result.returncode)
        print((output / "PASS.txt").read_text())
        if cli.profile:
            def options(path):
                return dict(line.split(':', 1) for line in path.read_text(encoding='utf-8').splitlines() if ':' in line)
            original = options(cli.profile / 'options.txt')
            loaded = options(output / 'loaded-options.txt')
            changed = []
            for key in original.keys() & loaded.keys():
                try:
                    equal = json.loads(original[key]) == json.loads(loaded[key])
                except (ValueError, TypeError):
                    equal = original[key] == loaded[key]
                if not equal:
                    changed.append(key)
            if changed:
                raise RuntimeError('Migrated options changed: ' + ', '.join(changed))
            message = f'PASS: {len(original.keys() & loaded.keys())} supported options retained unchanged'
            (output / 'migration-options.txt').write_text(message + '\n')
            print((output / 'migration-controls.txt').read_text().strip())
            print(message)
        print("Production jar SHA-256:", hashlib.sha256(toolkit.read_bytes()).hexdigest())


if __name__ == "__main__":
    main()
