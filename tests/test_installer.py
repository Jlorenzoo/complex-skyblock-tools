"""Portable installer tests; all files are disposable fixtures."""
import importlib.util
import json
from pathlib import Path
import sys
import tempfile
import unittest
from unittest.mock import patch
import zipfile

BASE = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(BASE / "scripts"))
spec = importlib.util.spec_from_file_location("toolkit_install", BASE / "scripts/install.py")
installer = importlib.util.module_from_spec(spec)
spec.loader.exec_module(installer)

def jar(path, mod_id):
    with zipfile.ZipFile(path, "w") as archive:
        archive.writestr("fabric.mod.json", json.dumps({"id": mod_id, "version": "1.0.0"}))
        archive.writestr("fixture.txt", mod_id)

class InstallerTest(unittest.TestCase):
    def setUp(self):
        temp = tempfile.TemporaryDirectory()
        self.addCleanup(temp.cleanup)
        self.root = Path(temp.name)
        self.mods = self.root / "mods"
        self.mods.mkdir()
        self.source = self.root / "skyblock-toolkit-1.0.0.jar"
        jar(self.source, "skyblocktoolkit")
        for name, mod_id in [("booster.jar", "moneyboosterhud"), ("fishing.jar", "fishingpings"),
                             ("chat.jar", "chattextselect"), ("fabric-api.jar", "fabric-api"),
                             ("ChatShot.jar", "chatshot")]:
            jar(self.mods / name, mod_id)
        self.before = {p.name: p.read_bytes() for p in self.mods.iterdir()}
        self.config = self.root / "config"
        self.config.mkdir()
        (self.config / "history.json").write_text('{"earnings":123}')
        for patcher in [patch.object(installer, "MODS", self.mods),
                        patch.object(installer, "SOURCE", self.source),
                        patch.object(installer, "STATUS", self.root / "status.txt"),
                        patch.object(installer, "LOCK_FILE", self.root / "install.lock"),
                        patch.object(sys, "argv", ["install.py"])]:
            patcher.start()
            self.addCleanup(patcher.stop)

    def test_replacement_backups_preservation_and_idempotence(self):
        with patch.object(installer, "active_clients", return_value=[]):
            installer.main()
            backups = list(self.mods.glob("*.bak.*"))
            self.assertEqual(len(backups), 3)
            for backup in backups:
                self.assertEqual(backup.read_bytes(), self.before[backup.name.split(".bak.")[0]])
            for name in ("fabric-api.jar", "ChatShot.jar"):
                self.assertEqual((self.mods / name).read_bytes(), self.before[name])
            self.assertEqual((self.mods / self.source.name).read_bytes(), self.source.read_bytes())
            self.assertEqual((self.config / "history.json").read_text(), '{"earnings":123}')
            installer.main()
            self.assertEqual(set(backups), set(self.mods.glob("*.bak.*")))

    def test_running_game_leaves_originals_untouched(self):
        with patch.object(installer, "active_clients", return_value=[123]), self.assertRaises(SystemExit):
            installer.main()
        self.assertEqual({p.name: p.read_bytes() for p in self.mods.iterdir()}, self.before)

    def test_reopen_during_preparation_does_not_replace(self):
        with patch.object(installer, "active_clients", side_effect=[[], [123]]), self.assertRaises(SystemExit):
            installer.main()
        for name, content in self.before.items():
            self.assertEqual((self.mods / name).read_bytes(), content)
        self.assertFalse((self.mods / self.source.name).exists())
        self.assertFalse(list(self.mods.glob("*.pending")))

    def test_rollback_restores_originals(self):
        original_replace = Path.replace
        def fail_install(path, target):
            if path.name.endswith(".pending"):
                raise OSError("simulated installation failure")
            return original_replace(path, target)
        with patch.object(installer, "active_clients", return_value=[]), patch.object(Path, "replace", fail_install):
            with self.assertRaises(OSError):
                installer.main()
        for name, content in self.before.items():
            self.assertEqual((self.mods / name).read_bytes(), content)

    def test_corrupt_old_toolkit_stops_before_changes(self):
        (self.mods / "money-booster-hud-broken.jar").write_bytes(b"broken")
        with patch.object(installer, "active_clients", return_value=[]), self.assertRaises(SystemExit):
            installer.main()
        for name, content in self.before.items():
            self.assertEqual((self.mods / name).read_bytes(), content)

    def test_second_installer_is_rejected(self):
        with installer.installer_lock():
            with self.assertRaises(SystemExit), installer.installer_lock():
                pass

if __name__ == "__main__":
    unittest.main()
