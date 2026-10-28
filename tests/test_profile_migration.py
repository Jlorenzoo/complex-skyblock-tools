"""Disposable regression checks for settings-preserving profile migration."""
import gzip
import json
from pathlib import Path
import sys
import tempfile
import unittest
import zipfile

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / 'scripts'))
from profile_migration import adapt_schematic, copy_player_data, options_for_1_21_11, digest


class MigrationTest(unittest.TestCase):
    def test_options_keep_custom_and_unbound_controls_and_preferences(self):
        original = 'version:4903\r\nkey_key.jump:key.mouse.4\r\nkey_key.skyblocktoolkit.sellall:key.keyboard.unknown\r\nsoundCategory_master:0.19\r\n'
        self.assertEqual(options_for_1_21_11(original), original.replace('4903', '4671'))

    def test_profile_copy_preserves_settings_history_and_source_bytes(self):
        with tempfile.TemporaryDirectory() as temp:
            source, target = Path(temp) / 'old', Path(temp) / 'new'
            files = {'options.txt': b'version:4903\nkey_key.attack:key.mouse.left\n',
                     'config/skyblocktoolkit.json': b'{"darkMode":true}',
                     'config/moneyboosterhistory.json': b'{"income":123}',
                     'config/voicechat/voicechat-client.properties': b'microphone=custom\n',
                     'schematics/farm.litematic': b'schematic', 'servers.dat': b'servers',
                     'screenshots/keep.png': b'picture', 'saves/world/level.dat': b'world',
                     'mods/old.jar': b'old binary', '.fabric/cache': b'old cache'}
            for name, data in files.items():
                file = source / name
                file.parent.mkdir(parents=True, exist_ok=True)
                file.write_bytes(data)
            records = copy_player_data(source, target)
            for name, data in files.items():
                self.assertEqual((source / name).read_bytes(), data)
                if name.startswith(('mods/', '.fabric/')):
                    self.assertFalse((target / name).exists())
                elif name != 'options.txt':
                    self.assertEqual((target / name).read_bytes(), data)
                if name in records:
                    self.assertEqual(records[name]['installed_sha256'], digest(target / name))
            with self.assertRaises(ValueError):
                copy_player_data(source, target)

    def test_crop_pack_keeps_texture_bytes_and_retargets_format(self):
        with tempfile.TemporaryDirectory() as temp:
            source, target = Path(temp) / 'old', Path(temp) / 'new'
            pack = source / 'resourcepacks/Ready Crops - Color and Size.zip'
            pack.parent.mkdir(parents=True)
            with zipfile.ZipFile(pack, 'w') as archive:
                archive.writestr('pack.mcmeta', '{"pack":{"min_format":[88,0],"max_format":[88,0],"description":"crops"}}')
                archive.writestr('assets/minecraft/textures/block/wheat_stage0.png', b'pixels')
            before = digest(pack)
            copy_player_data(source, target)
            self.assertEqual(digest(pack), before)
            with zipfile.ZipFile(target / pack.relative_to(source)) as archive:
                self.assertEqual(json.loads(archive.read('pack.mcmeta'))['pack']['min_format'], [75, 0])
                self.assertEqual(archive.read('assets/minecraft/textures/block/wheat_stage0.png'), b'pixels')

    def test_schematic_keeps_payload_and_retargets_future_minecraft_version(self):
        with tempfile.TemporaryDirectory() as temp:
            source, target = Path(temp) / 'old', Path(temp) / 'new'
            schematic = source / 'schematics/farm.litematic'
            schematic.parent.mkdir(parents=True)
            raw = b'\n\x00\x00\x03\x00\x14MinecraftDataVersion\x00\x00\x13\x27\x00payload'
            with gzip.open(schematic, 'wb') as stream:
                stream.write(raw)
            before = digest(schematic)
            records = copy_player_data(source, target)
            self.assertEqual(digest(schematic), before)
            with gzip.open(target / schematic.relative_to(source), 'rb') as stream:
                decompressed = stream.read()
                self.assertEqual(decompressed, b'\n\x00\x00\x03\x00\x14MinecraftDataVersion\x00\x00\x12\x3f\x00payload')
            self.assertEqual(records['schematics/farm.litematic']['installed_sha256'], digest(target / 'schematics/farm.litematic'))

    def test_schematic_already_target_version_not_recompressed(self):
        with tempfile.TemporaryDirectory() as temp:
            schematic = Path(temp) / 'current.litematic'
            raw = b'\n\x00\x00\x03\x00\x14MinecraftDataVersion\x00\x00\x12\x3f\x00payload'
            with gzip.open(schematic, 'wb') as stream:
                stream.write(raw)
            self.assertFalse(adapt_schematic(schematic, 4671))

    def test_schematic_paths_retarget_external_references_when_present(self):
        with tempfile.TemporaryDirectory() as temp:
            source, stage, final = (Path(temp) / name for name in ('old', 'stage', 'final'))
            (source / 'config').mkdir(parents=True)
            (source / 'schematics').mkdir(parents=True)
            (source / 'schematics/farm.litematic').write_bytes(b'schematic')
            external_path = '/some/other/profile/schematics/farm.litematic'
            original = {'placements': [{'schematic': external_path}]}
            file = source / 'config/litematica.json'
            file.write_text(json.dumps(original))
            copy_player_data(source, stage, profile_path=final)
            copied = json.loads((stage / 'config/litematica.json').read_text())
            self.assertEqual(copied['placements'][0]['schematic'], str(final / 'schematics/farm.litematic'))


if __name__ == '__main__':
    unittest.main()
