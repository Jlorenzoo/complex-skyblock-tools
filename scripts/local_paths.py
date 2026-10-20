"""Optional workstation paths shared by the builder and installer."""
import json
from pathlib import Path

PROJECT_DIR = Path(__file__).resolve().parents[1]

def settings():
    path = PROJECT_DIR / "local-settings.json"
    return json.loads(path.read_text(encoding="utf-8")) if path.exists() else {}
