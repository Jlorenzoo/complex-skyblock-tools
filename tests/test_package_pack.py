"""Test multi-version package generator offline."""
import hashlib
import json
from pathlib import Path
import sys
import tempfile
import unittest
from unittest.mock import patch
import zipfile

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / 'scripts'))
import package_pack


class PackagePackTest(unittest.TestCase):
    def setUp(self):
        tmp = tempfile.TemporaryDirectory()
        self.addCleanup(tmp.cleanup)
        self.root = Path(tmp.name)

    def write_dummy_jar(self, path, mc_version):
        path.parent.mkdir(parents=True, exist_ok=True)
        with zipfile.ZipFile(path, 'w') as archive:
            archive.writestr('fabric.mod.json', json.dumps({
                'id': 'skyblocktoolkit',
                'version': '1.1.0',
                'depends': {'minecraft': f'~{mc_version}'}
            }))

    def test_package_pack_generates_valid_prism_and_modrinth(self):
        for mc in ['1.21.11', '26.2']:
            spec = package_pack.manifest(mc)
            deps = {}
            for mod in spec['mods']:
                dummy_file = self.root / mod['filename']
                dummy_file.write_bytes(b'dummy-content')
                mod['sha512'] = hashlib.sha512(dummy_file.read_bytes()).hexdigest()
                deps[mod['filename']] = dummy_file

            jar = self.root / f'skyblock-toolkit-1.1.0+mc{mc}.jar'
            self.write_dummy_jar(jar, mc)

            out = self.root / f'out-{mc}'
            with patch.object(package_pack, 'manifest', return_value=spec), \
                 patch.object(package_pack, 'fetch_dependencies', return_value=deps), \
                 patch.object(package_pack, 'source_files', return_value={}):
                package_pack.package(mc, jar, out)

            prism_zip = out / f'Full-Pack-Prism-{mc}.zip'
            mrpack = out / f'Full-Pack-Modrinth-{mc}.mrpack'
            self.assertTrue(prism_zip.is_file())
            self.assertTrue(mrpack.is_file())

            with zipfile.ZipFile(prism_zip) as z:
                self.assertIsNone(z.testzip())
                components = json.loads(z.read('mmc-pack.json'))['components']
                self.assertEqual(components[0]['version'], mc)

            with zipfile.ZipFile(mrpack) as z:
                self.assertIsNone(z.testzip())
                index = json.loads(z.read('modrinth.index.json'))
                self.assertEqual(index['dependencies']['minecraft'], mc)


if __name__ == '__main__':
    unittest.main()
