package com.lukr99.workout.data

/**
 * The single API over persistence and files — the *only* layer that touches Room and the
 * filesystem (see 01-architecture.md). ViewModels call the repository; screens call ViewModels.
 *
 * Phase 0 stub: no Room database, DAO, or import/export yet — those arrive in Phase 1 (data core)
 * and Phase 3 (import/export). This exists so the dependency shape is real from day one.
 */
class WorkoutRepository {
    // Phase 1: inject WorkoutDb/WorkoutDao; expose Flow-returning queries + upserts.
    // Phase 3: own ExportBundle JSON + CSV export and Lyfta/MAUI importers.
}
