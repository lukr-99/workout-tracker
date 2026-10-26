package com.lukr99.workout.data.document

import com.lukr99.workout.settings.SeenVersionStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** The [SeenVersionStore] over a [DocumentDatabase]: outside the backup, like Android's. */
class DocumentSeenVersionStore(private val database: DocumentDatabase) : SeenVersionStore {

    override val seen: Flow<Int?> = database.document.map { it.seenVersion }.distinctUntilChanged()

    override suspend fun markSeen(versionCode: Int) = database.update { it.copy(seenVersion = versionCode) }
}
