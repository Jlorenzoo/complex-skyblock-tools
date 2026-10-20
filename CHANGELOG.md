# Changelog

## 1.1.0 — 2026-10-20

- Add Simple Voice Chat 2.6.22+26.2 to the Prism pack for Complex proximity chat without Lunar.
- Configure V for the voice menu, Caps Lock for push-to-talk, Right Alt for whisper,
  F8 for microphone mute, N for voice chat toggle, and H for voice icons.
  F8 avoids Litematica's M menu binding.
- Add a checksum-verified voice chat installer that waits for Minecraft to close,
  preserves existing voice bindings and microphone settings, and backs up changed files.
- Verify voice initialization, exact keybindings and onboarding in a real offline Fabric client.

The standalone toolkit jar does not bundle Simple Voice Chat. Existing toolkit users
can install the separate dependency with `python scripts/voicechat.py --install --wait`
from the source distribution, or import the complete Prism pack. Microphone selection
and Complex's recording consent remain part of the player's first-run setup.
