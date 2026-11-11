import { LocalDay } from "./LocalDay";
import { round, weeklyStreak } from "./WorkoutReader";

export interface RunFilter {
  from: string | null;
  to: string | null;
  limit: number;
}

type RunRow = {
  id: string;
  started_at: string;
  local_day: string;
  duration_seconds: number;
  moving_seconds: number;
  distance_m: number;
  avg_pace_sec_per_km: number;
  elevation_gain_m: number;
  calories: number | null;
  avg_hr: number | null;
  route: string | null;
  notes: string;
};

const RUN_SELECT = `SELECT r.*, rt.name AS route FROM shared_runs r LEFT JOIN shared_routes rt ON rt.id = r.route_id`;

/**
 * Reads the shared copy's runs for Claude: summaries, one run with its splits, and the running
 * picture over recent weeks. Distances are kilometres, paces minutes per kilometre as `m:ss`.
 */
export class RunReader {
  constructor(private readonly sql: SqlStorage) {}

  runs(filter: RunFilter) {
    const rows = this.sql
      .exec<RunRow>(`${RUN_SELECT} WHERE r.local_day >= ? AND r.local_day <= ? ORDER BY r.started_at DESC LIMIT ?`, filter.from ?? "0000-01-01", filter.to ?? "9999-12-31", filter.limit + 1)
      .toArray();
    return { runs: rows.slice(0, filter.limit).map(runView), more: rows.length > filter.limit };
  }

  run(id: string) {
    const row = this.sql.exec<RunRow>(`${RUN_SELECT} WHERE r.id = ?`, id).toArray()[0];
    if (!row) return null;
    const splits = this.sql
      .exec<{ idx: number; distance_m: number; duration_seconds: number; pace_sec_per_km: number; elevation_gain_m: number | null; is_full: number }>(
        "SELECT idx, distance_m, duration_seconds, pace_sec_per_km, elevation_gain_m, is_full FROM shared_splits WHERE run_id = ? ORDER BY idx",
        id,
      )
      .toArray()
      .map((split) => ({
        km: split.idx,
        distanceKm: round(split.distance_m / 1000),
        pace: pace(split.pace_sec_per_km),
        seconds: split.duration_seconds,
        elevationGainM: split.elevation_gain_m === null ? null : round(split.elevation_gain_m),
        full: split.is_full === 1,
      }));
    return { ...runView(row), splits };
  }

  /** Totals, weekly kilometres, the streak, and the best efforts the shared runs show. */
  overview(today: string, weeks: number) {
    const all = this.sql.exec<RunRow>(`${RUN_SELECT} ORDER BY r.started_at DESC`).toArray();
    const from = LocalDay.addDays(LocalDay.weekStart(today), -7 * (weeks - 1));
    const weekly = new Map<string, { runs: number; km: number }>();
    for (let week = from; week <= today; week = LocalDay.addDays(week, 7)) weekly.set(week, { runs: 0, km: 0 });
    for (const run of all) {
      const week = weekly.get(LocalDay.weekStart(run.local_day));
      if (week) {
        week.runs++;
        week.km += run.distance_m / 1000;
      }
    }
    const since = (days: number) => all.filter((run) => run.local_day > LocalDay.addDays(today, -days));
    const longest = all.reduce<RunRow | null>((top, run) => (top === null || run.distance_m > top.distance_m ? run : top), null);
    const fastestOver = (meters: number) =>
      all.filter((run) => run.distance_m >= meters && run.avg_pace_sec_per_km > 0).reduce<RunRow | null>((top, run) => (top === null || run.avg_pace_sec_per_km < top.avg_pace_sec_per_km ? run : top), null);
    const fastestSplit = this.sql
      .exec<{ run_id: string; local_day: string; pace_sec_per_km: number }>(
        "SELECT sp.run_id, r.local_day, sp.pace_sec_per_km FROM shared_splits sp JOIN shared_runs r ON r.id = sp.run_id WHERE sp.is_full = 1 AND sp.pace_sec_per_km > 0 ORDER BY sp.pace_sec_per_km LIMIT 1",
      )
      .toArray()[0];
    const best = (run: RunRow | null) => (run ? { runId: run.id, day: run.local_day, distanceKm: round(run.distance_m / 1000), avgPace: pace(run.avg_pace_sec_per_km) } : null);
    return {
      totalRuns: all.length,
      totalKm: round(all.reduce((sum, run) => sum + run.distance_m, 0) / 1000),
      lastRunDay: all[0]?.local_day ?? null,
      runsLast30Days: since(30).length,
      kmLast30Days: round(since(30).reduce((sum, run) => sum + run.distance_m, 0) / 1000),
      currentWeeklyStreak: weeklyStreak(all.map((run) => run.local_day), today),
      weekly: [...weekly.entries()].map(([week, totals]) => ({ week: `week of ${week}`, runs: totals.runs, km: round(totals.km) })),
      bests: {
        longestRun: best(longest),
        fastestPaceOver5Km: best(fastestOver(5000)),
        fastestPaceOver10Km: best(fastestOver(10000)),
        fastestKmSplit: fastestSplit ? { runId: fastestSplit.run_id, day: fastestSplit.local_day, pace: pace(fastestSplit.pace_sec_per_km) } : null,
      },
      recentRuns: all.slice(0, 5).map(runView),
    };
  }
}

function runView(row: RunRow) {
  return {
    id: row.id,
    day: row.local_day,
    startedAt: row.started_at,
    title: row.notes.split("\n")[0]?.trim() || "Run",
    distanceKm: round(row.distance_m / 1000),
    durationMinutes: round(row.duration_seconds / 60),
    movingMinutes: round(row.moving_seconds / 60),
    avgPace: pace(row.avg_pace_sec_per_km),
    elevationGainM: Math.round(row.elevation_gain_m),
    avgHr: row.avg_hr,
    calories: row.calories,
    route: row.route,
    notes: row.notes,
  };
}

/** Seconds per kilometre as `m:ss /km`, or null when there is no pace. */
export function pace(secondsPerKm: number): string | null {
  if (!(secondsPerKm > 0) || !Number.isFinite(secondsPerKm)) return null;
  const total = Math.round(secondsPerKm);
  return `${Math.floor(total / 60)}:${String(total % 60).padStart(2, "0")} /km`;
}
