package com.lukr99.workout.data.document

/** A photo could not be written, for example because the device storage is full. */
class PhotoStorageException(message: String) : Exception(message)
