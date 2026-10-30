"""Exercise actual version generation and release publication without public writes."""
import json
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest
from unittest.mock import patch
import zipfile

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / 'scripts'))
import build_version
import publish_patch


class PatchDeliveryTest(unittest.TestCase):
    def test_every_commit_advances_a_numeric_patch_and_shallow_history_is_rejected(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            subprocess.run(['git', 'init', '-q', str(root)], check=True)
            subprocess.run(['git', '-C', str(root), 'config', 'user.email', 'test@example.invalid'], check=True)
            subprocess.run(['git', '-C', str(root), 'config', 'user.name', 'Test'], check=True)
            metadata = root / 'src/main/resources/fabric.mod.json'
            metadata.parent.mkdir(parents=True)
            metadata.write_text(json.dumps({'version': '1.1.0'}))
            subprocess.run(['git', '-C', str(root), 'add', '.'], check=True)
            subprocess.run(['git', '-C', str(root), 'commit', '-qm', 'First'], check=True)
            with patch.object(build_version, 'ROOT', root):
                self.assertEqual(build_version.build_version(), '1.1.1')
                subprocess.run(['git', '-C', str(root), 'commit', '--allow-empty', '-qm', 'Patch'], check=True)
                self.assertEqual(build_version.build_version(), '1.1.2')
                metadata.write_text(json.dumps({'version': '1.2.0'}))
                self.assertEqual(build_version.build_version(), '1.2.2')
            shallow = root / 'shallow'
            subprocess.run(['git', 'clone', '-q', '--depth=1', root.as_uri(), str(shallow)], check=True)
            with patch.object(build_version, 'ROOT', shallow), self.assertRaisesRegex(RuntimeError, 'full Git history'):
                build_version.build_version()

    def test_publication_validates_real_jar_and_uploads_before_leaving_draft(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            jar = root / 'versions/1.21.11/build/libs/skyblock-toolkit-1.1.44+mc1.21.11.jar'
            jar.parent.mkdir(parents=True)
            with zipfile.ZipFile(jar, 'w') as archive:
                archive.writestr('fabric.mod.json', json.dumps({'id': 'skyblocktoolkit', 'version': '1.1.44', 'depends': {'minecraft': '~1.21.11'}}))
            calls = []
            def run(args, **kwargs):
                calls.append(args)
                return subprocess.CompletedProcess(args, 1 if args[1:3] == ['release', 'view'] else 0)
            with patch.object(publish_patch, '__file__', str(root / 'scripts/publish_patch.py')), \
                    patch.object(publish_patch, 'build_version', return_value='1.1.44'), \
                    patch.object(publish_patch.subprocess, 'check_output', return_value='abc123\n'), \
                    patch.object(publish_patch.subprocess, 'run', side_effect=run):
                publish_patch.publish('1.21.11')
            self.assertEqual([c[2] for c in calls], ['view', 'create', 'upload', 'edit'])
            self.assertIn('--draft', calls[1])
            self.assertIn('--draft=false', calls[-1])
            self.assertIn('--latest=false', calls[-1])
            self.assertIn('v1.1.44-mc1.21.11', calls[-1])
            uploaded = Path(calls[2][4])
            self.assertEqual(uploaded.name, jar.name)
            self.assertEqual(uploaded.read_bytes(), jar.read_bytes())
            checksum = Path(calls[2][5]).read_text()
            import hashlib
            self.assertEqual(checksum, hashlib.sha256(jar.read_bytes()).hexdigest() + '  ' + jar.name + '\n')


if __name__ == '__main__':
    unittest.main()
