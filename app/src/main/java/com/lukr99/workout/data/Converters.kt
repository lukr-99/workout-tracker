package com.lukr99.workout.data

import androidx.room.TypeConverter
import com.lukr99.workout.domain.ExerciseCategory
import com.lukr99.workout.domain.ExerciseSource
import com.lukr99.workout.domain.SetType
import com.lukr99.workout.domain.WorkoutSessionStatus
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/**
 * Room type converters. Enums are stored as their **Int ordinal** (the MAUI wire value — do not
 * reorder enum members) and secondary body parts as a JSON string array, matching Records.cs.
 */
class Converters {
    // --- Enums <-> ordinal Int -----------------------------------------------------------------

    @TypeConverter fun categoryToInt(v: ExerciseCategory): Int = v.ordinal
    @TypeConverter fun intToCategory(v: Int): ExerciseCategory = ExerciseCategory.entries[v]

    @TypeConverter fun sourceToInt(v: ExerciseSource): Int = v.ordinal
    @TypeConverter fun intToSource(v: Int): ExerciseSource = ExerciseSource.entries[v]

    @TypeConverter fun statusToInt(v: WorkoutSessionStatus): Int = v.ordinal
    @TypeConverter fun intToStatus(v: Int): WorkoutSessionStatus = WorkoutSessionStatus.entries[v]

    @TypeConverter fun setTypeToInt(v: SetType): Int = v.ordinal
    @TypeConverter fun intToSetType(v: Int): SetType = SetType.entries[v]

    // --- List<String> <-> JSON -----------------------------------------------------------------

    @TypeConverter
    fun stringListToJson(list: List<String>): String =
        json.encodeToString(stringListSerializer, list)

    @TypeConverter
    fun jsonToStringList(value: String?): List<String> {
        if (value.isNullOrBlank()) return emptyList()
        return runCatching { json.decodeFromString(stringListSerializer, value) }.getOrDefault(emptyList())
    }

    private companion object {
        val json = Json { ignoreUnknownKeys = true }
        val stringListSerializer = ListSerializer(String.serializer())
    }
}
