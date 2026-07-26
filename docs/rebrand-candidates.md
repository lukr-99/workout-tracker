# Rebrand — app name candidates

The app now covers **strength + running** (v2.1), with a minimal-dark look, an **ember** accent, and a
**dumbbell logo we're keeping**. So the name should feel warm/energetic, pair with a dumbbell/ember/
iron mark, and not read as lifting-only. "Workout Tracker" is a placeholder, not a brand.

> Availability caveat: I can't verify Play Store / trademark uniqueness — **check each shortlisted
> name** on Play + a quick trademark search before committing. The Android **applicationId stays
> `com.lukr99.workout`**; only the display label + `app_name` string change.

## Shortlist (grouped)

**Ember / forge / iron — pairs with the current logo**
- **Forge** ⭐ — "forge yourself"; short, strong, brandable; works for lift *and* run; great with an
  ember/anvil/dumbbell mark.
- **Cinder** ⭐ — ember-adjacent but far less used than "Ember"; distinctive, sleek, dark.
- **Ember** — literally the accent; warm and clean, but common (many apps/products).
- **Anvil** — iron/forge energy, pairs with the dumbbell; solid and masculine.
- **Ignite** — energy/start; motivational (fairly common).

**Motion / tempo — leans into the new running side but still broad**
- **Tempo** ⭐ — pace + rhythm; modern and minimal; reads athletic without being run-only.
- **Cadence** — elegant run/ride term; a touch niche.
- **Momentum** — progress across any activity; motivational (a bit long).

**Effort / grit — activity-neutral**
- **Grit** ⭐ — effort itself; punchy, memorable, one syllable; covers everything.
- **Strive** — positive, broad, progress-oriented.
- **Hone** — "hone yourself"; quiet and minimal.

## CHOSEN: **Ember**

Decided (personal-use, so Play/trademark collision is irrelevant — optimized purely for meaning): an
**ember still glows and holds heat — alive, ready to reignite**, the stronger/positive metaphor for a
training app, and it matches the app's existing **ember accent color** for cohesion. **Cinder** was
the close runner-up (more distinctive, "a chunk that can still start burning") — trivial to switch to
if preferred. Wired in during Run-Mode **R0**: `app_name`/label/splash/Settings footer/README; the
`applicationId` (`com.lukr99.workout`) is unchanged so it upgrades in place.

## Recommendation (original)

Top pick **Forge** (on-brand with the ember/dumbbell mark, covers both activities, distinctive) —
with **Cinder**, **Tempo**, and **Grit** as strong alternates depending on the vibe you want (warm-
forge vs. sleek-athletic vs. punchy-effort). Ember-family (**Cinder**/**Ember**) leans hardest into
the existing logo color.

## When chosen

I can wire the pick in a small change: `app_name` in `strings.xml`, the launcher label, the splash
title, the in-app header/Settings footer ("Workout Tracker · 2.x"), and the README — no package/id
change, so installs upgrade in place. Optional: a wordmark treatment next to the dumbbell logo.
