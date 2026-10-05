package com.lukr99.workout.data.transfer

/** How an import lands in the store. */
enum class RestoreMode {
    /** Add what is new and merge matches. Nothing already on the phone is removed. */
    Merge,

    /**
     * Delete everything first and put the backup in its place, exactly as it was saved, settings
     * included. Only an Ember backup can replace; a Lyfta CSV cannot.
     */
    Replace,
}
