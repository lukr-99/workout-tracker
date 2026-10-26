package com.lukr99.workout.data.document

/** What the UI tells the owner about storage: the save [state] and any data set aside. */
data class StorageStatus(
    val state: StorageState = StorageState.Opening,
    val setAside: SetAside? = null,
)
