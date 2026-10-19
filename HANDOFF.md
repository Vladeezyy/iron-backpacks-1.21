# HANDOFF — Iron Backpacks port (state on 2026-10-19)

Read this first when continuing on another machine or in a new session.

## Owner and rules
- Owner: Vladeezyy (GitHub `Vladeezyy`), answers in Russian. Commits as `Vladeezyy <vladeezym@gmail.com>`, no AI
  attribution anywhere. Global rules: `~/.claude/CLAUDE.md`; knowledge base `Vladeezyy/modding-kb` (read `README.md`,
  `porting/mc-versions/26.*.md`, `testing/gametests.md` — Iron Backpacks notes are there).
- License: GPL-3.0 (same as the original; GPL stays GPL).

## Repo and branches
| Branch | MC | State |
|---|---|---|
| `main` | 1.21.1 | release `v1.0.3+1.21.1` |
| `mc/26.1.2` | 26.1.2 | release `v1.0.3+26.1.2` |
| `mc/26.2` | 26.2 | release `v1.0.3+26.2` |
| `mc/26.3` | 26.3 | release `v1.0.3+26.3` (Latest) |

Chain: `main` → `mc/26.1.2` (port commit) → `mc/26.2` (port + bannerFile commit) → `mc/26.3` (port); each `mc/*` also has
the cherry-picked "PerfScene: no inactivity frame rate limit" commit. On the owner's Windows PC the worktrees were
`D:\mods\iron-backpacks-1.21` (main) and `D:\mods\iron-backpacks-<mc>`: `git worktree add ../iron-backpacks-<mc> mc/<mc>`.

## Build and test
- `main`: JDK 21. `mc/*`: JDK 25 as `JAVA_HOME` (Loom 1.18 needs it for Gradle itself), Gradle 9.8.1.
- `./gradlew :neoforge:runGameTestServer`, `:fabric:runGametest`, `:fabric:prodGametest` (20 tests each, all green on
  all 4 versions), `:neoforge:runScene` / `:fabric:runScene` (17 screenshots in `<loader>/run/screenshots`; on 26.x the
  scene creates a new flat world itself), `:neoforge:runPerf` (PerfScene).
- 26.x resources: `python3 -I tools/convert_26.py common/src/main/resources` after `tools/gen_resources.py` (main).

## What was done (2026-10-19)
- Original's open issues read (42); fixed in `main` and all branches: dupe with an open backpack (#234, #268), lost or
  reverted contents with sorters (#255, #221), open key sound (#243), open key ignored the Latch. README has the table.
- Ports to 26.1.2 / 26.2 / 26.3: see PORTLOG.md "Minecraft 26.1.2, 26.2 and 26.3".
- GitHub releases 1.0.3 for all four versions (NeoForge + Fabric jars).

## Open / next
- **Published 2026-10-19:** 1.0.3 for 1.21.1, 26.1.2, 26.2, 26.3 (NeoForge + Fabric, 8 jars) on Modrinth (`iron-backpacks`)
  and CurseForge (project 1751003, approved) with `~/Desktop/modrinth-publish/ib_release.py <version> [--go]` (Mac;
  files and changelogs from the GitHub releases). The CurseForge description has the "Original mod" block, the 26.x
  versions and the 1.0.3 fixes.
- Compatibility runs (Curios/Accessories etc.) were checked on 1.21.1 only.
- Known: default open key I is also F3+I (debug "copy recreate command") on 26.x; no real conflict.
- 400 backpacks on the ground render at ~15 % of shulker boxes' FPS on 26.3: vanilla item/generated geometry (the mod's
  code is ~2 % of the render thread in a flight recording).
