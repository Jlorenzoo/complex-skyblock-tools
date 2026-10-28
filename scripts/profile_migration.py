"""Copy player data to a separate profile; never edit the source profile.

Launcher registration and mod downloads are kept separate from player data.
Call only after the source client exits for the final, consistent snapshot.
"""
import gzip
import hashlib
import json
from pathlib import Path
import shutil
import zipfile

# These are launcher/runtime caches, old binaries and redundant backups, rather
# than player preferences. Worlds are copied but must not be opened downgraded.
EXCLUDED = {'.fabric', '.curseclient', 'minecraftinstance.json', 'mods', 'logs',
            'crash-reports', 'downloads', 'debug-profile.json', 'toolkit-backups'}


def digest(path):
    checksum = hashlib.sha256()
    with Path(path).open('rb') as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b''):
            checksum.update(chunk)
    return checksum.hexdigest()


def options_for_1_21_11(text):
    """All controls/preferences stay verbatim; only the options data version changes."""
    lines = text.splitlines(keepends=True)
    found = False
    for i, line in enumerate(lines):
        if line.startswith('version:'):
            lines[i] = 'version:4671' + ('\r\n' if line.endswith('\r\n') else '\n')
            found = True
    return ''.join(lines) if found else 'version:4671\n' + text


def adapt_crop_pack(path):
    """Adapt the known Ready Crops art pack; keep all texture/model asset bytes."""
    if path.name != 'Ready Crops - Color and Size.zip':
        return False
    with zipfile.ZipFile(path) as archive:
        entries = [(info, archive.read(info)) for info in archive.infolist()]
    metadata = next((data for info, data in entries if info.filename == 'pack.mcmeta'), None)
    if metadata is None:
        raise ValueError('Crop resource pack has no metadata')
    if any(info.filename.startswith('data/') for info, _ in entries):
        raise ValueError('Unexpected data pack assets in the crop pack')
    pack = json.loads(metadata)
    pack['pack']['min_format'] = [75, 0]
    pack['pack']['max_format'] = [75, 0]
    temporary = path.with_suffix('.zip.pending')
    try:
        with zipfile.ZipFile(temporary, 'w') as archive:
            for info, data in entries:
                archive.writestr(info, json.dumps(pack, indent=2).encode() if info.filename == 'pack.mcmeta' else data)
        with zipfile.ZipFile(temporary) as archive:
            if archive.testzip() is not None:
                raise ValueError('Adapted crop resource pack failed verification')
        temporary.replace(path)
    finally:
        temporary.unlink(missing_ok=True)
    return True


def adapt_schematic(path, target_version=4671):
    """Adapt Litematica schematic data version if it is newer than target Minecraft version."""
    path = Path(path)
    if path.suffix != '.litematic':
        return False
    try:
        with gzip.open(path, 'rb') as stream:
            data = stream.read()
    except Exception:
        return False
    marker = b'\x03\x00\x14MinecraftDataVersion'
    index = data.find(marker)
    if index == -1:
        return False
    value_start = index + len(marker)
    if len(data) < value_start + 4:
        return False
    value = int.from_bytes(data[value_start:value_start + 4], 'big')
    if value <= target_version:
        return False
    adapted = data[:value_start] + target_version.to_bytes(4, 'big') + data[value_start + 4:]
    temporary = path.with_suffix('.litematic.pending')
    try:
        with gzip.open(temporary, 'wb', compresslevel=9) as stream:
            stream.write(adapted)
        with gzip.open(temporary, 'rb') as stream:
            if stream.read() != adapted:
                raise ValueError('Adapted schematic failed verification')
        temporary.replace(path)
    finally:
        temporary.unlink(missing_ok=True)
    return True


def copy_player_data(source, destination, profile_path=None):
    source, destination = Path(source).resolve(), Path(destination).resolve()
    if not source.is_dir():
        raise ValueError('Source profile does not exist')
    if source == destination or source in destination.parents or destination in source.parents:
        raise ValueError('Source and destination must be separate profiles')
    if destination.exists() and any(destination.iterdir()):
        raise ValueError('Destination must be empty; existing player data will not be overwritten')
    destination.mkdir(parents=True, exist_ok=True)
    records = {}
    for item in sorted(source.rglob('*')):
        relative = item.relative_to(source)
        if relative.parts[0] in EXCLUDED or relative.parts[0].startswith('_backup'):
            continue
        if item.is_symlink() or item.is_junction():
            raise ValueError('Linked player data requires explicit handling: ' + str(relative))
        if not item.is_file():
            continue
        target = destination / relative
        target.parent.mkdir(parents=True, exist_ok=True)
        original = digest(item)
        shutil.copy2(item, target)
        if digest(target) != original or digest(item) != original:
            raise RuntimeError('Player data changed while copying: ' + str(relative))
        records[relative.as_posix()] = {'source_sha256': original, 'installed_sha256': original}
    options = destination / 'options.txt'
    if options.exists():
        # Decode explicitly so every line except data-version stays byte-for-byte.
        options.write_bytes(options_for_1_21_11(options.read_bytes().decode('utf-8')).encode('utf-8'))
        records['options.txt']['installed_sha256'] = digest(options)
    for pack in (destination / 'resourcepacks').glob('*.zip'):
        if adapt_crop_pack(pack):
            records[pack.relative_to(destination).as_posix()]['installed_sha256'] = digest(pack)
    for schematic in (destination / 'schematics').rglob('*.litematic'):
        if adapt_schematic(schematic):
            records[schematic.relative_to(destination).as_posix()]['installed_sha256'] = digest(schematic)
    # Litematica stores absolute schematic paths. Point copied configurations at
    # the copied files, so future edits do not keep using the fallback profile.
    final_profile = Path(profile_path or destination).resolve()
    def retarget(value):
        if isinstance(value, dict):
            return {key: retarget(item) for key, item in value.items()}
        if isinstance(value, list):
            return [retarget(item) for item in value]
        if isinstance(value, str):
            for old, new in ((str(source), str(final_profile)), (source.as_posix(), final_profile.as_posix())):
                if value.lower() == old.lower() or value.lower().startswith(old.lower() + ('/' if '/' in old else '\\')):
                    return new + value[len(old):]
            if value.lower().endswith('.litematic') or '/schematics/' in value.replace('\\', '/').lower():
                schematic_name = Path(value).name
                if (destination / 'schematics' / schematic_name).exists():
                    return (final_profile / 'schematics' / schematic_name).as_posix()
        return value
    for config in (destination / 'config').rglob('*.json'):
        try:
            original = json.loads(config.read_text(encoding='utf-8'))
        except ValueError:
            continue
        updated = retarget(original)
        if original != updated:
            config.write_text(json.dumps(updated, indent=2) + '\n', encoding='utf-8')
            records[config.relative_to(destination).as_posix()]['installed_sha256'] = digest(config)
    return records
