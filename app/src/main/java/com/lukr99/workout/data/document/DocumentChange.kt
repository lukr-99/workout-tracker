package com.lukr99.workout.data.document

/**
 * One write, as a function from the document to the changed document. It must give the same result
 * for the same input (ids and times are chosen before the change is made), because after a
 * conflict with another tab it is applied again to the newer document.
 */
typealias DocumentChange = (WorkoutDocument) -> WorkoutDocument
