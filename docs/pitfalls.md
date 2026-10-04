# Pitfalls

Things that went wrong in this repository and took longer to find than to fix. Search here, and in
CodePrint's `docs/pitfalls/`, for the error text before debugging something surprising. The format
is in CodePrint's `docs/pitfalls/README.md`. Newest first.

## A PowerShell 5.1 rewrite turns `—` into `â€”` in Kotlin sources

- Symptom: after a scripted edit, a source file shows `â€”`, `Ã—` or `â†’` where it had `—`, `×`
  or `→`, and `git diff` shows changes on lines the script never meant to touch.
- Cause: Windows PowerShell 5.1 `Get-Content` reads a UTF-8 file without a byte-order mark as the
  ANSI code page (Windows-1252). Writing that text back as UTF-8 encodes the misread bytes again.
- Fix: read with `Get-Content -Encoding UTF8` or `[IO.File]::ReadAllText($p, [Text.UTF8Encoding]::new($false))`,
  or edit with a tool that keeps the encoding. To repair a damaged file, re-encode its text to
  Windows-1252 bytes and decode those as UTF-8, then check `git diff` for leftovers.
- Closed off by: not yet.
- Seen: richer notes work, 2026-10-04, while moving `EntryCard` out of `LiveWorkoutScreen.kt`.
