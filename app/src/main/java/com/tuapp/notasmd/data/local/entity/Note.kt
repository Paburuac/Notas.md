package com.tuapp.notasmd.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "notes",
    foreignKeys = [ForeignKey(
        entity          = Section::class,
        parentColumns   = ["id"],
        childColumns    = ["sectionId"],
        onDelete        = ForeignKey.CASCADE   // si borras una sección, se borran sus notas
    )],
    indices = [Index("sectionId")]
)
data class Note(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sectionId: Long,                       // FK → Section.id
    val title: String,
    val contentMarkdown: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)