package com.lukr99.workout.data.claude

/** What became of a share request the phone received. */
enum class ShareAnswer {
    Shared,
    Declined,

    /** The share mode is Ask: the owner decides with Allow or Deny. */
    NeedsOwner,

    /** The request was answered elsewhere or lapsed before this answer. */
    Expired,
}
