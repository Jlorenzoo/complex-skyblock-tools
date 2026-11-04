"""Regression checks for desktop isolation and fail-closed graphical launches."""
import os
from pathlib import Path
import subprocess
import sys
import unittest
from unittest.mock import patch

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / 'scripts'))
import test_display


class DisplayTests(unittest.TestCase):
    def test_missing_xvfb_never_launches_client(self):
        with patch.object(test_display.shutil, 'which', return_value=None), \
                patch.object(Path, 'is_file', return_value=False), \
                patch.object(test_display.subprocess, 'run') as run:
            with self.assertRaisesRegex(RuntimeError, 'Xvfb is required'):
                test_display.run_graphical(['must-not-launch'])
            run.assert_not_called()

    def test_windows_never_launches_on_active_desktop(self):
        with patch.object(test_display.sys, 'platform', 'win32'), \
                patch.object(test_display.subprocess, 'run') as run:
            with self.assertRaisesRegex(RuntimeError, 'active desktop is disabled'):
                test_display.run_graphical(['must-not-launch'])
            run.assert_not_called()

    @unittest.skipUnless(sys.platform.startswith('linux'), 'Linux Xvfb integration')
    def test_real_private_display_and_timeout_cleanup(self):
        # Include hostile inherited display choices: none may reach GLFW.
        original = os.environ.get('DISPLAY')
        with patch.dict(os.environ, {'WAYLAND_DISPLAY': 'player-wayland',
                                    'WAYLAND_SOCKET': '42', 'XAUTHORITY': '/player/auth'}):
            with test_display.private_display() as env:
                self.assertNotEqual(env['DISPLAY'], original)
                self.assertNotIn('WAYLAND_DISPLAY', env)
                self.assertNotIn('WAYLAND_SOCKET', env)
                self.assertNotIn('XAUTHORITY', env)
                probe = '''import ctypes
x = ctypes.CDLL('libX11.so.6')
x.XOpenDisplay.restype = ctypes.c_void_p
assert x.XOpenDisplay(None), 'Cannot connect to virtual X server'
'''
                subprocess.run([sys.executable, '-c', probe], env=env, check=True)
            # A timed-out client must be reaped before its private server goes away.
            with self.assertRaises(subprocess.TimeoutExpired):
                test_display.run_graphical([sys.executable, '-c', 'import time; time.sleep(10)'], timeout=.1)
        self.assertEqual(os.environ.get('DISPLAY'), original)


if __name__ == '__main__':
    unittest.main()
