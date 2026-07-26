# Final Phase — Cutover: remove the old C# app, promote the rework to `main` + `release/2.0`

**Decision:** the app is **personal-use only — not shipping to Google Play.** So the Play-submission
work in [phase-6-release-checklist.md](phase-6-release-checklist.md) is **not pursued**; this cutover
is the real final phase. The native Kotlin rework (Phases 0–4 + bug-fix + release hardening) is
complete on `feature/app-rework`, verified on-device, and now holds the real imported Lyfta history.

## Goal

Make the Kotlin app the canonical app: delete the frozen `.NET MAUI` source from the working line,
promote `feature/app-rework` to **`main`**, and cut a **`release/2.0`** branch + `v2.0.0` tag. The
MAUI POC stays preserved forever on `release/1.0` + tag `v1.0.0` (and in history), so removing it from
`main` loses nothing.

## Steps (all doable directly by the agent on request)

1. **Remove the C# / MAUI artifacts** on `feature/app-rework`:
   ```
   git rm -r WorkoutTracker.App WorkoutTracker.Core WorkoutTracker.Tests WorkoutTracker.sln
   ```
   Also delete/repoint anything MAUI-specific left at the root.
2. **Rewrite the root `README.md`** — it currently describes the MAUI app. Replace with the native
   Android app (Kotlin/Compose/Room, build via `tools/build-and-install.ps1`, `docs/rework/` as the
   design/architecture home). Fold the still-relevant `docs/decisions.md` / `docs/roadmap.md` content
   into the rework docs or mark them historical (they describe the MAUI POC).
3. **Commit** on `feature/app-rework` (`chore: remove MAUI POC; native Kotlin app is canonical`).
4. **Promote to `main`.** `main` is the MAUI-freeze commit and an ancestor of `feature/app-rework`,
   so this fast-forwards cleanly:
   ```
   git checkout main && git merge --ff-only feature/app-rework
   ```
   (Use `--no-ff` instead if you'd prefer a visible merge commit.)
5. **Tag + release branch:**
   ```
   git tag -a v2.0.0 -m "Native Android rewrite (Kotlin/Compose)"
   git branch release/2.0
   ```
6. **Push:** `git push origin main release/2.0 --tags`.
7. **Branch cleanup (confirm first):** optionally delete `feature/app-rework` once `main` carries
   everything (local + remote). Keep `release/1.0` and both tags.

## Decisions (locked by the user)

- **Merge into `main` is `--ff-only`** — linear history (`main` is a strict ancestor of
  `feature/app-rework`).
- **Keep `feature/app-rework`** after promotion — **but note it for future deletion** once `main` has
  been used for a while (it will be fully contained in `main`).
- The frozen MAUI code stays only on `release/1.0` + `v1.0.0` (that's the archive; no copy on `main`).

## Sequencing

Run the cutover **after** the exercise-images feature
([handoff-codex-exercise-images.md](handoff-codex-exercise-images.md)) is merged and verified — that's
the last planned feature before cutover.

## Not in scope (personal use)

Signing for the Play Store, store listing, privacy-policy hosting, Play Console forms — all dropped.
The debug build installed via `tools/build-and-install.ps1 -Launch` is the personal distribution.
(The R8 release build + signing config from Phase 5 remain available if you ever want a self-signed
release APK for your own devices — see [release-signing.md](release-signing.md) — but nothing
requires it.)
