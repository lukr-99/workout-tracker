package com.lukr99.workout.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "templates",
    indices = [Index("name")],
)
data class TemplateEntity(
    @PrimaryKey val id: String,
    val name: String,
    val notes: String,
)
