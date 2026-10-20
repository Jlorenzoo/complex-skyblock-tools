#!/usr/bin/env python3
"""Install the integrated toolkit only after the shared Minecraft clients exit."""
from pathlib import Path
import argparse,datetime,fcntl,hashlib,json,os,shutil,time,zipfile

PROJECT_DIR = Path(__file__).resolve().parents[1] if Path(__file__).resolve().parent.name == "scripts" else Path(__file__).resolve().parent
SOURCE = PROJECT_DIR / "build" / "skyblock-toolkit-1.0.0.jar"
if not SOURCE.exists() and (PROJECT_DIR / "skyblock-toolkit-1.0.0.jar").exists():
    SOURCE = PROJECT_DIR / "skyblock-toolkit-1.0.0.jar"
MODS = Path(os.environ.get("MODS_DIR", Path.home() / "Documents/curseforge/minecraft/Instances/Complex Skyblock/mods"))
STATUS = PROJECT_DIR / "toolkit-installation-status.txt"
LOCK_FILE = PROJECT_DIR / ".toolkit-install.lock"
IDS = {"moneyboosterhud", "fishingpings", "chattextselect", "skyblocktoolkit"}

def status(message):
    temp = STATUS.with_suffix(".tmp")
    temp.write_text(message + "\n")
    temp.replace(STATUS)

def active_clients():
    result = []
    for p in Path("/proc").iterdir():
        if not p.name.isdigit():
            continue
        try:
            args = (p / "cmdline").read_bytes().split(b"\0")
            if args and args[0].endswith(b"/java") and any(b"/PrismLauncher/instances/Complex-Skyblock" in x or b"/curseforge/minecraft/Instances/Complex Skyblock" in x for x in args):
                result.append(p.name)
        except OSError:
            pass
    return result

def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()

def mod_id(path):
    with zipfile.ZipFile(path) as z:
        if "fabric.mod.json" not in z.namelist():
            return None
        return json.loads(z.read("fabric.mod.json")).get("id")

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--wait", action="store_true", help="Wait until running Minecraft client closes")
    args = parser.parse_args()

    if not SOURCE.exists():
        raise SystemExit(f"Build jar not found at {SOURCE}. Run python3 build.py first.")

    with LOCK_FILE.open("a") as lock:
        try:
            fcntl.flock(lock, fcntl.LOCK_EX | fcntl.LOCK_NB)
        except BlockingIOError:
            raise SystemExit("A toolkit installer is already waiting.")

        with zipfile.ZipFile(SOURCE) as z:
            meta = json.loads(z.read("fabric.mod.json"))
            assert meta["id"] == "skyblocktoolkit" and meta["version"] == "1.0.0" and z.testzip() is None

        deadline = time.monotonic() + 86400
        while active_clients():
            status("Queued: Skyblock Toolkit 1.0.0 replaces Booster HUD, Fishing Pings and Chat Copy after all shared Complex Skyblock clients close. Settings and history stay in place.")
            if not args.wait:
                raise SystemExit("Close Minecraft and run again, or use --wait.")
            if time.monotonic() > deadline:
                status("Wait expired. Close Minecraft and run install.py.")
                raise SystemExit(1)
            time.sleep(3)

        originals = []
        for path in MODS.glob("*.jar"):
            try:
                if mod_id(path) in IDS:
                    originals.append(path)
            except (zipfile.BadZipFile, json.JSONDecodeError, KeyError):
                if path.name.startswith(("money-booster-hud", "fishing-pings", "chattextselect", "skyblock-toolkit")):
                    status("Stopped: could not identify " + path.name + ". No mods replaced.")
                    raise SystemExit(1)

        dest = MODS / SOURCE.name
        if originals == [dest] and digest(dest) == digest(SOURCE):
            status("Skyblock Toolkit 1.0.0 is already installed. Open with Right Shift or Escape -> Skyblock Toolkit.")
            print("Skyblock Toolkit 1.0.0 is already up to date in mods folder.")
            return

        stamp = datetime.datetime.now().strftime("%Y%m%d-%H%M%S")
        backups = {}
        for path in originals:
            backup = path.with_name(path.name + ".bak." + stamp)
            shutil.copy2(path, backup)
            backups[path] = backup

        temporary = MODS / (dest.name + ".pending")
        shutil.copy2(SOURCE, temporary)
        if digest(temporary) != digest(SOURCE):
            temporary.unlink()
            raise SystemExit("Copy verification failed.")

        if active_clients():
            temporary.unlink()
            status("Client reopened during preparation. Close Minecraft and run the installer again.")
            raise SystemExit(1)

        removed = []
        installed = False
        try:
            for path in originals:
                path.unlink()
                removed.append(path)
            temporary.replace(dest)
            installed = True
            if digest(dest) != digest(SOURCE):
                raise RuntimeError("Installed checksum mismatch")
            status("Installed Skyblock Toolkit 1.0.0. Start Minecraft, then press Right Shift or Escape -> Skyblock Toolkit. Original mods backed up; settings/history preserved.")
            print("Installed Skyblock Toolkit 1.0.0 successfully.")
        except Exception as error:
            if installed and dest.exists():
                dest.unlink()
            for path in removed:
                shutil.copy2(backups[path], path)
            temporary.unlink(missing_ok=True)
            status("Installation failed; original mods restored: " + str(error))
            raise

if __name__ == "__main__":
    main()
