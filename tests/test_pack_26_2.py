"""Exercise wrong-version rejection, clean packs and dependency integrity offline."""
import hashlib
import json
from pathlib import Path
import sys
import tempfile
import unittest
from unittest.mock import patch
import zipfile

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / 'scripts'))
import fabric_26_2
import package_26_2


class PackTest(unittest.TestCase):
    def setUp(self):
        tmp = tempfile.TemporaryDirectory()
        self.addCleanup(tmp.cleanup)
        self.root = Path(tmp.name)
        self.jar = self.root / 'toolkit.jar'
        self.write_jar('~26.2')

    def write_jar(self, version):
        with zipfile.ZipFile(self.jar, 'w') as archive:
            archive.writestr('fabric.mod.json', json.dumps({'id': 'skyblocktoolkit', 'version': '1.1.0',
                'depends': {'minecraft': version, 'java': '>=25'}}))

    def test_wrong_minecraft_build_is_rejected(self):
        self.write_jar('~1.21.11')
        with self.assertRaises(ValueError):
            package_26_2.package(self.root / 'out', self.jar, {})
        self.assertFalse((self.root / 'out').exists())

    def test_prism_and_modrinth_contain_matching_clean_pack(self):
        specs = fabric_26_2.manifest()
        deps = {}
        for mod in specs['mods']:
            path = self.root / mod['filename']
            path.write_bytes(mod['project'].encode())
            mod['sha512'] = hashlib.sha512(path.read_bytes()).hexdigest()
            deps[mod['filename']] = path
        out = self.root / 'out'
        with patch.object(package_26_2, 'manifest', return_value=specs), \
                patch.object(package_26_2, 'fetch_mod', side_effect=lambda mod: deps[mod['filename']]):
            package_26_2.package(out, self.jar, {'build.py': self.jar})
        with zipfile.ZipFile(next(out.glob('*.zip'))) as prism, zipfile.ZipFile(next(out.glob('*.mrpack'))) as mrpack:
            self.assertEqual(len([n for n in prism.namelist() if n.startswith('.minecraft/mods/')]), 7)
            self.assertEqual(prism.read('.minecraft/mods/' + package_26_2.JAR_NAME), self.jar.read_bytes())
            components = json.loads(prism.read('mmc-pack.json'))['components']
            self.assertEqual(components[0]['version'], '26.2')
            index = json.loads(mrpack.read('modrinth.index.json'))
            self.assertEqual(index['dependencies'], {'minecraft': '26.2', 'fabric-loader': '0.19.5'})
            self.assertEqual(len(index['files']), 6)
            for entry in index['files']:
                data = prism.read('.minecraft/' + entry['path'])
                self.assertEqual(hashlib.sha512(data).hexdigest(), entry['hashes']['sha512'])
                self.assertEqual(len(data), entry['fileSize'])
            self.assertEqual(mrpack.read('overrides/mods/' + package_26_2.JAR_NAME), self.jar.read_bytes())
            options = prism.read('.minecraft/options.txt').decode()
            self.assertIn('version:4903\n', options)
            self.assertIn('key_key.push_to_talk:key.keyboard.caps.lock', options)
            self.assertIn('skyblocktoolkit:clear_harvest', options)
            self.assertFalse(any('voicechat-client.properties' in n or 'accounts' in n or 'history.json' in n for n in prism.namelist()))
        for line in (out / 'SHA256SUMS.txt').read_text().splitlines():
            digest, name = line.split('  ')
            self.assertEqual(hashlib.sha256((out / name).read_bytes()).hexdigest(), digest)

    def test_corrupt_cached_dependency_is_rejected(self):
        path = self.root / 'dependency.jar'
        path.write_bytes(b'corrupt')
        response = unittest.mock.MagicMock()
        response.__enter__.return_value.read.return_value = b'wrong download'
        with patch.object(fabric_26_2.urllib.request, 'urlopen', return_value=response):
            with self.assertRaises(ValueError):
                fabric_26_2.fetch('https://example.test/mod.jar', path, hashlib.sha1(b'correct').hexdigest())
        self.assertEqual(path.read_bytes(), b'corrupt')


if __name__ == '__main__':
    unittest.main()
