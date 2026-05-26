package com.tuapp.notasmd.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.tuapp.notasmd.data.local.entity.Notebook
import com.tuapp.notasmd.data.repository.NotebookRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NotebooksUiState(
    val notebooks: List<Notebook> = emptyList(),
    val showCreateDialog: Boolean   = false,
    val notebookToEdit: Notebook?   = null,
    val notebookToDelete: Notebook? = null
)

class NotebookViewModel(
    private val repository: NotebookRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotebooksUiState())
    val uiState: StateFlow<NotebooksUiState> = _uiState.asStateFlow()

    init {
        // Escucha cambios en la BD y actualiza el estado automáticamente
        viewModelScope.launch {
            repository.allNotebooks.collect { notebooks ->
                _uiState.update { it.copy(notebooks = notebooks) }
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
        viewModelScope.launch {
            repository.deleteNotebook(notebook)
        }
        hideDeleteDialog()
    }

    // Funciones para controlar los diálogos
    fun showCreateDialog()               = _uiState.update { it.copy(showCreateDialog = true) }
    fun hideCreateDialog()               = _uiState.update { it.copy(showCreateDialog = false) }
    fun showEditDialog(n: Notebook)      = _uiState.update { it.copy(notebookToEdit = n) }
    fun hideEditDialog()                 = _uiState.update { it.copy(notebookToEdit = null) }
    fun showDeleteDialog(n: Notebook)    = _uiState.update { it.copy(notebookToDelete = n) }
    fun hideDeleteDialog()               = _uiState.update { it.copy(notebookToDelete = null) }

    companion object {
        fun factory(repository: NotebookRepository): ViewModelProvider.Factory =
            viewModelFactory {
                initializer { NotebookViewModel(repository) }
            }
    }
}