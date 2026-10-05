package com.lukr99.workout.data.sync

import com.lukr99.workout.domain.Exercise
import com.lukr99.workout.domain.ExerciseCategory
import com.lukr99.workout.domain.ExerciseSource
import com.lukr99.workout.domain.newId
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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
    internal fun toExercise(preferredLanguage: Int, baseUrl: String = ""): Exercise? {
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
        val rawImageUrl = images.firstOrNull(WgerImageDto::isMain)?.image
            ?: images.firstOrNull()?.image
        // wger's exerciseinfo endpoint usually returns absolute image URLs, but self-hosted/older
        // instances hand back a site-relative path ("/media/…") that Coil cannot load — make it absolute.
        val imageUrl = rawImageUrl?.trim()?.takeIf(String::isNotBlank)?.let { url ->
            if (url.startsWith("/") && baseUrl.isNotBlank()) baseUrl.trimEnd('/') + url else url
        }
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
            // The description is how-to text, not a personal note (v8 split the two).
            instructions = description.toPlainText(),
            source = ExerciseSource.Synced,
            externalSourceId = "wger:$externalId",
            imageUrl = imageUrl,
            imageAttribution = imageAttribution,
        )
    }
}
