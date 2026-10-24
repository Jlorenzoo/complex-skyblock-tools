"""Optional workstation paths shared by the builder and installer."""
import json
from pathlib import Path

PROJECT_DIR = Path(__file__).resolve().parents[1]

def settings():
    path = PROJECT_DIR / "local-settings.json"
    return json.loads(path.read_text(encoding="utf-8")) if path.exists() else {}


def minecraft_version():
    """The maintained pack target; workstation overrides can retain a legacy pack."""
    return settings().get("minecraft_version", "1.21.11")


def toolkit_jar(version=None):
    version = version or minecraft_version()
    if version == "1.21.11":
        return PROJECT_DIR / "versions/1.21.11/build/libs/skyblock-toolkit-1.1.0+mc1.21.11.jar"
    if version == "26.2":
        return PROJECT_DIR / "build/skyblock-toolkit-1.1.0.jar"
    raise ValueError("Unsupported Minecraft target: " + version)
