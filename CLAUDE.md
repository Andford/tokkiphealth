# Tokkip Healthy PC — guide for AI coding agents

Andy's (LoperStack) PC cleaner and optimizer, 100% ours: Tauri 2 (Rust in
`src-tauri/`, Preact + TypeScript in `src/`) with Tokkip PC's look and Tokkip
himself showing how the PC feels. Part of the Tokkip family (Andford/tokkip).
Spanish first (Mexican, de tú), English too.

This repo (Andford/tokkiphealth) is the new app. The older one (Electron,
based on Kudu) stays in Andford/tokkiphealthypc (branch `tokkip`) and is the
one installed until this one does everything it did, and the reference for
each tool; the plan and its progress are in `docs/PLAN.md`.

## Where things are
- `src-tauri/src/tools/` — one module per tool (the cleaner is `cleaner.rs`), each with its tests.
- `src-tauri/src/fsx.rs` — what every tool touching files shares: a file's identity (checked again right before deleting), the protected folders, the Recycle Bin, "show in folder".
- Scans keep what they found in `lib.rs` (`Found`), by id; the page sends ids back, never paths to delete.
- Kudu (MIT) is the reference for what each tool does and its Windows care: read its version (old app: Andford/tokkiphealthypc, branch `tokkip`) before writing one, rewrite it in Rust, and credit it in the file and THIRD-PARTY-NOTICES.md.
- `src-tauri/src/platform.rs` — what asks Windows (processes, disk, recycle bin, admin). Other systems get quiet stubs, so the logic tests run on Linux.
- `rules/*.json` — our cleaning rules: paths with `%VARIABLES%` and `*` folders, file patterns, minimum age, admin, the program that keeps them open.
- `src-tauri/src/health.rs` — the health score and mood; `notice.rs` writes `%APPDATA%\Tokkip\shared\healthy-pc.json` for Tokkip PC (moods and kinds are a contract with it).
- `src/pages/` — one page per section; `src/core/api.ts` — the commands (and the example PC shown outside the app).
- `src/i18n/es.json` — the Spanish for each English key.
- Copied from Tokkip PC by `npm run sync-tokkip` (don't edit here, change them there): `src/tokkip/*`, `src/core/{theme.ts,tokens.css,layout.ts,fonts.ts}`, `public/sounds/`.
- `npm run icons` regenerates `src/ui/icons.ts` from Material Symbols.

## Android (`android/`)
Tokkip Health for Android (`mx.loperstack.tokkiphealth`): a fork of SD Maid SE
(GPL-3.0, d4rken-org/sdmaid-se): **its engine, our face**. The tools' code stays
SD Maid's; what people see is Tokkip's: name, colours (Material 3 from Tokkip's
teal, Material You by default), Google Sans, Tokkip himself
(`app-common-tokkip`, copied from Andford/tokkip `android/shared` — change him
there) and Spanish. SD Maid's mascot, icons, animations, store texts and
translations are excluded by its license: never bring them in; write our own
Spanish in `res/values-es`. Kotlin packages stay `mx.loperstack.tokkiphealth.*`.
Shizuku (and accessibility) for access on a Pixel without root. No MOTD, no
billing; the update check only reads `android-v*` releases of this repo.
Credit SD Maid SE in Acknowledgements and README. Build: `cd android && ./gradlew :app:assembleRelease`
(CI: `.github/workflows/android.yml`). Fixes from SD Maid are brought over by hand.

## Rules
- Keep the identifier `mx.loperstack.tokkiphealthypc`.
- Look first, then touch: every tool reviews, lets you choose, then applies. Never delete while scanning. Files in use are skipped and counted, never forced. Links are never followed.
- Undoable, or say so first (the recycle bin, the shredder).
- Nothing leaves the PC: no cloud, no telemetry. Network only for what the user asks.
- Use the theme tokens (`--c-*`), never fixed colours.
- Commits as Andy (`soportemacrobit001@gmail.com`), Conventional Commits.

## Check
```
npm ci
npx tsc --noEmit && npm test     # page
npx vite build && (cd src-tauri && cargo test)   # Rust (needs dist/)
npm run tauri dev                # the app
```
