package com.lukr99.workout.data.document

import com.lukr99.workout.data.transfer.PhotoArchive

/**
 * The web edition's [PhotoArchive] until exercise photos come to the web (milestone 3): it keeps
 * no photos. A backup's photo is accepted but not stored, and [stage] returns a blank key, which
 * the exercise saves as "no photo", so a restore of an Android backup with photos still succeeds
 * and the exercise shows the normal placeholder. Backups made on the web carry no photos.
 */
object DroppedPhotoArchive : PhotoArchive {
    override fun exportBytes(path: String): ByteArray? = null
    override fun exists(path: String): Boolean = false
    override fun stage(exerciseId: String, bytes: ByteArray): String = ""
    override fun delete(path: String) = Unit
    override fun deleteAllExcept(keep: Set<String>) = Unit
}
