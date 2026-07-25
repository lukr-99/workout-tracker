package com.lukr99.workout.domain

/**
 * Pure Kotlin unit helpers. **No Android imports** — `domain/` stays portable and unit-testable in
 * isolation (this is what could later be lifted into a shared/desktop module). See 01-architecture.md.
 *
 * Phase 0 seeds this with the kg⇄lb conversion so the domain layer has a real, tested function;
 * the fuller analytics/estimates land in Phase 1.
 */
object Units {
    const val LB_PER_KG: Double = 2.2046226218

    /** Kilograms → pounds. */
    fun kgToLb(kg: Double): Double = kg * LB_PER_KG

    /** Pounds → kilograms. */
    fun lbToKg(lb: Double): Double = lb / LB_PER_KG
}
