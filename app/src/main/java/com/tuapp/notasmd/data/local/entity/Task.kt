package com.tuapp.notasmd.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tasks",
    foreignKeys = [ForeignKey(
        entity        = TaskCategory::class,
        parentColumns = ["id"],
        childColumns  = ["categoryId"],
        onDelete      = ForeignKey.SET_NULL
    )],
    indices = [Index("categoryId")]
)
data class Task(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val categoryId: Long? = null,
    val title: String,
    val isCompleted: Boolean = false,
    val isRecurring: Boolean = false,
    val recurUnit: String = "DAYS",   // HOURS, DAYS, WEEKS, MONTHS, YEARS
    val recurAmount: Int = 1,
    val nextDueAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
