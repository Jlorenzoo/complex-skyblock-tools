# Player-facing releases

Keep GitHub release pages short and easy to use without programming knowledge.

- Use the display title `v1 - 26.2` or `v1 - 1.21.11` for the current v1 family.
  Keep existing Git tags and mod metadata unchanged for presentation-only edits.
- Put a direct **Download the mod** link first, followed by three short install
  steps and the required Fabric API / Java version.
- Publish one toolkit jar per Minecraft version. For 26.2, also offer one Prism
  ZIP and one Modrinth pack, labeled **Full Pack - Prism** and **Full Pack - Modrinth**.
- Give the jar the GitHub display label `Skyblock-Toolkit-26.2.jar` (or
  `Skyblock-Toolkit-1.21.11.jar`). Preserve its underlying versioned asset name:
  existing installed updaters use that filename to select the right game version.
  Player instructions should use direct download links, not ask readers to decode filenames.
- Do not attach duplicate aliases, manifests, install-text files, checksum-text
  files or separate source archives. Keep technical verification in Actions and
  the repository. Link matching source in a short footer when needed.
- GitHub automatically adds its own two source-code links; those are not manual
  release attachments and cannot be removed through release asset management.
- Back up removed assets and metadata outside the repository before cleanup.
  Verify the remaining public downloads and their checksums afterward.

Build and regression guidance stays in `DEVELOPMENT.md`. Release descriptions
should explain what to download and how to play, without build logs, commit IDs,
developer commands or long lists of test results.
