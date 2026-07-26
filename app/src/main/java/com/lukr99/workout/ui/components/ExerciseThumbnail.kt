package com.lukr99.workout.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.lukr99.workout.data.images.ExerciseImageResolver
import com.lukr99.workout.data.images.ExerciseImageSource
import com.lukr99.workout.data.images.ResolvedExerciseImage
import com.lukr99.workout.domain.Exercise
import java.io.File

val LocalExerciseImageResolver = compositionLocalOf<ExerciseImageResolver?> { null }

@Composable
fun resolvedExerciseImage(exercise: Exercise): ResolvedExerciseImage? {
    val resolver = LocalExerciseImageResolver.current
    return remember(exercise, resolver) {
        resolver?.resolve(exercise) ?: exercise.localImagePath
            ?.takeIf(String::isNotBlank)
            ?.let(::File)
            ?.takeIf(File::isFile)
            ?.let { ResolvedExerciseImage(it, ExerciseImageSource.UserPhoto) }
            ?: exercise.imageUrl?.takeIf(String::isNotBlank)?.let {
                ResolvedExerciseImage(
                    model = it,
                    source = ExerciseImageSource.Wger,
                    attribution = exercise.imageAttribution,
                )
            }
    }
}

/**
 * Offline-tolerant exercise art. Seeded/custom exercises, and failed remote requests, retain the
 * body-part monogram underneath the image. Coil supplies memory and disk caching for remote images.
 */
@Composable
fun ExerciseThumbnail(
    exercise: Exercise,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
) {
    val resolved = resolvedExerciseImage(exercise)
    val monogram = exercise.primaryBodyPart
        .trim()
        .take(2)
        .ifBlank { exercise.category.name.take(2) }
        .uppercase()

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            monogram,
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
        )
        if (resolved != null) {
            AsyncImage(
                model = resolved.model,
                contentDescription = "${exercise.name} illustration",
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
            )
        }
    }
}
