package com.tuapp.notasmd.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "habit_entries",
    foreignKeys = [ForeignKey(
        entity        = Habit::class,
        parentColumns = ["id"],
        childColumns  = ["habitId"],
        onDelete      = ForeignKey.CASCADE
    )],
    indices = [Index("habitId"), Index(value = ["habitId", "date"], unique = true)]
)
data class HabitEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val habitId: Long,
    val date: String   // "yyyy-MM-dd"
)
