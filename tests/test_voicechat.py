"""Voice chat installation tests use only disposable jars and profiles."""
import hashlib
import json
from pathlib import Path
import sys
import tempfile
import unittest
from unittest.mock import patch
import zipfile

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / 'scripts'))
import voicechat
import package_prism


def jar(path, mod_id, version='2.6.22+26.2'):
    with zipfile.ZipFile(path, 'w') as archive:
        archive.writestr('fabric.mod.json', json.dumps({
            'id': mod_id, 'version': version, 'depends': {'minecraft': '26.2.x'}}))


class VoicechatTest(unittest.TestCase):
    def setUp(self):
        temp = tempfile.TemporaryDirectory()
        self.addCleanup(temp.cleanup)
        self.root = Path(temp.name)
        self.mods = self.root / 'profile/mods'
        self.mods.mkdir(parents=True)
        self.source = self.root / 'voicechat-fabric-2.6.22+26.2.jar'
        jar(self.source, 'voicechat')
        self.spec = {**voicechat.manifest(), 'sha512': hashlib.sha512(self.source.read_bytes()).hexdigest()}
        jar(self.mods / 'old-voice.jar', 'voicechat', '2.6.20+26.2')
        jar(self.mods / 'toolkit.jar', 'skyblocktoolkit')
        jar(self.mods / 'fabric-api.jar', 'fabric-api')
        config = self.mods.parent / 'config/voicechat'
        config.mkdir(parents=True)
        self.config = config / 'voicechat-client.properties'
        self.config.write_bytes(b'microphone=personal device\nonboarding_finished=true\n')
        self.before = {p.name: p.read_bytes() for p in self.mods.iterdir()}
        self.options = self.mods.parent / 'options.txt'
        self.original_options = b'key_key.attack:key.mouse.left\nkey_key.skyblocktoolkit.menu:key.keyboard.right.shift\n'
        self.options.write_bytes(self.original_options)
        for patcher in [patch.object(voicechat, 'manifest', return_value=self.spec),
                        patch.object(voicechat, 'STATUS', self.root / 'status.txt'),
                        patch.object(voicechat.toolkit_install, 'LOCK_FILE', self.root / 'install.lock'),
                        patch.object(voicechat.toolkit_install, 'MODS', self.mods)]:
            patcher.start()
            self.addCleanup(patcher.stop)

    def run_install(self):
        voicechat.install(self.source, self.mods)

    def assert_originals(self):
        self.assertEqual({p.name: p.read_bytes() for p in self.mods.iterdir()}, self.before)

    def test_replace_only_voicechat_preserve_settings_and_idempotence(self):
        with patch.object(voicechat.toolkit_install, 'active_clients', return_value=[]):
            self.run_install()
            self.assertFalse((self.mods / 'old-voice.jar').exists())
            self.assertEqual((self.mods / self.source.name).read_bytes(), self.source.read_bytes())
            for name in ('toolkit.jar', 'fabric-api.jar'):
                self.assertEqual((self.mods / name).read_bytes(), self.before[name])
            self.assertEqual(self.config.read_bytes(), b'microphone=personal device\nonboarding_finished=true\n')
            saved_options = self.options.read_text()
            self.assertTrue(saved_options.startswith(self.original_options.decode()))
            for key, value in voicechat.CONTROL_DEFAULTS.items():
                self.assertIn(key + ':' + value + '\n', saved_options)
            option_backups = list((self.mods.parent / 'toolkit-backups').rglob('options.txt'))
            self.assertEqual(len(option_backups), 1)
            self.assertEqual(option_backups[0].read_bytes(), self.original_options)
            backups = list((self.mods.parent / 'toolkit-backups').rglob('*.jar'))
            self.assertEqual(len(backups), 1)
            self.assertEqual(backups[0].read_bytes(), self.before['old-voice.jar'])
            self.run_install()
            self.assertEqual(backups, list((self.mods.parent / 'toolkit-backups').rglob('*.jar')))

    def test_bad_checksum_stops_before_touching_mods(self):
        self.source.write_bytes(b'bad download')
        with self.assertRaises(ValueError):
            self.run_install()
        self.assert_originals()

    def test_wrong_mod_rejected_even_with_matching_checksum(self):
        jar(self.source, 'different-mod')
        self.spec['sha512'] = hashlib.sha512(self.source.read_bytes()).hexdigest()
        with self.assertRaises(ValueError):
            self.run_install()
        self.assert_originals()

    def test_running_client_defers_install(self):
        with patch.object(voicechat.toolkit_install, 'active_clients', return_value=[1]):
            with self.assertRaises(SystemExit):
                self.run_install()
        self.assert_originals()
        self.assertIn('Queued:', voicechat.STATUS.read_text())

    def test_wait_installs_only_after_exit(self):
        with patch.object(voicechat.toolkit_install, 'active_clients', side_effect=[[1], [], [], []]), \
                patch.object(voicechat.time, 'sleep') as sleep:
            voicechat.install(self.source, self.mods, wait=True)
        sleep.assert_called_once_with(3)
        self.assertTrue((self.mods / self.source.name).exists())

    def test_reopened_client_leaves_originals(self):
        with patch.object(voicechat.toolkit_install, 'active_clients', side_effect=[[], [1]]):
            with self.assertRaises(RuntimeError):
                self.run_install()
        self.assert_originals()

    def test_controls_preserve_custom_and_unbound_keys(self):
        existing = 'key_key.push_to_talk:key.mouse.4\nkey_key.mute_microphone:key.keyboard.unknown\n'
        seeded = voicechat.control_options(existing)
        self.assertTrue(seeded.startswith(existing))
        self.assertNotIn('key_key.push_to_talk:key.keyboard.caps.lock', seeded)
        self.assertEqual(voicechat.control_options(seeded), seeded)

    def test_controls_failure_rolls_back_jar_and_preserves_options(self):
        original_replace = Path.replace

        def fail_options(path, dest):
            if path.name == 'options.txt.voicechat.pending':
                raise OSError('simulated options failure')
            return original_replace(path, dest)

        with patch.object(voicechat.toolkit_install, 'active_clients', return_value=[]), \
                patch.object(Path, 'replace', fail_options):
            with self.assertRaises(OSError):
                self.run_install()
        self.assert_originals()
        self.assertEqual(self.options.read_bytes(), self.original_options)

    def test_failed_replacement_restores_originals(self):
        original_replace = Path.replace

        def fail_install(path, dest):
            if path.name.endswith('.pending'):
                raise OSError('simulated failure')
            return original_replace(path, dest)

        with patch.object(voicechat.toolkit_install, 'active_clients', return_value=[]), \
                patch.object(Path, 'replace', fail_install):
            with self.assertRaises(OSError):
                self.run_install()
        self.assert_originals()

    def test_failed_final_verification_restores_originals(self):
        original_verify = voicechat.verify

        def fail_final(path, spec):
            if path == self.mods / self.source.name:
                raise ValueError('simulated installed checksum failure')
            original_verify(path, spec)

        with patch.object(voicechat.toolkit_install, 'active_clients', return_value=[]), \
                patch.object(voicechat, 'verify', fail_final):
            with self.assertRaises(ValueError):
                self.run_install()
        self.assert_originals()

    def test_prism_pack_includes_voicechat_and_excludes_personal_audio_config(self):
        instance = self.root / 'prism'
        game = instance / '.minecraft'
        (game / 'mods').mkdir(parents=True)
        (game / 'resourcepacks').mkdir()
        (game / 'config/voicechat').mkdir(parents=True)
        (game / 'config/voicechat/voicechat-client.properties').write_text('microphone=PRIVATE')
        for name in ('skyblocktoolkit.json', 'fishingpings.json', 'moneyboosterhud.json'):
            (game / 'config' / name).write_text('{}')
        (instance / 'mmc-pack.json').write_text(json.dumps({'components': [
            {'uid': 'net.minecraft', 'version': '26.2'},
            {'uid': 'net.fabricmc.fabric-loader', 'version': '0.19.5'}]}))
        jar(game / 'mods/fabric-api-0.160.0+26.2.jar', 'fabric-api')
        jar(game / 'resourcepacks/Clear-Harvest-90pct-26.2.zip', 'fixture-pack')
        (self.root / 'build').mkdir()
        jar(self.root / 'build/skyblock-toolkit-1.0.0.jar', 'skyblocktoolkit')
        for name in ('LICENSE', 'THIRD-PARTY.md', 'README.md', 'build.py',
                     'modpack/voicechat.json', 'scripts/voicechat.py', 'scripts/install.py', 'scripts/local_paths.py'):
            path = self.root / name
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text('fixture')
        output = self.root / 'pack.zip'
        with patch.object(package_prism, 'PROJECT', self.root), \
                patch.object(package_prism, 'fetch_voicechat', return_value=self.source), \
                patch.object(sys, 'argv', ['package_prism.py', '--instance', str(instance), '--output', str(output)]):
            package_prism.main()
        with zipfile.ZipFile(output) as archive:
            self.assertEqual(archive.read('.minecraft/mods/' + self.source.name), self.source.read_bytes())
            self.assertFalse(any('voicechat-client.properties' in name for name in archive.namelist()))
            self.assertIn('Max Henkel', archive.read('READ-ME.txt').decode())
            packed_options = archive.read('.minecraft/options.txt').decode()
            self.assertIn('key_key.push_to_talk:key.keyboard.caps.lock\n', packed_options)
            self.assertIn('key_key.mute_microphone:key.keyboard.f8\n', packed_options)
            self.assertIn('version:4903\n', packed_options)


if __name__ == '__main__':
    unittest.main()
