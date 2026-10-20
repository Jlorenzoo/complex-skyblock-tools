#!/usr/bin/env python3
"""Fetch or safely install the pack's pinned, unmodified Simple Voice Chat mod."""
import argparse
import datetime
import hashlib
import json
from pathlib import Path
import shutil
import time
import urllib.request
import zipfile

import install as toolkit_install

PROJECT = Path(__file__).resolve().parents[1]
MANIFEST = PROJECT / 'modpack/voicechat.json'
STATUS = PROJECT / 'voicechat-installation-status.txt'
CONTROL_DEFAULTS = {
    'key_key.voice_chat': 'key.keyboard.v',
    'key_key.push_to_talk': 'key.keyboard.caps.lock',
    'key_key.whisper': 'key.keyboard.right.alt',
    'key_key.mute_microphone': 'key.keyboard.f8',
    'key_key.disable_voice_chat': 'key.keyboard.n',
    'key_key.hide_icons': 'key.keyboard.h',
    'key_key.voice_chat_settings': 'key.keyboard.unknown',
    'key_key.voice_chat_group': 'key.keyboard.unknown',
    'key_key.voice_chat_toggle_recording': 'key.keyboard.unknown',
    'key_key.voice_chat_adjust_volumes': 'key.keyboard.unknown',
}


def control_options(existing=''):
    """Seed only missing voice keys; retain all player settings, including unbound keys."""
    present = {line.split(':', 1)[0] for line in existing.splitlines()}
    # Without the options data version, Minecraft treats modern key names as
    # pre-LWJGL3 numeric bindings and discards them during migration.
    defaults = {'version': '4903', **CONTROL_DEFAULTS}  # Minecraft 26.2 data version
    additions = ''.join(key + ':' + value + '\n' for key, value in defaults.items()
                        if key not in present)
    if not additions:
        return existing
    return existing + ('\n' if existing and not existing.endswith('\n') else '') + additions


def install_controls(game, backup_dir):
    options = game / 'options.txt'
    original = options.read_bytes() if options.exists() else None
    updated = control_options(original.decode('utf-8') if original is not None else '').encode('utf-8')
    if updated == original:
        return
    if original is not None:
        backup_dir.mkdir(parents=True, exist_ok=True)
        backup = backup_dir / 'options.txt'
        backup.write_bytes(original)
        if backup.read_bytes() != original:
            raise RuntimeError('Options backup verification failed')
    temporary = game / 'options.txt.voicechat.pending'
    replaced = False
    try:
        temporary.write_bytes(updated)
        if toolkit_install.active_clients():
            raise RuntimeError('Minecraft reopened before controls were saved; rerun after closing it.')
        temporary.replace(options)
        replaced = True
        if options.read_bytes() != updated:
            raise RuntimeError('Voice control verification failed')
    except Exception:
        if replaced:
            if original is None:
                options.unlink(missing_ok=True)
            else:
                temporary.write_bytes(original)
                temporary.replace(options)
        raise
    finally:
        temporary.unlink(missing_ok=True)


def manifest():
    return json.loads(MANIFEST.read_text(encoding='utf-8'))


def verify(path, spec):
    if hashlib.sha512(path.read_bytes()).hexdigest() != spec['sha512']:
        raise ValueError('Simple Voice Chat checksum mismatch: ' + str(path))
    with zipfile.ZipFile(path) as archive:
        meta = json.loads(archive.read('fabric.mod.json'))
        if (archive.testzip() is not None or meta['id'] != 'voicechat'
                or meta['version'] != spec['version']
                or meta['depends']['minecraft'] != spec['minecraft'] + '.x'):
            raise ValueError('Unexpected Simple Voice Chat Fabric metadata')


def fetch():
    spec = manifest()
    dest = PROJECT / 'build/dependencies' / spec['filename']
    if dest.exists():
        verify(dest, spec)
        return dest
    dest.parent.mkdir(parents=True, exist_ok=True)
    # A private temporary file also allows packaging and installation concurrently.
    import tempfile
    with tempfile.NamedTemporaryFile(dir=dest.parent, suffix='.download', delete=False) as temp:
        temporary = Path(temp.name)
    try:
        request = urllib.request.Request(spec['url'], headers={
            'User-Agent': 'Jlorenzoo/complex-skyblock-tools (Simple Voice Chat pack dependency)'})
        with urllib.request.urlopen(request, timeout=60) as response, temporary.open('wb') as output:
            shutil.copyfileobj(response, output)
        verify(temporary, spec)
        temporary.replace(dest)
    finally:
        temporary.unlink(missing_ok=True)
    return dest


def status(message):
    temporary = STATUS.with_suffix('.tmp')
    temporary.write_text(message + '\n', encoding='utf-8')
    temporary.replace(STATUS)


def install(source, mods, wait=False):
    spec = manifest()
    verify(source, spec)
    if not mods.is_dir():
        raise SystemExit('Mods directory does not exist: ' + str(mods))
    # Share the toolkit installer's profile detection and exclusive lock.
    toolkit_install.MODS = mods
    with toolkit_install.installer_lock():
        deadline = time.monotonic() + 86400
        while toolkit_install.active_clients():
            status('Queued: Simple Voice Chat ' + spec['version'] +
                   ' and voice controls will install after Minecraft closes. Restart, then press V for setup.')
            if not wait:
                raise SystemExit('Close Minecraft and run again, or use --wait.')
            if time.monotonic() > deadline:
                status('Wait expired. Close Minecraft and rerun scripts/voicechat.py --install.')
                raise SystemExit(1)
            time.sleep(3)

        originals = []
        for path in mods.glob('*.jar'):
            try:
                if toolkit_install.mod_id(path) == 'voicechat':
                    originals.append(path)
            except (zipfile.BadZipFile, json.JSONDecodeError, KeyError):
                if path.name.lower().startswith('voicechat'):
                    raise SystemExit('Could not identify existing voice chat jar: ' + path.name)
        dest = mods / spec['filename']
        if dest.exists() and dest not in originals:
            raise SystemExit('Target filename belongs to an unidentified mod: ' + dest.name)
        stamp = datetime.datetime.now().strftime('%Y%m%d-%H%M%S-%f')
        backup_dir = mods.parent / 'toolkit-backups' / ('voicechat-' + stamp)
        if originals == [dest] and toolkit_install.digest(dest) == toolkit_install.digest(source):
            install_controls(mods.parent, backup_dir)
            status('Simple Voice Chat ' + spec['version'] + ' is already installed. Press V for setup.')
            return

        # Backups live outside mods, beyond the toolkit updater's .bak cleanup.
        backups = {}
        for path in originals:
            backup_dir.mkdir(parents=True, exist_ok=True)
            backup = backup_dir / path.name
            shutil.copy2(path, backup)
            if toolkit_install.digest(backup) != toolkit_install.digest(path):
                raise RuntimeError('Backup verification failed: ' + path.name)
            backups[path] = backup

        temporary = dest.with_suffix('.jar.pending')
        removed = []
        installed = False
        try:
            shutil.copy2(source, temporary)
            verify(temporary, spec)
            if toolkit_install.active_clients():
                raise RuntimeError('Minecraft reopened during preparation; rerun after closing it.')
            for path in originals:
                path.unlink()
                removed.append(path)
            temporary.replace(dest)
            installed = True
            verify(dest, spec)
            install_controls(mods.parent, backup_dir)
            status('Installed Simple Voice Chat ' + spec['version'] +
                   '. Defaults: V menu, Caps Lock push-to-talk, Right Alt whisper, F8 mute. '
                   'Existing voice bindings preserved. Press V to select your microphone. '
                   'On Complex, use /voice and accept the server consent prompt yourself.')
        except Exception as error:
            if installed:
                dest.unlink(missing_ok=True)
            for path in removed:
                shutil.copy2(backups[path], path)
            status('Installation stopped; original mods restored: ' + str(error))
            raise
        finally:
            temporary.unlink(missing_ok=True)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--install', action='store_true', help='Install in the configured live profile')
    parser.add_argument('--wait', action='store_true', help='Wait for Minecraft to exit before installing')
    parser.add_argument('--mods', type=Path, help='Override the configured mods directory')
    args = parser.parse_args()
    source = fetch()
    print('Verified: ' + str(source), flush=True)
    if args.install:
        install(source, args.mods or toolkit_install.MODS, args.wait)
        print(STATUS.read_text(encoding='utf-8').strip())


if __name__ == '__main__':
    main()
