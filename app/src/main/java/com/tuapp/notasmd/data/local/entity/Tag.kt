package com.tuapp.notasmd.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tags",
    indices   = [Index("name", unique = true)]
)
data class Tag(
    @PrimaryKey(autoGenerate = true)
    val id:   Long   = 0,
    val name: String  // lowercase, sin el '#'
)
