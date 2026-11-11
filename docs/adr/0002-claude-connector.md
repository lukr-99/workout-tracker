# 0002: Claude reads Ember through a connector, on request

Status: accepted (2026-11-11).

## Context

The owner wants Claude to analyse their training, suggest improvements and make templates. Ember
has no backend and no account: the Room database on the phone is the source of truth
(ARCHITECTURE.md). claude.ai and the Claude apps reach outside data only through a remote MCP
connector, which must be a public HTTPS server. The owner's other apps (Aletheia, Lumen) run theirs
in a Cloudflare Worker with OAuth 2.1 and a single-use code from the app.

## Decision

- A Cloudflare Worker in `cloud/` hosts the connector at `/mcp`, built like Aletheia's and Lumen's.
- **The phone stays the owner of the data.** The Worker gets it only when Claude asks: it sends a
  content-free FCM message, and Ember asks the owner (Allow or Deny), shares by itself, or never
  shares, by a setting. A share is the export bundle without photos, settings or GPS traces. The
  Worker deletes it after 30 minutes.
- **Claude cannot change Ember's data.** It proposes templates and exercises; they wait in Settings,
  Claude until the owner accepts them, and Ember writes them through `WorkoutRepository`.
- The connector code is Android-only (`data/claude/`, `ui/android/`). The web edition has no
  Claude section.

## Considered

- **The Worker as the source of truth**, with Ember as a cache and outbox (Aletheia's model). Claude
  could change data directly, but Ember would need sync and conflict handling, and the data would
  live off the phone all the time. Too large a change to an offline-first app for this gain.
- **An always-on mirror** that Ember pushes after every change. Simple, but the training data would
  sit on a server all the time, and every change would cost a network call.
- **A local MCP server on the PC** reading a backup file. Works only in Claude Code or Claude
  Desktop on that PC, not in claude.ai or on the phone.

## Consequences

- Ember now has an optional network peer that receives training data, only when the owner allows
  it. The privacy policy says so, and the data contract lists the Claude link store.
- The share format is a contract with the Worker (`ClaudeShare.FORMAT`, a shared fixture tested on
  both sides). The Worker must read a new format before the app sends it.
- Push depends on Google Play services and a Firebase project. Without them Ember still answers
  when it is opened.
- The first read in a chat can take up to 25 seconds, or need the owner to tap Allow and ask again.
