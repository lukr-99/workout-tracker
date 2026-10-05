package com.lukr99.workout.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.orientationDataStore by preferencesDataStore(name = "orientation")

/**
 * What the app has already shown this person, kept apart from settings so a backup restore or a
 * settings reset never brings an old card back.
 */
class OrientationStore(private val context: Context) : SeenVersionStore {

    override val seen: Flow<Int?> = context.orientationDataStore.data.map { it[WhatsNewSeen] }

    override suspend fun markSeen(versionCode: Int) {
        context.orientationDataStore.edit { it[WhatsNewSeen] = versionCode }
    }

    private companion object {
        val WhatsNewSeen = intPreferencesKey("whats_new_seen")
    }
}
