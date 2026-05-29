package com.tuapp.notasmd.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sections",
    foreignKeys = [ForeignKey(
        entity          = Notebook::class,
        parentColumns   = ["id"],
        childColumns    = ["notebookId"],
        onDelete        = ForeignKey.CASCADE
    )],
    indices = [Index("notebookId"), Index("parentSectionId")]
)
data class Section(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val notebookId: Long,
    val parentSectionId: Long? = null,         // null = sección raíz; no null = sub-sección
    val name: String,
    val color: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)