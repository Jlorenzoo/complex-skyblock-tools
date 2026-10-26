# Player-facing releases

Keep GitHub release pages short and easy to use without programming knowledge.

## Automated Releases via GitHub Actions
Releases are automated via [`.github/workflows/release.yml`](../.github/workflows/release.yml):
- Pushing a version tag (e.g. `git tag v1.2.0 && git push origin v1.2.0`) or clicking **Publish release** on GitHub triggers the build and packaging workflow.
- GitHub Actions builds both **1.21.11** and **26.2**, runs all test suites, and generates:
  - `Full-Pack-Modrinth-<version>.mrpack` (Modrinth App 1-click import)
  - `Full-Pack-Prism-<version>.zip` (Prism Launcher 1-click import)
  - `skyblock-toolkit-1.1.0+mc<version>.jar` (standalone JAR for existing mods folders)
  - `SHA256SUMS.txt`
- The workflow automatically attaches all 6 package assets directly to the GitHub Release.

## Player Presentation
- The release description presents a clear download table by launcher and game version.
- Preserves the underlying `+mc<version>.jar` filename so the in-game auto-updater in `UpdateManager.java` automatically detects and downloads updates for players' installed game version.
- Build and regression guidance stays in `DEVELOPMENT.md`. Release descriptions explain what to download and how to play.

