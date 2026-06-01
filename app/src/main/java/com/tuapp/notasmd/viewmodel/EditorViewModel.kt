package com.tuapp.notasmd.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.tuapp.notasmd.data.local.entity.Note
import com.tuapp.notasmd.data.repository.NoteRepository
import com.tuapp.notasmd.data.repository.TagRepository
import com.tuapp.notasmd.viewmodel.extractTags
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EditorUiState(
    val noteId:          Long        = -1L,
    val title:           String      = "",
    val content:         String      = "",
    val createdAt:       Long        = System.currentTimeMillis(),
    val updatedAt:       Long        = System.currentTimeMillis(),
    val isSaved:         Boolean     = true,
    val isPreviewMode:   Boolean     = false,
    val linkSuggestions: List<Note>  = emptyList(),
    val showLinkPicker:  Boolean     = false
)

class EditorViewModel(
    private val repository:    NoteRepository,
    private val tagRepository: TagRepository,
    private val sectionId:     Long,
    private val initialNoteId: Long
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    init {
        if (initialNoteId != -1L) {
            viewModelScope.launch {
                repository.getNoteById(initialNoteId)?.let { note ->
                    _uiState.update {
                        it.copy(
                            noteId    = note.id,
                            title     = note.title,
                            content   = note.contentMarkdown,
                            createdAt = note.createdAt,
                            updatedAt = note.updatedAt,
                            isSaved   = true
                        )
                    }
                }
            }
        }
    }

    fun onTitleChange(newTitle: String) =
        _uiState.update { it.copy(title = newTitle, isSaved = false) }

    fun onContentChange(newContent: String) =
        _uiState.update { it.copy(content = newContent, isSaved = false) }

    fun togglePreview() =
        _uiState.update { it.copy(isPreviewMode = !it.isPreviewMode) }

    fun onLinkQueryChange(query: String) {
        viewModelScope.launch {
            val results = if (query.isEmpty()) emptyList()
                          else repository.searchNotesByTitle(query)
            _uiState.update { it.copy(linkSuggestions = results, showLinkPicker = true) }
        }
    }

    fun dismissLinkPicker() =
        _uiState.update { it.copy(showLinkPicker = false, linkSuggestions = emptyList()) }

    fun saveNote() {
        val state = _uiState.value
        // No guardar si no hay cambios o si la nota está completamente vacía
        if (state.isSaved) return
        if (state.title.isBlank() && state.content.isBlank()) return

        viewModelScope.launch {
            val now  = System.currentTimeMillis()
            val tags = extractTags(state.content)
            if (state.noteId == -1L) {
                val newId = repository.insertNote(
                    Note(
                        sectionId       = sectionId,
                        title           = state.title.ifBlank { "Sin título" },
                        contentMarkdown = state.content,
                        createdAt       = now,
                        updatedAt       = now
                    )
                )
                tagRepository.syncTagsForNote(newId, tags)
                _uiState.update { it.copy(noteId = newId, updatedAt = now, isSaved = true) }
            } else {
                repository.updateNote(
                    Note(
                        id              = state.noteId,
                        sectionId       = sectionId,
                        title           = state.title.ifBlank { "Sin título" },
                        contentMarkdown = state.content,
                        createdAt       = state.createdAt,
                        updatedAt       = now
                    )
                )
                tagRepository.syncTagsForNote(state.noteId, tags)
                _uiState.update { it.copy(updatedAt = now, isSaved = true) }
            }
        }
    }

    companion object {
        fun factory(
            repository:    NoteRepository,
            tagRepository: TagRepository,
            sectionId:     Long,
            noteId:        Long
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer { EditorViewModel(repository, tagRepository, sectionId, noteId) }
        }
    }
}