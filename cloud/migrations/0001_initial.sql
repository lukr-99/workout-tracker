-- The owner's Worker data (docs/claude-connector.md). The phone owns the training data; the shared_*
-- tables hold a copy only while a share is fresh, and are emptied when it runs out.

CREATE TABLE devices (
  id TEXT PRIMARY KEY,
  name TEXT NOT NULL,
  platform TEXT NOT NULL CHECK (platform IN ('android', 'connector')),
  token_hash TEXT NOT NULL UNIQUE,
  scopes TEXT NOT NULL DEFAULT '[]',
  push_token TEXT,
  created_at TEXT NOT NULL,
  last_seen_at TEXT,
  revoked_at TEXT
);

CREATE TABLE connector_codes (
  code_hash TEXT PRIMARY KEY,
  expires_at TEXT NOT NULL
);

CREATE TABLE share_requests (
  id TEXT PRIMARY KEY,
  device_id TEXT NOT NULL,
  created_at TEXT NOT NULL,
  expires_at TEXT NOT NULL,
  status TEXT NOT NULL CHECK (status IN ('pending', 'shared', 'declined', 'expired')),
  answered_at TEXT
);

CREATE INDEX share_requests_pending ON share_requests (status, expires_at);

CREATE TABLE share_state (
  id INTEGER PRIMARY KEY CHECK (id = 1),
  request_id TEXT,
  shared_at TEXT NOT NULL,
  keep_until TEXT NOT NULL,
  time_zone TEXT NOT NULL,
  units TEXT NOT NULL,
  app_version TEXT,
  format_version TEXT NOT NULL
);

CREATE TABLE shared_exercises (
  id TEXT PRIMARY KEY,
  name TEXT NOT NULL,
  category TEXT NOT NULL,
  primary_body_part TEXT NOT NULL,
  secondary_body_parts TEXT NOT NULL,
  equipment TEXT NOT NULL,
  notes TEXT NOT NULL,
  instructions TEXT NOT NULL,
  source TEXT NOT NULL,
  default_rest_seconds INTEGER,
  track_per_location INTEGER NOT NULL,
  archived INTEGER NOT NULL
);

CREATE TABLE shared_locations (
  id TEXT PRIMARY KEY,
  name TEXT NOT NULL,
  kind TEXT NOT NULL,
  archived INTEGER NOT NULL
);

CREATE TABLE shared_templates (
  id TEXT PRIMARY KEY,
  name TEXT NOT NULL,
  notes TEXT NOT NULL
);

CREATE TABLE shared_template_exercises (
  id TEXT PRIMARY KEY,
  template_id TEXT NOT NULL,
  exercise_id TEXT NOT NULL,
  exercise_name TEXT NOT NULL,
  category TEXT NOT NULL,
  body_part TEXT NOT NULL,
  sort_order INTEGER NOT NULL,
  notes TEXT NOT NULL,
  target_sets INTEGER,
  reps_min INTEGER,
  reps_max INTEGER,
  rest_seconds INTEGER,
  superset_group INTEGER
);

CREATE TABLE shared_sessions (
  id TEXT PRIMARY KEY,
  template_id TEXT,
  name TEXT NOT NULL,
  status TEXT NOT NULL,
  started_at TEXT NOT NULL,
  ended_at TEXT,
  local_day TEXT NOT NULL,
  duration_seconds INTEGER NOT NULL,
  notes TEXT NOT NULL,
  location_id TEXT,
  perceived_effort INTEGER,
  bodyweight_kg REAL
);

CREATE INDEX shared_sessions_day ON shared_sessions (status, local_day);

CREATE TABLE shared_entries (
  id TEXT PRIMARY KEY,
  session_id TEXT NOT NULL,
  exercise_id TEXT NOT NULL,
  exercise_name TEXT NOT NULL,
  category TEXT NOT NULL,
  body_part TEXT NOT NULL,
  sort_order INTEGER NOT NULL,
  superset_group INTEGER,
  notes TEXT NOT NULL
);

CREATE INDEX shared_entries_session ON shared_entries (session_id);
CREATE INDEX shared_entries_exercise ON shared_entries (exercise_id);

-- is_warmup is 1 for a warm-up set (the flag, the Warm-up tag, or the old set type); tags are the
-- set tag names joined with commas.
CREATE TABLE shared_sets (
  id TEXT PRIMARY KEY,
  entry_id TEXT NOT NULL,
  set_number INTEGER NOT NULL,
  reps INTEGER NOT NULL,
  weight_kg REAL NOT NULL,
  rir REAL,
  rpe REAL,
  duration_seconds INTEGER,
  is_warmup INTEGER NOT NULL,
  is_pr INTEGER NOT NULL,
  tags TEXT NOT NULL,
  notes TEXT NOT NULL
);

CREATE INDEX shared_sets_entry ON shared_sets (entry_id);

CREATE TABLE shared_cardio (
  entry_id TEXT PRIMARY KEY,
  duration_seconds INTEGER NOT NULL,
  distance_km REAL,
  calories REAL,
  notes TEXT NOT NULL
);

CREATE TABLE shared_runs (
  id TEXT PRIMARY KEY,
  started_at TEXT NOT NULL,
  local_day TEXT NOT NULL,
  duration_seconds INTEGER NOT NULL,
  moving_seconds INTEGER NOT NULL,
  distance_m REAL NOT NULL,
  avg_pace_sec_per_km REAL NOT NULL,
  elevation_gain_m REAL NOT NULL,
  calories REAL,
  avg_hr INTEGER,
  route_id TEXT,
  notes TEXT NOT NULL
);

CREATE INDEX shared_runs_day ON shared_runs (local_day);

CREATE TABLE shared_splits (
  run_id TEXT NOT NULL,
  idx INTEGER NOT NULL,
  distance_m REAL NOT NULL,
  duration_seconds INTEGER NOT NULL,
  pace_sec_per_km REAL NOT NULL,
  elevation_gain_m REAL,
  is_full INTEGER NOT NULL,
  PRIMARY KEY (run_id, idx)
);

CREATE TABLE shared_routes (
  id TEXT PRIMARY KEY,
  name TEXT NOT NULL,
  distance_m REAL NOT NULL,
  elevation_gain_m REAL NOT NULL,
  notes TEXT NOT NULL
);

-- Claude's proposals, which wait in the app's inbox until the owner accepts or rejects them.
CREATE TABLE proposals (
  id TEXT PRIMARY KEY,
  kind TEXT NOT NULL CHECK (kind IN ('exercise', 'template')),
  status TEXT NOT NULL CHECK (status IN ('pending', 'accepted', 'rejected', 'withdrawn')),
  title TEXT NOT NULL,
  reason TEXT NOT NULL,
  payload TEXT NOT NULL,
  client_ref TEXT NOT NULL UNIQUE,
  device_id TEXT NOT NULL,
  created_at TEXT NOT NULL,
  decided_at TEXT
);

CREATE INDEX proposals_status ON proposals (status, created_at);
