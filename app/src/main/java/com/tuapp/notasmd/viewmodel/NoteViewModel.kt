package com.tuapp.notasmd.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.tuapp.notasmd.data.local.entity.Note
import com.tuapp.notasmd.data.repository.NoteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NotesUiState(
    val notes: List<Note>           = emptyList(),
    val noteToDelete: Note?         = null
)

class NoteViewModel(
    private val repository: NoteRepository,
    private val sectionId: Long
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotesUiState())
    val uiState: StateFlow<NotesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getNotesBySection(sectionId).collect { notes ->
                _uiState.update { it.copy(notes = notes) }
            }
        }
    }

    fun createNote(title: String): Long {
        var newId = 0L
        viewModelScope.launch {
            newId = repository.insertNote(
                Note(sectionId = sectionId, title = title, contentMarkdown = "")
            )
        }
        return newId
    }

    fun deleteNote(note: Note) {
        viewModelScope.launch { repository.deleteNote(note) }
        hideDeleteDialog()
    }

    fun showDeleteDialog(note: Note) = _uiState.update { it.copy(noteToDelete = note) }
    fun hideDeleteDialog()           = _uiState.update { it.copy(noteToDelete = null) }

    companion object {
        fun factory(repository: NoteRepository, sectionId: Long): ViewModelProvider.Factory =
            viewModelFactory {
                initializer { NoteViewModel(repository, sectionId) }
            }
    }
}