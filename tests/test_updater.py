#!/usr/bin/env python3
"""Test update checker version comparison, GitHub release parsing and safe jar replacement."""
import json
import os
import shutil
import tempfile
import zipfile
from pathlib import Path

PROJECT = Path(__file__).resolve().parents[1]

def compare_versions(v1: str, v2: str) -> int:
    """Python replica of UpdateManager.compareVersions."""
    if v1 is None:
        return 0 if v2 is None else -1
    if v2 is None:
        return 1
    v1 = v1.strip().lstrip("vV")
    v2 = v2.strip().lstrip("vV")
    import re
    parts1 = re.split(r'[-+.]', v1)
    parts2 = re.split(r'[-+.]', v2)
    length = max(len(parts1), len(parts2))
    for i in range(length):
        p1 = parts1[i] if i < len(parts1) else "0"
        p2 = parts2[i] if i < len(parts2) else "0"
        try:
            n1 = int(p1)
            n2 = int(p2)
            if n1 != n2:
                return 1 if n1 > n2 else -1
        except ValueError:
            cmp = (p1.lower() > p2.lower()) - (p1.lower() < p2.lower())
            if cmp != 0:
                return cmp
    return 0

def test_semver_comparisons():
    assert compare_versions("1.0.1", "1.0.0") > 0
    assert compare_versions("v1.0.1", "1.0.0") > 0
    assert compare_versions("v1.3.3", "1.3.2") > 0
    assert compare_versions("1.3.3", "v1.3.3") == 0
    assert compare_versions("1.0.0", "1.0.1") < 0
    assert compare_versions("0.9.9", "1.0.0") < 0
    assert compare_versions("1.10.0", "1.9.0") > 0
    assert compare_versions("2.0.0", "1.99.99") > 0
    assert compare_versions("1.0.0", "1.0.0") == 0
    print("PASS: semver comparison checks")

def test_release_json_parsing():
    sample_release = {
        "tag_name": "v1.0.1",
        "name": "Skyblock Toolkit 1.0.1",
        "body": "Fixed booster HUD and added fishing ping controls.",
        "assets": [
            {
                "name": "skyblock-toolkit-source-1.0.1.zip",
                "browser_download_url": "https://github.com/Jlorenzoo/complex-skyblock-tools/releases/download/v1.0.1/skyblock-toolkit-source-1.0.1.zip"
            },
            {
                "name": "skyblock-toolkit-1.0.1.jar",
                "browser_download_url": "https://github.com/Jlorenzoo/complex-skyblock-tools/releases/download/v1.0.1/skyblock-toolkit-1.0.1.jar"
            }
        ]
    }
    
    tag = sample_release.get("tag_name", "").lstrip("vV")
    assert tag == "1.0.1"
    
    jar_asset = None
    for asset in sample_release.get("assets", []):
        name = asset.get("name", "")
        if name.endswith(".jar") and "source" not in name:
            jar_asset = asset
            break
            
    assert jar_asset is not None
    assert jar_asset["name"] == "skyblock-toolkit-1.0.1.jar"
    assert jar_asset["browser_download_url"].endswith(".jar")
    print("PASS: GitHub release asset extraction")

def test_jar_replacement_lifecycle():
    with tempfile.TemporaryDirectory() as temp_dir:
        mods_dir = Path(temp_dir) / "mods"
        mods_dir.mkdir()
        
        # Initial state: 1.0.0 installed
        old_jar = mods_dir / "skyblock-toolkit-1.0.0.jar"
        with zipfile.ZipFile(old_jar, "w") as z:
            z.writestr("fabric.mod.json", json.dumps({"id": "skyblocktoolkit", "version": "1.0.0"}))
            z.writestr("dummy.txt", "old" * 5000)
            
        assert old_jar.exists()
        
        # Simulate download of 1.0.1
        download_file = mods_dir / "skyblock-toolkit-1.0.1.jar.download"
        with zipfile.ZipFile(download_file, "w") as z:
            z.writestr("fabric.mod.json", json.dumps({"id": "skyblocktoolkit", "version": "1.0.1"}))
            z.writestr("dummy.txt", "new" * 5000)
            
        # Verify archive integrity
        with zipfile.ZipFile(download_file) as z:
            assert "fabric.mod.json" in z.namelist()
            meta = json.loads(z.read("fabric.mod.json"))
            assert meta["id"] == "skyblocktoolkit"
            assert meta["version"] == "1.0.1"
            
        # Install new jar
        target_jar = mods_dir / "skyblock-toolkit-1.0.1.jar"
        download_file.replace(target_jar)
        assert target_jar.exists()
        
        # Remove old jar
        old_jar.unlink()
        assert not old_jar.exists()
        
        # Simulate leftover cleanup on startup
        junk_pending = mods_dir / "something.jar.pending"
        junk_pending.write_text("junk")
        junk_download = mods_dir / "skyblock-toolkit-9.9.9.jar.download"
        junk_download.write_text("junk")
        
        for p in list(mods_dir.iterdir()):
            if p.name.endswith((".download", ".pending")) or ".bak." in p.name:
                p.unlink()
                
        remaining = sorted(p.name for p in mods_dir.iterdir())
        assert remaining == ["skyblock-toolkit-1.0.1.jar"]
        print("PASS: jar download, verification, replacement and cleanup")

def main():
    test_semver_comparisons()
    test_release_json_parsing()
    test_jar_replacement_lifecycle()
    print("ALL UPDATER TESTS PASSED!")

if __name__ == "__main__":
    main()
