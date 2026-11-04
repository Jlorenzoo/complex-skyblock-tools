"""Launch graphical checks away from the player's keyboard, pointer and desktop."""
from contextlib import contextmanager
import os
from pathlib import Path
import select
import shutil
import subprocess
import sys
import tempfile

ROOT = Path(__file__).resolve().parents[1]


def require_test_display():
    """Fail closed on platforms without an implemented private display backend."""
    if not sys.platform.startswith('linux'):
        raise RuntimeError('Graphical tests require a private Linux Xvfb display. '
                           'Run them on Linux/CI; launching on your active desktop is disabled.')
    binary = shutil.which('Xvfb')
    local = ROOT / '.local/xvfb/usr/bin/Xvfb'
    if not binary and local.is_file():
        binary = str(local)
    if not binary:
        raise RuntimeError('Xvfb is required for graphical tests. Install the xvfb package '
                           'or extract it into .local/xvfb. No desktop client was launched.')
    return binary


@contextmanager
def private_display():
    binary = require_test_display()
    read_fd, write_fd = os.pipe()
    process = None
    try:
        with tempfile.TemporaryFile(mode='w+') as log:
            try:
                process = subprocess.Popen(
                    [binary, '-displayfd', str(write_fd), '-screen', '0', '1600x1000x24',
                     '-nolisten', 'tcp', '-noreset', '+extension', 'GLX'],
                    pass_fds=(write_fd,), stdout=log, stderr=log)
            finally:
                os.close(write_fd)
            if not select.select([read_fd], [], [], 15)[0]:
                raise RuntimeError('Private Xvfb display did not start; desktop fallback is disabled.')
            number = os.read(read_fd, 64).decode().strip()
            if not number.isdecimal() or process.poll() is not None:
                log.seek(0)
                raise RuntimeError('Private Xvfb display failed: ' + log.read()[-2000:])
            env = dict(os.environ)
            # GLFW must not select the real Wayland session instead of private X11.
            for key in ('WAYLAND_DISPLAY', 'WAYLAND_SOCKET', 'XAUTHORITY'):
                env.pop(key, None)
            env.update(DISPLAY=':' + number, XDG_SESSION_TYPE='x11',
                       ALSOFT_DRIVERS='null', LIBGL_ALWAYS_SOFTWARE='1')
            print('Rendering on private virtual display ' + env['DISPLAY'], flush=True)
            yield env
    finally:
        os.close(read_fd)
        if process is not None:
            process.terminate()
            try:
                process.wait(timeout=5)
            except subprocess.TimeoutExpired:
                process.kill()
                process.wait()


def run_graphical(command, **kwargs):
    """Keep the private server alive for the client, including timeout cleanup."""
    with private_display() as env:
        return subprocess.run(command, env=env, **kwargs)
