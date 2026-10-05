package com.lukr99.workout.update

/**
 * Picks the expected files from a release by exact name, never "the first APK": `Ember-<version>.apk`
 * and its checksum `Ember-<version>.apk.sha256`. `tools/publish-release.ps1` attaches both.
 */
object ArtifactSelector {

    fun apkName(version: String): String = "Ember-$version.apk"

    fun checksumName(version: String): String = "${apkName(version)}.sha256"

    /** The APK and checksum assets, or null with the reason this release cannot be verified. */
    fun select(release: PublishedRelease): Result<Pair<ReleaseAsset, ReleaseAsset>> {
        val apk = release.assets.firstOrNull { it.name == apkName(release.version) }
            ?: return Result.failure(
                IllegalStateException("Release ${release.tag} has no ${apkName(release.version)}."),
            )
        val checksum = release.assets.firstOrNull { it.name == checksumName(release.version) }
            ?: return Result.failure(
                IllegalStateException("Release ${release.tag} has no checksum, so it cannot be verified."),
            )
        return Result.success(apk to checksum)
    }
}
