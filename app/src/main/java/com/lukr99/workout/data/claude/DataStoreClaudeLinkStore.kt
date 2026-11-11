package com.lukr99.workout.data.claude

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.claudeStore: DataStore<Preferences> by preferencesDataStore(name = "claude")

/**
 * [ClaudeLinkStore] in its own Preferences DataStore, in app-private storage like the rest of
 * Ember's data. The settings archive never reads it, so the token stays out of backups.
 */
class DataStoreClaudeLinkStore(context: Context) : ClaudeLinkStore {
    private val store = context.applicationContext.claudeStore

    override val link: Flow<ClaudeLink?> = store.data.map { prefs ->
        val url = prefs[SERVER_URL]
        val token = prefs[TOKEN]
        if (url.isNullOrBlank() || token.isNullOrBlank()) null else ClaudeLink(url, token)
    }

    override val shareMode: Flow<ShareMode> = store.data.map { prefs ->
        ShareMode.entries.firstOrNull { it.name == prefs[SHARE_MODE] } ?: ShareMode.Ask
    }

    override suspend fun setLink(link: ClaudeLink?) {
        store.edit { prefs ->
            if (link == null) {
                prefs.remove(SERVER_URL)
                prefs.remove(TOKEN)
            } else {
                prefs[SERVER_URL] = link.serverUrl
                prefs[TOKEN] = link.token
            }
        }
    }

    override suspend fun setShareMode(mode: ShareMode) {
        store.edit { it[SHARE_MODE] = mode.name }
    }

    private companion object {
        val SERVER_URL = stringPreferencesKey("server_url")
        val TOKEN = stringPreferencesKey("token")
        val SHARE_MODE = stringPreferencesKey("share_mode")
    }
}
