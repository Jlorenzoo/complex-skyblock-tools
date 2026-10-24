#!/usr/bin/env python3
"""Make a portable Prism instance without account data or personal history."""
import argparse
import hashlib
import json
from pathlib import Path
import zipfile
from voicechat import fetch as fetch_voicechat, control_options

PROJECT = Path(__file__).resolve().parents[1]
DEFAULT_INSTANCE = Path.home() / '.var/app/org.prismlauncher.PrismLauncher/data/PrismLauncher/instances/Complex-Skyblock-2'


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--instance', type=Path, default=DEFAULT_INSTANCE)
    parser.add_argument('--output', type=Path, default=PROJECT / 'dist/Complex-Skyblock-Prism-1.1.0.zip')
    args = parser.parse_args()
    # This explicitly retained export is the historical 26.2 pack. The maintained
    # 1.21.11 profile uses the Gradle build and its own dependency manifest.
    game = args.instance / '.minecraft'
    jar = PROJECT / 'build/skyblock-toolkit-1.1.0.jar'
    api = game / 'mods/fabric-api-0.160.0+26.2.jar'
    pack = game / 'resourcepacks/Clear-Harvest-90pct-26.2.zip'
    components = json.loads((args.instance / 'mmc-pack.json').read_text())
    versions = {c['uid']: c['version'] for c in components['components']}
    assert versions['net.minecraft'] == '26.2'
    assert versions['net.fabricmc.fabric-loader'] == '0.19.5'
    for path in (jar, api, pack):
        with zipfile.ZipFile(path) as archive:
            assert archive.testzip() is None, path
    with zipfile.ZipFile(jar) as archive:
        metadata = json.loads(archive.read('fabric.mod.json'))
        assert metadata['id'] == 'skyblocktoolkit' and metadata['version'] == '1.1.0'
    voicechat = fetch_voicechat(minecraft='26.2')

    files = {
        '.minecraft/mods/' + jar.name: jar,
        '.minecraft/mods/' + api.name: api,
        '.minecraft/mods/' + voicechat.name: voicechat,
        '.minecraft/resourcepacks/' + pack.name: pack,
    }
    for name in ('skyblocktoolkit.json', 'fishingpings.json', 'moneyboosterhud.json'):
        path = game / 'config' / name
        json.loads(path.read_text())
        files['.minecraft/config/' + name] = path
    # Corresponding GPL source and attribution travel with the binary.
    for path in sorted((PROJECT / 'src').rglob('*')):
        if path.is_file():
            files['toolkit-source/' + path.relative_to(PROJECT).as_posix()] = path
    for name in ('LICENSE', 'THIRD-PARTY.md', 'README.md', 'CHANGELOG.md', 'build.py',
                 'modpack/voicechat.json', 'modpack/voicechat-26.2.json', 'scripts/voicechat.py', 'scripts/install.py', 'scripts/local_paths.py'):
        files['toolkit-source/' + name] = PROJECT / name

    readme = '''COMPLEX SKYBLOCK - PRISM PACK

Import this ZIP without extracting it:
Prism Launcher > Add Instance > Import > Browse > choose this ZIP > OK.
Then launch the new Complex Skyblock Toolkit instance using your own account.
Prism downloads Minecraft 26.2 and Fabric Loader 0.19.5. Java 25 is required;
use Prism's automatic Java management or select Java 25 if prompted.

Included: Skyblock Toolkit 1.1.0, Fabric API 0.160.0+26.2, Simple Voice Chat 2.6.22+26.2,
Clear Harvest crop visibility texture pack (built-in & enabled), and toolkit settings.
Press Right Shift in-game to open the Toolkit (toggle crop textures in Right Shift > Farming).
Simple Voice Chat is by Max Henkel: https://modrinth.com/plugin/simple-voice-chat
Controls: V menu, hold Caps Lock to talk, hold Right Alt to whisper, F8 mute, N disable voice.
Press V to select your own microphone/speakers and choose push-to-talk.
On Complex, /voice opens server settings; accept its consent prompt yourself.
Complex records/moderates voice chat. Range: 48 blocks (whisper: 24); groups disabled.
Lunar is not required. No microphone settings or completed onboarding are bundled.
Fish sell-price estimates use the sender's configured prices; adjust them
in Fishing > Fish earnings if your server prices differ.

No accounts, chats, worlds, server list, earnings history or old mod backups
are included. Connect to your server yourself from Multiplayer.
Toolkit GPL source and licenses are in toolkit-source.
'''
    args.output.parent.mkdir(parents=True, exist_ok=True)
    temporary = args.output.with_suffix('.tmp')
    with zipfile.ZipFile(temporary, 'w', compression=zipfile.ZIP_DEFLATED) as archive:
        archive.writestr('instance.cfg', '[General]\nConfigVersion=1.3\nInstanceType=OneSix\nname=Complex Skyblock Toolkit\niconKey=default\nOverrideJavaLocation=false\nOverrideCommands=false\nOverrideMemory=false\n')
        archive.writestr('mmc-pack.json', json.dumps(components, indent=2) + '\n')
        archive.writestr('.minecraft/options.txt', control_options('resourcePacks:["vanilla","skyblocktoolkit:clear_harvest","file/' + pack.name + '"]\nincompatibleResourcePacks:[]\n', minecraft='26.2'))
        archive.writestr('READ-ME.txt', readme)
        for name, path in sorted(files.items()):
            archive.write(path, name)
    with zipfile.ZipFile(temporary) as archive:
        assert archive.testzip() is None
        for name, path in files.items():
            assert hashlib.sha256(archive.read(name)).digest() == hashlib.sha256(path.read_bytes()).digest()
        assert len([n for n in archive.namelist() if n.startswith('.minecraft/mods/')]) == 3
        assert all(not n.startswith('/') and '..' not in Path(n).parts for n in archive.namelist())
    temporary.replace(args.output)
    print(f'Created and byte-verified: {args.output} ({args.output.stat().st_size:,} bytes)')


if __name__ == '__main__':
    main()
