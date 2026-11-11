package com.lukr99.workout.data.claude

/** How this phone answers when Claude asks for the training data (docs/claude-connector.md). */
enum class ShareMode {
    /** A notification with Allow and Deny. */
    Ask,

    /** Share without asking. */
    Automatic,

    /** Decline every request. */
    Never,
}
