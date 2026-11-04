from pathlib import Path
import hashlib, json, os, shutil, subprocess, sys, tempfile, urllib.request

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "scripts"))
from local_paths import settings, toolkit_jar
from build_version import build_version

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
    PRISM = Path.home() / ".var/app/org.prismlauncher.PrismLauncher/data/PrismLauncher"
    install = PRISM
    version = json.loads((install / f"meta/net.minecraft/{VERSION}.json").read_text()) if (install / f"meta/net.minecraft/{VERSION}.json").exists() else json.loads(urllib.request.urlopen(f"https://meta.fabricmc.net/v2/versions/loader/{VERSION}/{LOADER}/profile/json").read())
    output = ROOT / "artifacts/seasonals-preview"
    output.mkdir(parents=True, exist_ok=True)
    for screenshot in (output / "screenshots").glob("*.png"):
        screenshot.unlink()
    cache = output / "dependencies"
    with urllib.request.urlopen(f"https://meta.fabricmc.net/v2/versions/loader/{VERSION}/{LOADER}/profile/json", timeout=30) as response:
        profile = json.load(response)

    vanilla_meta = json.loads(urllib.request.urlopen("https://piston-meta.mojang.com/mc/game/version_manifest_v2.json").read())
    v_url = next(v["url"] for v in vanilla_meta["versions"] if v["id"] == VERSION)
    v_info = json.loads(urllib.request.urlopen(v_url).read())

    libraries = []
    for library in v_info["libraries"] + profile["libraries"]:
        allowed = not library.get("rules")
        for rule in library.get("rules", []):
            os_name = rule.get("os", {}).get("name", "linux")
            if os_name == "linux":
                allowed = rule["action"] == "allow"
            elif "os" in rule:
                allowed = False
        if not allowed:
            continue
        artifact = library.get("downloads", {}).get("artifact")
        if artifact:
            relative, url, checksum = artifact["path"], artifact["url"], artifact.get("sha1")
        else:
            group, name, revision = library["name"].split(":")[:3]
            relative = f"{group.replace('.', '/')}/{name}/{revision}/{name}-{revision}.jar"
            url = library["url"] + relative
            checksum = None
        installed = install / "libraries" / relative
        libraries.append(fetch(url, installed if installed.exists() else cache / relative, checksum))

    client = install / "libraries/com/mojang/minecraft/1.21.11/minecraft-1.21.11-client.jar"
    if not client.exists():
        client = fetch(v_info["downloads"]["client"]["url"], cache / "minecraft-1.21.11.jar", v_info["downloads"]["client"]["sha1"])

    api_url = f"https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/{API}/fabric-api-{API}.jar"
    api = fetch(api_url, cache / f"fabric-api-{API}.jar")

    java = install / "java/java-runtime-delta/bin/java"
    toolkit = toolkit_jar("1.21.11")
    smoke = ROOT / f"versions/1.21.11/build/libs/complexify-{build_version()}-menu-smoke.jar"
    if not smoke.exists():
        smoke = ROOT / f"versions/1.21.11/build/libs/skyblock-toolkit-{build_version()}-menu-smoke.jar"

    with tempfile.TemporaryDirectory(prefix="toolkit-preview-") as temp:
        game = Path(temp)
        mods = game / "mods"
        mods.mkdir()
        for mod in (toolkit, smoke, api):
            shutil.copy2(mod, mods / ("menu-smoke.jar" if mod == smoke else mod.name))
        (game / "options.txt").write_text("onboardAccessibility:false\nshowMusicToast:false\nmaxFps:30\nrenderDistance:2\npauseOnLostFocus:false\n")
        args = ["-Xmx1G", "-Dtoolkit.preview=" + str(output), "-Dfabric.log.disableAnsi=true",
                "-cp", os.pathsep.join(map(str, [*libraries, client])), profile["mainClass"],
                "--gameDir", str(game), "--assetsDir", str(install / "assets"),
                "--assetIndex", v_info["assetIndex"]["id"], "--version", VERSION,
                "--accessToken", "0", "--username", "ToolkitPreview",
                "--uuid", "00000000-0000-0000-0000-000000000001", "--userType", "legacy",
                "--width", "1280", "--height", "800"]
        argfile = game / "launch.args"
        argfile.write_text("\n".join('"' + arg.replace('\\', '/') + '"' for arg in args), encoding="utf-8")
        with (output / "runtime.log").open("w", encoding="utf-8") as log:
            print("Launching isolated client to render screenshots...")
            result = subprocess.run([str(java), "@" + str(argfile)], cwd=game,
                env={**os.environ, "ALSOFT_DRIVERS": "null"}, stdout=log, stderr=subprocess.STDOUT, timeout=120)
        if result.returncode:
            print((output / "runtime.log").read_text(encoding="utf-8", errors="replace")[-4000:])
            raise SystemExit(result.returncode)
        print("Success! Screenshots generated at:", output / "screenshots")

if __name__ == "__main__":
    main()
