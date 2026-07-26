package com.lukr99.workout.data.health

import android.app.Activity
import android.os.Bundle

/**
 * Manifest endpoint required by Health Connect. Phase 4 UI will replace this non-rendering stub
 * with the product privacy/rationale destination.
 */
class HealthConnectPermissionRationaleActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        finish()
    }
}
