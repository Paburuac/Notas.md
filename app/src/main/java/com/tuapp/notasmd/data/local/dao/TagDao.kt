package com.tuapp.notasmd.data.local.dao

import androidx.room.*
import com.tuapp.notasmd.data.local.entity.Note
import com.tuapp.notasmd.data.local.entity.NoteTag
import com.tuapp.notasmd.data.local.entity.Tag
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {

    // Inserta el tag si no existe; si ya existe, lo ignora y devuelve -1
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTag(tag: Tag): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertNoteTag(noteTag: NoteTag)

    @Query("SELECT id FROM tags WHERE name = :name")
    suspend fun getTagIdByName(name: String): Long?

    // Borra todas las relaciones de una nota para re-indexar desde cero
    @Query("DELETE FROM note_tags WHERE noteId = :noteId")
    suspend fun clearTagsForNote(noteId: Long)

    // Todos los tags únicos ordenados por cantidad de notas que los usan
    @Query("""
        SELECT t.name FROM tags t
        INNER JOIN note_tags nt ON nt.tagId = t.id
        GROUP BY t.id
        ORDER BY COUNT(nt.noteId) DESC
    """)
    fun getAllTagsSortedByCount(): Flow<List<String>>

    // Notas que tienen un tag específico
    @Query("""
        SELECT n.* FROM notes n
        INNER JOIN note_tags nt ON nt.noteId = n.id
        INNER JOIN tags t       ON t.id = nt.tagId
        WHERE t.name = :tagName
        ORDER BY n.updatedAt DESC
    """)
    fun getNotesByTag(tagName: String): Flow<List<Note>>
}
