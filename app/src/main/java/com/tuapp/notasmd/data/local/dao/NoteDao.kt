package com.tuapp.notasmd.data.local.dao

import androidx.room.*
import com.tuapp.notasmd.data.local.entity.Note
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {

    @Query("SELECT * FROM notes WHERE sectionId = :sectionId ORDER BY updatedAt DESC")
    fun getNotesBySection(sectionId: Long): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getNoteById(id: Long): Note?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: Note): Long

    @Update
    suspend fun updateNote(note: Note)

    @Delete
    suspend fun deleteNote(note: Note)

    @Query("UPDATE notes SET sectionId = :newSectionId, updatedAt = :updatedAt WHERE id = :noteId")
    suspend fun updateNoteSectionId(noteId: Long, newSectionId: Long, updatedAt: Long)

    @Query("SELECT * FROM notes WHERE id IN (:ids)")
    suspend fun getNotesByIds(ids: List<Long>): List<Note>

    @Query("SELECT * FROM notes WHERE title LIKE '%' || :query || '%' OR contentMarkdown LIKE '%' || :query || '%' ORDER BY updatedAt DESC")
    fun searchNotes(query: String): Flow<List<Note>>

    @Query("SELECT * FROM notes ORDER BY updatedAt DESC")
    fun getAllNotes(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE title LIKE '%' || :query || '%' ORDER BY updatedAt DESC LIMIT 10")
    suspend fun searchNotesByTitle(query: String): List<Note>

    @Query("SELECT * FROM notes WHERE sectionId = :sectionId ORDER BY updatedAt DESC")
    suspend fun getNotesBySectionOnce(sectionId: Long): List<Note>
}