# Changelog

## v1.1.0 — Themes, persistence & new icon
- **5 full themes** (MINT, OCEAN, SUNSET, NIGHT, PAPER) — each recolors the whole app (ink/paper/mute + accent). The old purple VIOLET option was removed; no purple anywhere.
- **Settings persistence** across platforms: your edition, theme and first-run setup are remembered (Android SharedPreferences; desktop `~/.fllscorer` properties file; browser `localStorage` for web targets).
- **New launcher icon**: precision-token ring with rising score bars on a LEGO stud plate + leaf graze. All densities regenerated (square + round), plus `icon_preview.png` at the project root.
- Self-updater controls kept (upstream build), disabled for F-Droid style builds via `SELF_UPDATER_ENABLED`.
- Newest release APK: `FLL-Scorer-debug.apk` (installable) / `FLL-Scorer-release-unsigned.apk`.

## v1.0.0 — Baseline
- Both editions (Founders + Future) with 15 + 5(+Level Up) missions, real rulebook text.
- MISSIONS / ITEMS / SCORER / BOOK / GUIDE / BUILD tabs; search, photos, 2:30 timer, precision tokens, penalties, 3-match recorder with best/average.
