package com.tuapp.notasmd.data.local.dao

import androidx.room.*
import com.tuapp.notasmd.data.local.entity.Section
import kotlinx.coroutines.flow.Flow

@Dao
interface SectionDao {

    @Query("SELECT * FROM sections WHERE notebookId = :notebookId AND parentSectionId IS NULL ORDER BY updatedAt DESC")
    fun getSectionsByNotebook(notebookId: Long): Flow<List<Section>>

    @Query("SELECT * FROM sections WHERE parentSectionId = :parentSectionId ORDER BY updatedAt DESC")
    fun getSubSections(parentSectionId: Long): Flow<List<Section>>

    @Query("SELECT * FROM sections WHERE id = :id")
    suspend fun getSectionById(id: Long): Section?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSection(section: Section): Long

    @Update
    suspend fun updateSection(section: Section)

    @Delete
    suspend fun deleteSection(section: Section)
}