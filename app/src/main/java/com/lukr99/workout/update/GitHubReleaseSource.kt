package com.lukr99.workout.update

import kotlinx.serialization.json.Json

/**
 * The public GitHub Releases API, without authentication. This works because the repository is
 * public; that is the chosen distribution model (CodePrint "Private repository releases").
 */
class GitHubReleaseSource(
    private val owner: String,
    private val repo: String,
    private val http: UpdateHttp,
) : ReleaseSource {
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun latest(): PublishedRelease {
        val text = http.getText("https://api.github.com/repos/$owner/$repo/releases/latest", MAX_RELEASE_JSON)
        return json.decodeFromString(PublishedRelease.serializer(), text)
    }

    private companion object {
        /** A release description with assets is a few kilobytes; refuse anything absurd. */
        const val MAX_RELEASE_JSON = 512 * 1024
    }
}
