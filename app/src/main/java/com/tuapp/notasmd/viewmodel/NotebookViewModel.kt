package com.tuapp.notasmd.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.tuapp.notasmd.data.local.entity.Notebook
import com.tuapp.notasmd.data.local.entity.Note
import com.tuapp.notasmd.data.local.entity.Section
import com.tuapp.notasmd.data.repository.NotebookRepository
import com.tuapp.notasmd.data.repository.NoteRepository
import com.tuapp.notasmd.data.repository.SectionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PinnedNoteItem(
    val note: Note,
    val sectionName: String,
    val notebookName: String
)

data class NotebooksUiState(
    val notebooks: List<Notebook>           = emptyList(),
    val pinnedNotes: List<PinnedNoteItem>   = emptyList(),
    val showCreateDialog: Boolean           = false,
    val notebookToEdit: Notebook?           = null,
    val notebookToDelete: Notebook?         = null
)

class NotebookViewModel(
    private val repository: NotebookRepository,
    private val noteRepository: NoteRepository,
    private val sectionRepository: SectionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotebooksUiState())
    val uiState: StateFlow<NotebooksUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.allNotebooks.collect { notebooks ->
                _uiState.update { it.copy(notebooks = notebooks) }
            }
        }
        viewModelScope.launch {
            noteRepository.getPinnedNotes().collect { notes ->
                val items = notes.mapNotNull { note ->
                    val section  = sectionRepository.getSectionById(note.sectionId) ?: return@mapNotNull null
                    val notebook = repository.getNotebookById(section.notebookId) ?: return@mapNotNull null
                    PinnedNoteItem(note, section.name, notebook.name)
                }
                _uiState.update { it.copy(pinnedNotes = items) }
            }
        }
    }

    fun createNotebook(name: String, color: String) {
        viewModelScope.launch {
            repository.insertNotebook(Notebook(name = name, color = color))
        }
        hideCreateDialog()
    }

    fun updateNotebook(notebook: Notebook, newName: String, newColor: String) {
        viewModelScope.launch {
            repository.updateNotebook(
                notebook.copy(
                    name      = newName,
                    color     = newColor,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
        hideEditDialog()
    }

    fun deleteNotebook(notebook: Notebook) {
        viewModelScope.launch { repository.deleteNotebook(notebook) }
        hideDeleteDialog()
    }

    fun showCreateDialog()               = _uiState.update { it.copy(showCreateDialog = true) }
    fun hideCreateDialog()               = _uiState.update { it.copy(showCreateDialog = false) }
    fun showEditDialog(n: Notebook)      = _uiState.update { it.copy(notebookToEdit = n) }
    fun hideEditDialog()                 = _uiState.update { it.copy(notebookToEdit = null) }
    fun showDeleteDialog(n: Notebook)    = _uiState.update { it.copy(notebookToDelete = n) }
    fun hideDeleteDialog()               = _uiState.update { it.copy(notebookToDelete = null) }

    companion object {
        fun factory(
            repository: NotebookRepository,
            noteRepository: NoteRepository,
            sectionRepository: SectionRepository
        ): ViewModelProvider.Factory =
            viewModelFactory {
                initializer { NotebookViewModel(repository, noteRepository, sectionRepository) }
            }
    }
}
