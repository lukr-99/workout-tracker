package com.lukr99.workout.data.sync

import com.lukr99.workout.data.ExternalExerciseMergeSummary
import com.lukr99.workout.data.WorkoutRepository
import com.lukr99.workout.domain.Exercise
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import okhttp3.HttpUrl.Companion.toHttpUrl

fun interface ExternalExerciseMerger {
    suspend fun merge(exercises: List<Exercise>): ExternalExerciseMergeSummary
}

fun interface ExternalExerciseImageBackfiller {
    suspend fun backfill(exercises: List<Exercise>): Int
}

fun interface WgerPageSource {
    suspend fun fetchPage(url: String): WgerPage
}

/**
 * Imports the public wger exercise catalog using bounded, cancellable paging and a protected
 * repository merge. The page source and merger are replaceable for offline tests or other clients.
 */
class WgerSyncService(
    private val merger: ExternalExerciseMerger,
    private val imageBackfiller: ExternalExerciseImageBackfiller =
        ExternalExerciseImageBackfiller { 0 },
    private val pageSource: WgerPageSource = WgerApiClient(),
    private val baseUrl: String = DefaultBaseUrl,
) {
    constructor(
        repository: WorkoutRepository,
        pageSource: WgerPageSource = WgerApiClient(),
        baseUrl: String = DefaultBaseUrl,
    ) : this(
        merger = ExternalExerciseMerger { repository.mergeExternalExercisesDetailed(it) },
        imageBackfiller =
            ExternalExerciseImageBackfiller { repository.backfillMissingExerciseImages(it) },
        pageSource = pageSource,
        baseUrl = baseUrl,
    )

    suspend fun sync(options: WgerSyncOptions = WgerSyncOptions()): WgerSyncSummary {
        options.validate()
        val origin = baseUrl.toHttpUrl()
        var next: String? = origin.newBuilder()
            .addPathSegments("api/v2/exerciseinfo/")
            .addQueryParameter("language", options.language.toString())
            .addQueryParameter("limit", minOf(options.pageSize, options.limit).toString())
            .addQueryParameter("offset", options.offset.toString())
            .build()
            .toString()
        var pages = 0
        var fetched = 0
        var mappingSkipped = 0
        val mapped = mutableListOf<Exercise>()
        val warnings = mutableListOf<String>()

        while (next != null && fetched < options.limit && pages < options.maxPages) {
            currentCoroutineContext().ensureActive()
            val page = pageSource.fetchPage(next)
            pages++
            for (remote in page.results.take(options.limit - fetched)) {
                fetched++
                val exercise = remote.toExercise(options.language, baseUrl)
                if (exercise == null) {
                    mappingSkipped++
                    warnings += "Skipped wger exercise ${remote.uuid ?: remote.id ?: "unknown"}: no usable id/name."
                } else {
                    mapped += exercise
                }
            }
            next = page.next?.takeIf { candidate ->
                runCatching {
                    val url = candidate.toHttpUrl()
                    url.scheme == origin.scheme && url.host == origin.host && url.port == origin.port
                }.getOrDefault(false)
            }
            if (page.next != null && next == null) {
                warnings += "Stopped paging because wger returned a next URL outside the configured origin."
            }
        }
        if (next != null && pages >= options.maxPages && fetched < options.limit) {
            warnings += "Stopped after the configured maximum of ${options.maxPages} pages."
        }

        val merge = merger.merge(mapped)
        val imagesBackfilled = imageBackfiller.backfill(mapped)
        return WgerSyncSummary(
            fetched = fetched,
            mapped = mapped.size,
            added = merge.added,
            updated = merge.updated,
            skipped = mappingSkipped + merge.skipped,
            pages = pages,
            warnings = warnings,
            imagesBackfilled = imagesBackfilled,
        )
    }

    companion object {
        const val DefaultBaseUrl = "https://wger.de/"
    }
}

internal fun String.toPlainText(): String = replace(Regex("<[^>]+>"), " ")
    .replace("&nbsp;", " ")
    .replace("&amp;", "&")
    .replace("&lt;", "<")
    .replace("&gt;", ">")
    .replace(Regex("\\s+"), " ")
    .trim()
