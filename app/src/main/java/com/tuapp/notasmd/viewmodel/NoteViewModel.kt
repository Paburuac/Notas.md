package com.tuapp.notasmd.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.tuapp.notasmd.data.local.entity.Note
import com.tuapp.notasmd.data.local.entity.Notebook
import com.tuapp.notasmd.data.local.entity.Section
import com.tuapp.notasmd.data.repository.NoteRepository
import com.tuapp.notasmd.data.repository.NotebookRepository
import com.tuapp.notasmd.data.repository.SectionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NotesUiState(
    val notes: List<Note>                   = emptyList(),
    val subSections: List<Section>          = emptyList(),
    val noteToDelete: Note?                 = null,
    val showCreateSubSectionDialog: Boolean = false,
    val noteToMove: Note?                   = null,
    val moveNotebooks: List<Notebook>       = emptyList(),
    val moveSelectedNotebook: Notebook?     = null,
    val moveSections: List<Section>         = emptyList(),
    val isSelectionMode: Boolean            = false,
    val selectedNoteIds: Set<Long>          = emptySet()
)

class NoteViewModel(
    private val noteRepository: NoteRepository,
    private val sectionRepository: SectionRepository,
    private val notebookRepository: NotebookRepository,
    private val sectionId: Long
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotesUiState())
    val uiState: StateFlow<NotesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            noteRepository.getNotesBySection(sectionId).collect { notes ->
                _uiState.update { it.copy(notes = notes) }
            }
        }
        viewModelScope.launch {
            sectionRepository.getSubSections(sectionId).collect { sections ->
                _uiState.update { it.copy(subSections = sections) }
            }
        }
    }

    fun createNote(title: String): Long {
        var newId = 0L
        viewModelScope.launch {
            newId = noteRepository.insertNote(
                Note(sectionId = sectionId, title = title, contentMarkdown = "")
            )
        }
        return newId
    }

    fun createSubSection(name: String, color: String) {
        viewModelScope.launch {
            val parent = sectionRepository.getSectionById(sectionId) ?: return@launch
            sectionRepository.insertSection(
                Section(
                    notebookId      = parent.notebookId,
                    parentSectionId = sectionId,
                    name            = name,
                    color           = color
                )
            )
        }
        hideCreateSubSectionDialog()
    }

    fun deleteNote(note: Note) {
        viewModelScope.launch { noteRepository.deleteNote(note) }
        hideDeleteDialog()
    }

    fun showMoveDialog(note: Note) {
        viewModelScope.launch {
            val list = notebookRepository.allNotebooks.first()
            _uiState.update { it.copy(noteToMove = note, moveNotebooks = list, moveSelectedNotebook = null, moveSections = emptyList()) }
        }
    }

    fun selectNotebookForMove(notebook: Notebook) {
        viewModelScope.launch {
            val sections = sectionRepository.getSectionsByNotebookOnce(notebook.id)
            _uiState.update { it.copy(moveSelectedNotebook = notebook, moveSections = sections) }
        }
    }

    fun moveNote(targetSectionId: Long) {
        val note = _uiState.value.noteToMove ?: return
        viewModelScope.launch { noteRepository.moveNote(note.id, targetSectionId) }
        hideMoveDialog()
    }

    fun hideMoveDialog() = _uiState.update {
        it.copy(noteToMove = null, moveNotebooks = emptyList(), moveSelectedNotebook = null, moveSections = emptyList())
    }

    fun showDeleteDialog(note: Note)          = _uiState.update { it.copy(noteToDelete = note) }
    fun hideDeleteDialog()                    = _uiState.update { it.copy(noteToDelete = null) }
    fun showCreateSubSectionDialog()          = _uiState.update { it.copy(showCreateSubSectionDialog = true) }
    fun hideCreateSubSectionDialog()          = _uiState.update { it.copy(showCreateSubSectionDialog = false) }

    fun enterSelectionMode(noteId: Long)      = _uiState.update { it.copy(isSelectionMode = true, selectedNoteIds = setOf(noteId)) }
    fun exitSelectionMode()                   = _uiState.update { it.copy(isSelectionMode = false, selectedNoteIds = emptySet()) }
    fun toggleNoteSelection(noteId: Long)     = _uiState.update {
        val updated = if (noteId in it.selectedNoteIds) it.selectedNoteIds - noteId
                      else it.selectedNoteIds + noteId
        it.copy(selectedNoteIds = updated, isSelectionMode = updated.isNotEmpty())
    }
    fun selectedNotes(): List<Note>           = _uiState.value.notes.filter { it.id in _uiState.value.selectedNoteIds }

    companion object {
        fun factory(
            noteRepository: NoteRepository,
            sectionRepository: SectionRepository,
            notebookRepository: NotebookRepository,
            sectionId: Long
        ): ViewModelProvider.Factory =
            viewModelFactory {
                initializer { NoteViewModel(noteRepository, sectionRepository, notebookRepository, sectionId) }
            }
    }
}
