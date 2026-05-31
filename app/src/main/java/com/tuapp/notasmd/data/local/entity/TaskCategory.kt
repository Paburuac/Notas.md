package com.tuapp.notasmd.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "task_categories")
data class TaskCategory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val color: String,
    val order: Int = 0
)
