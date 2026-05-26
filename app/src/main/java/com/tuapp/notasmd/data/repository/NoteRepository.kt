package com.tuapp.notasmd.data.repository

import com.tuapp.notasmd.data.local.dao.NoteDao
import com.tuapp.notasmd.data.local.entity.Note
import kotlinx.coroutines.flow.Flow

class NoteRepository(private val noteDao: NoteDao) {

    fun getNotesBySection(sectionId: Long): Flow<List<Note>> =
        noteDao.getNotesBySection(sectionId)

    suspend fun getNoteById(id: Long): Note? =
        noteDao.getNoteById(id)

    suspend fun insertNote(note: Note): Long =
        noteDao.insertNote(note)

    suspend fun updateNote(note: Note) =
        noteDao.updateNote(note)

    suspend fun deleteNote(note: Note) =
        noteDao.deleteNote(note)
}