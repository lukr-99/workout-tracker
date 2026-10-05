package com.lukr99.workout.update

import java.io.File
import java.io.IOException

/**
 * The updater core, free of UI: release source -> version policy -> artifact selector -> verified
 * download -> installer launcher (CodePrint "Auto-update seam"). The UI only shows its state and
 * asks for consent. The manual path stays open: every release is also a plain GitHub download.
 */
class UpdateService(
    val currentVersion: String,
    private val source: ReleaseSource,
    private val http: UpdateHttp,
    private val downloader: VerifiedDownloader,
    private val signatureCheck: SignatureCheck,
    private val launcher: InstallerLauncher,
) {
    suspend fun check(): UpdateCheck {
        if (VersionPolicy.isDevelopmentVersion(currentVersion)) return UpdateCheck.DevelopmentBuild
        val release = source.latest()
        if (release.draft || release.prerelease || !VersionPolicy.isNewer(release.version, currentVersion)) {
            return UpdateCheck.UpToDate
        }
        return ArtifactSelector.select(release).fold(
            onSuccess = { (apk, checksum) ->
                UpdateCheck.Available(UpdateOffer(release.version, release.body.orEmpty(), apk, checksum))
            },
            onFailure = { UpdateCheck.NotVerifiable(release.version, it.message.orEmpty()) },
        )
    }

    /** Downloads and verifies [offer]: checksum, then signer. Returns the APK ready to install. */
    suspend fun download(offer: UpdateOffer, onProgress: (Float) -> Unit = {}): File {
        val expected = Sha256File.parse(http.getText(offer.checksum.url, MAX_CHECKSUM_BYTES), offer.apk.name)
        val apk = downloader.download(offer.apk, expected, onProgress)
        if (!signatureCheck.sameSigner(apk)) {
            apk.delete()
            throw IOException("The update is not signed by the same key as this app, so it was not installed.")
        }
        return apk
    }

    fun install(apk: File) = launcher.launch(apk)

    private companion object {
        const val MAX_CHECKSUM_BYTES = 4 * 1024
    }
}
