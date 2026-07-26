package com.lukr99.workout.data.sync

import com.lukr99.workout.data.ExternalExerciseMergeSummary
import com.lukr99.workout.data.WorkoutRepository
import com.lukr99.workout.domain.Exercise
import com.lukr99.workout.domain.ExerciseCategory
import com.lukr99.workout.domain.ExerciseSource
import com.lukr99.workout.domain.newId
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response

fun interface ExternalExerciseMerger {
    suspend fun merge(exercises: List<Exercise>): ExternalExerciseMergeSummary
}

fun interface ExternalExerciseImageBackfiller {
    suspend fun backfill(exercises: List<Exercise>): Int
}

fun interface WgerPageSource {
    suspend fun fetchPage(url: String): WgerPage
}

/** Cancellable OkHttp transport kept separate so paging/mapping can be tested without a network. */
class WgerApiClient(
    private val client: OkHttpClient = OkHttpClient(),
    private val json: Json = Json { ignoreUnknownKeys = true },
) : WgerPageSource {
    override suspend fun fetchPage(url: String): WgerPage = suspendCancellableCoroutine { result ->
        val call = client.newCall(Request.Builder().url(url).get().build())
        result.invokeOnCancellation { call.cancel() }
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, exception: IOException) {
                if (result.isActive) result.resumeWithException(exception)
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    response.use {
                        if (!it.isSuccessful) {
                            throw IOException("wger request failed with HTTP ${it.code}.")
                        }
                        val body = it.body?.string()
                            ?: throw IOException("wger returned an empty response body.")
                        if (result.isActive) {
                            result.resume(json.decodeFromString(WgerPage.serializer(), body))
                        }
                    }
                } catch (failure: Throwable) {
                    if (result.isActive) result.resumeWithException(failure)
                }
            }
        })
    }
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
                val exercise = remote.toExercise(options.language)
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

data class WgerSyncOptions(
    val language: Int = 2,
    val limit: Int = 2_000,
    val pageSize: Int = 50,
    val offset: Int = 0,
    val maxPages: Int = 40,
) {
    internal fun validate() {
        require(language > 0) { "language must be positive." }
        require(limit > 0) { "limit must be positive." }
        require(pageSize in 1..100) { "pageSize must be between 1 and 100." }
        require(offset >= 0) { "offset must not be negative." }
        require(maxPages > 0) { "maxPages must be positive." }
    }
}

data class WgerSyncSummary(
    val fetched: Int,
    val mapped: Int,
    val added: Int,
    val updated: Int,
    val skipped: Int,
    val pages: Int,
    val warnings: List<String>,
    val imagesBackfilled: Int = 0,
) {
    val changed: Int get() = added + updated
}

@Serializable
data class WgerPage(
    val count: Int = 0,
    val next: String? = null,
    val results: List<WgerExerciseDto> = emptyList(),
)

@Serializable
data class WgerExerciseDto(
    val id: Int? = null,
    val uuid: String? = null,
    val name: String? = null,
    val category: WgerNamedDto? = null,
    val muscles: List<WgerNamedDto> = emptyList(),
    @SerialName("muscles_secondary")
    val secondaryMuscles: List<WgerNamedDto> = emptyList(),
    val equipment: List<WgerNamedDto> = emptyList(),
    val images: List<WgerImageDto> = emptyList(),
    val license: WgerLicenseDto? = null,
    @SerialName("license_author")
    val licenseAuthor: String? = null,
    val translations: List<WgerTranslationDto> = emptyList(),
) {
    internal fun toExercise(preferredLanguage: Int): Exercise? {
        val externalId = uuid?.trim()?.takeIf(String::isNotBlank)
            ?: id?.toString()
            ?: return null
        val translation = translations.firstOrNull { it.language == preferredLanguage }
            ?: translations.firstOrNull { !it.name.isNullOrBlank() }
        val displayName = translation?.name?.trim()?.takeIf(String::isNotBlank)
            ?: name?.trim()?.takeIf(String::isNotBlank)
            ?: return null
        val categoryName = category?.displayName().orEmpty()
        val isCardio = categoryName.contains("cardio", ignoreCase = true)
        val primary = muscles.firstOrNull()?.displayName()
            ?.takeIf(String::isNotBlank)
            ?: if (isCardio) "Cardio" else categoryName.ifBlank { "Full Body" }
        val description = translation?.descriptionSource
            ?.takeIf(String::isNotBlank)
            ?: translation?.description.orEmpty()
        val imageUrl = images.firstOrNull(WgerImageDto::isMain)?.image
            ?: images.firstOrNull()?.image
        val imageAttribution = imageUrl?.let {
            listOf("wger", license?.shortName, licenseAuthor)
                .filterNotNull()
                .map(String::trim)
                .filter(String::isNotBlank)
                .joinToString(" · ")
        }

        return Exercise(
            id = newId(),
            name = displayName,
            category = if (isCardio) ExerciseCategory.Cardio else ExerciseCategory.Strength,
            primaryBodyPart = primary,
            secondaryBodyParts = secondaryMuscles.map(WgerNamedDto::displayName)
                .filter(String::isNotBlank)
                .distinctBy(String::lowercase),
            equipment = equipment.joinToString(", ", transform = WgerNamedDto::displayName),
            notes = description.toPlainText(),
            source = ExerciseSource.Synced,
            externalSourceId = "wger:$externalId",
            imageUrl = imageUrl,
            imageAttribution = imageAttribution,
        )
    }
}

@Serializable
data class WgerImageDto(
    val image: String? = null,
    @SerialName("is_main")
    val isMain: Boolean = false,
)

@Serializable
data class WgerLicenseDto(
    @SerialName("short_name")
    val shortName: String = "",
    val url: String = "",
)

@Serializable
data class WgerNamedDto(
    val id: Int? = null,
    val name: String = "",
    @SerialName("name_en")
    val englishName: String? = null,
) {
    internal fun displayName(): String = englishName?.trim()
        ?.takeIf(String::isNotBlank)
        ?: name.trim()
}

@Serializable
data class WgerTranslationDto(
    val language: Int? = null,
    val name: String? = null,
    val description: String? = null,
    @SerialName("description_source")
    val descriptionSource: String? = null,
)

private fun String.toPlainText(): String = replace(Regex("<[^>]+>"), " ")
    .replace("&nbsp;", " ")
    .replace("&amp;", "&")
    .replace("&lt;", "<")
    .replace("&gt;", ">")
    .replace(Regex("\\s+"), " ")
    .trim()
