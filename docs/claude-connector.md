# The Claude connector

Claude reads the owner's training and proposes templates through a remote MCP server at `/mcp` in a
Cloudflare Worker (`cloud/`), built the way CodePrint's reference projects (Aletheia, Lumen) build
theirs. One difference: **the phone stays the owner of the data.** The Worker holds a copy only for a
short while after the phone shares it, and the phone shares only when Claude asks and the owner
allows it ([ADR 0002](adr/0002-claude-connector.md)).

```text
 Claude ──MCP──▶ Worker /mcp ──"no fresh copy"──▶ share request ──FCM {"t":"share","r":id}──▶ Ember
    ▲                 │                                                                      │
    │                 │◀────────── POST /v1/share-requests/{id}/share (ClaudeShare) ─────────┘
    └── tool answer ◀─┘   (the tool call waits up to 25 s; the copy stays 30 min, then is deleted)
```

## Sharing on request

1. A read tool (`get_overview`, `list_workouts`, ...) finds no fresh copy. The Worker opens a share
   request (10 minutes to answer), sends Ember a content-free FCM message, and waits up to
   `SHARE_WAIT_SECONDS` (25) for the answer. Calls that come together share one request.
2. Ember answers by **Settings, Claude, When Claude asks for your data**:
   - *Ask me* (default): a notification "Claude asks for your training data" with **Allow** and
     **Deny**. Settings, Claude lists open requests with the same buttons, for a phone that shows no
     notifications.
   - *Always share*: Ember uploads straight away, without a notification.
   - *Never*: Ember declines.
3. Allow uploads a `ClaudeShare` (`data/claude/ClaudeShare.kt`): the backup's `ExportBundle` without
   photos, settings or runs, plus the runs without their GPS trace but with their kilometre splits,
   the phone's time zone and the unit setting. The Worker keeps it for `SHARE_KEEP_MINUTES` (30), so
   the rest of the chat needs no new request, then an alarm deletes every row. **Share now** and
   **Stop sharing** in Settings, Claude do the same by hand.
4. If the owner has not answered when the wait ends, the tool answers `waiting_for_phone`, and
   Claude asks the owner to tap Allow and calls again. Deny answers `declined`, and Claude does not
   ask again unless the owner says so.

Without push (a build without `app/google-services.json`, no Play services), Ember answers open
requests whenever it comes to the front (`MainActivity.onResume`), and the tool tells Claude to have
Ember opened.

## The inbox

Claude never changes the owner's data. `propose_template` and `propose_exercise` put a proposal in
the Worker's inbox and push `{"t":"inbox"}`; Ember shows "Claude proposed something", and Settings,
Claude lists each proposal with Claude's reason and **Accept** or **Reject**. Accepting writes the
template (with each exercise's plan: target sets, rep range, rest, superset group) or the exercise
(as the owner's own, with its steps) through `WorkoutRepository` first and then tells the Worker,
so it is safe to repeat. A template may bring a new exercise; accepting the template creates it too.
A template with `replacesTemplateId` is a new version of an existing template and replaces it.

## Scopes and tools

| Scope | Tool | What it does |
|---|---|---|
| read | `get_overview` | Today in the owner's zone and their units; training (counts, streak, volume, locations, most done exercises, last workouts, the live one) and running (totals, weekly km, bests, last runs) |
| read | `list_workouts {from?, to?, q?, locationId?, limit?}` | Finished workouts with location, working and hard sets, reps, volume, cardio, effort |
| read | `get_workout {id}` | One workout: every set with RIR or RPE, set tags, warm-up and PR marks, supersets |
| read | `list_exercises {q?, bodyPart?, category?, includeArchived?}` | The catalog with the owner's note, steps, own rest, tracked per gym, how often and when last |
| read | `get_exercise_progress {exerciseId, from?, to?, limit?}` | Per workout: location, working sets, top weight, best set, estimated 1RM; records and trend |
| read | `get_training_volume {from?, to?, period?, by?}` | Hard sets, working sets, reps and volume per week or month by body part or exercise |
| read | `list_templates` | Templates with each exercise's plan |
| read | `get_running_overview {weeks?}` | Weekly km, streak, bests (longest, fastest pace over 5 and 10 km, fastest km split) |
| read | `list_runs {from?, to?, limit?}`, `get_run {id}` | Runs with pace, elevation, heart rate, route; one run's splits |
| read | `list_proposals {status?, limit?}` | Claude's proposals and what the owner did with them (needs no share) |
| plan | `propose_template {name, notes?, exercises[{exerciseId, targetSets?, repsMin?, repsMax?, restSeconds?, supersetGroup?, notes?}], replacesTemplateId?, reason, clientRef}` | A template for the inbox |
| plan | `propose_exercise {name, category, primaryBodyPart, secondaryBodyParts?, equipment?, instructions?, defaultRestSeconds?, reason, clientRef}` | A new exercise; answers the id `propose_template` can use |
| plan | `withdraw_proposal {id}` | Takes back a proposal that still waits |

Warm-ups never count toward volume or records. A hard set is CONTEXT.md's: a working set within 4
reps of failure (RIR 4 or less, RPE 6 or more) or with no effort logged. Prompts:
`training_review {focus?}` (read) and `build_template {goal?}` (read, plan).

Errors come back as tool errors whose text starts with the code: `waiting_for_phone`, `declined`,
`no_phone`, `not_found`, `invalid` (every problem listed), `duplicate`, `conflict`, `rate_limited`.

## The share is a contract

`ClaudeShare` is read by `cloud/src/workouts/SharedBundle.ts`. `ClaudeShareContractTest` writes a
fixed share and compares it with `cloud/test/fixtures/ember-share.json`, which the Worker's
`contract.test.ts` reads, so a change on either side fails a test. A change to the bundle that the
Worker reads bumps `ClaudeShare.FORMAT`; deploy the Worker that reads the new format before the app.

## The app's API

All under `/v1`, with the phone's bearer token (`wt1.<secret>`) except setup.

| Route | Purpose |
|---|---|
| `POST /v1/devices {setupSecret, name}` | Registers the phone; a new phone replaces the old one |
| `PUT /v1/push-token {pushToken}` | The FCM token |
| `GET /v1/share-requests` | Requests not answered yet, for catching up |
| `POST /v1/share-requests/{id}/share` (a `ClaudeShare`) / `.../decline` | Answers one |
| `POST /v1/share`, `GET /v1/share`, `DELETE /v1/share` | Share unasked, see, or delete the copy |
| `GET /v1/proposals?status=pending`, `POST /v1/proposals/{id}/decision {decision}` | The inbox |
| `POST /v1/connector-codes` | A single-use code (10 minutes) for Claude's sign-in page |
| `GET /v1/devices`, `DELETE /v1/devices/{id}` | Devices; revoking the connector stops Claude at once |

## Setting it up

The Worker runs at `https://workout-tracker.lukrejci9.workers.dev`, with its OAuth KV namespace and
`SETUP_SECRET` set (the secret is in `cloud/.dev.vars` on the owner's PC, never committed).

- **Push** (optional): add Android apps `com.lukr99.workout` and `com.lukr99.workout.debug` to a
  Firebase project (Lumen's is fine), save `google-services.json` as `app/google-services.json`
  (git-ignored), and give the Worker a service-account key:
  `npx wrangler secret put FCM_SERVICE_ACCOUNT` from `cloud/`.
- **Ember**: Settings, Claude, Connect: the Worker address and the setup secret. Allow
  notifications, and pick the share mode.
- **Claude**: in Ember, Connect Claude, Get code. On claude.ai, Settings, Connectors, Add custom
  connector, paste the address (`https://<worker>/mcp`), and on the sign-in page type the code and
  tick Read and Propose. Connectors added on the web also work in the Claude apps.

## Pitfalls

- claude.ai caches a connector's tool list. After a deploy that adds or renames tools, remove the
  connector in claude.ai and add it again (seen first in GoalMaker).
- A high-priority FCM message gives Ember about 20 seconds; *Always share* must upload in that time
  (the handler stops at 15). A long history on a slow connection may answer `waiting_for_phone`
  once; the next call reads the copy.
- The Worker's local dev server (`npm run dev`, port 8799) serves OAuth only on `localhost`; an
  emulator reaching it at `http://10.0.2.2:8799` gets the app API alone, which Ember accepts.
