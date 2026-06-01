package com.tuapp.notasmd.data.repository

import com.tuapp.notasmd.data.local.dao.TagDao
import com.tuapp.notasmd.data.local.entity.Note
import com.tuapp.notasmd.data.local.entity.NoteTag
import com.tuapp.notasmd.data.local.entity.Tag
import kotlinx.coroutines.flow.Flow

class TagRepository(private val tagDao: TagDao) {

    fun getAllTags(): Flow<List<String>> = tagDao.getAllTagsSortedByCount()

    fun getNotesByTag(tagName: String): Flow<List<Note>> = tagDao.getNotesByTag(tagName)

    // Reindexar los tags de una nota: borra los anteriores e inserta los nuevos
    suspend fun syncTagsForNote(noteId: Long, tagNames: List<String>) {
        tagDao.clearTagsForNote(noteId)
        tagNames.forEach { name ->
            val existingId = tagDao.getTagIdByName(name)
            val tagId = existingId ?: run {
                val inserted = tagDao.insertTag(Tag(name = name))
                // insertTag devuelve -1 si hubo conflicto (race condition); buscar de nuevo
                if (inserted == -1L) tagDao.getTagIdByName(name) ?: return@forEach
                else inserted
            }
            tagDao.insertNoteTag(NoteTag(noteId = noteId, tagId = tagId))
        }
    }
}
