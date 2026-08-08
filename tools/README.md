# Tools — phone / ADB omni-tooling

Helper scripts for working with the tethered Android phone and the app, adapted from the
ring-set project's `build-and-install.ps1` / `pull-data.ps1` patterns.

They auto-locate `adb.exe` (via `ANDROID_HOME`/`ANDROID_SDK_ROOT`, `PATH`, or common SDK
install dirs) and give actionable errors when the phone is missing or unauthorized. All scripts
are PowerShell; run them from the repo root.

## `phone.ps1` — the one entry point

```powershell
.\tools\phone.ps1 devices            # who's connected + auth state
.\tools\phone.ps1 wait               # block until you accept the USB-debugging prompt
.\tools\phone.ps1 find lyfta         # locate an installed package by name
.\tools\phone.ps1 apps fit           # list user-installed apps, filtered
.\tools\phone.ps1 screenshot         # grab a PNG to .\import\
.\tools\phone.ps1 tap 540 1200       # tap a device pixel
.\tools\phone.ps1 swipe 540 1500 540 600 250   # scroll
.\tools\phone.ps1 key BACK           # send a key event (BACK/HOME/ENTER/…)
.\tools\phone.ps1 seed-route Loop 1200 40      # seed a synthetic saved route (debug)
.\tools\phone.ps1 dump               # print Run Mode DB counts (RUNMODE_DUMP runs=.. routes=..)
.\tools\phone.ps1 logcat Workout     # filtered logcat
.\tools\phone.ps1 install app.apk    # adb install -r
.\tools\phone.ps1 launch com.lyfta   # launch an app
.\tools\phone.ps1 pull-lyfta         # pull Lyfta's CSV export
```

## Run Mode verification harness (chainable / agent-testable)

The manual "install → seed → click through → screenshot → confirm" loop is codified so it runs
headlessly and repeatably, and **exits non-zero on failure** (CI/agent chainable):

```powershell
.\tools\run-mode-check.ps1 -Build -Clean -Seed   # build, wipe, seed, screenshot, assert exact counts
.\tools\run-mode-check.ps1                        # capture + assert on the current state
.\tools\run-sim.ps1 -Meters 5000 -Seconds 1500    # replay a synthetic run into the live controller
.\tools\run-sim.ps1 -UseRoute -Meters 1100        # …linked to the newest saved route
```

- **Headless seeding** — a debug `RunSimReceiver` (runs) + `RunDevReceiver` (`DEV_SEED_ROUTE`,
  `DEV_CLEAR`, `DEV_DUMP`) let an agent create/reset/inspect Run Mode state from adb, no UI taps.
- **Non-visual assertions** — `DEV_DUMP` logs `RUNMODE_DUMP runs=.. routes=.. linkedRuns=..` for
  parseable checks; `-Clean -Seed` asserts *exact* counts (3 runs / 1 route / 1 linked).
- **Evidence** — `run-mode-check.ps1` also drops screenshots of Home / Runs / Progress→Running into a
  timestamped `import/run-mode-check-*/` folder (git-ignored).
- These receivers ship in the **debug** source set only — never in release builds.

## Task-specific scripts

| Script | What it does | Ready |
|--------|--------------|-------|
| `common.ps1` | Shared adb-locate + device-assert helpers (dot-sourced). | now |
| `pull-lyfta.ps1` | Scan the phone for Lyfta's exported CSV(s) and copy them to `import/lyfta/`. | now |
| `pull-data.ps1` | Pull OUR app's exported JSON/CSV via `adb run-as` (debug build). | when app exists |
| `build-and-install.ps1` | `gradlew assembleDebug` + `adb install -r` (+ `-Launch`). | now |
| `run-sim.ps1` | Replay a synthetic GPS run into the live controller (debug); `-UseRoute` links it. | now |
| `run-mode-check.ps1` | End-to-end Run Mode verify: build/seed/screenshot/assert, non-zero on fail. | now |

## First-time setup on the phone

1. Enable **Developer options** (tap Build number 7×) and **USB debugging**.
2. Plug in over USB; on the phone accept **"Allow USB debugging?"** (tick *Always allow*).
3. Verify: `\.tools\phone.ps1 devices` should show one device in state `device`.

## Notes

- `pull-data.ps1` only works on **debug** builds (that's what makes `run-as` legal without root).
  A release build must export via the in-app share sheet instead.
- Lyfta is release-signed, so we can't read its private DB — use its in-app CSV export, then
  `pull-lyfta.ps1`. See [../docs/rework/05-lyfta-import.md](../docs/rework/05-lyfta-import.md).
- Pulled data lands under `import/`, which is git-ignored — it may contain personal history.
