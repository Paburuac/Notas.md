package com.tuapp.notasmd.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.tuapp.notasmd.data.local.entity.Section
import com.tuapp.notasmd.data.repository.SectionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SectionsUiState(
    val sections: List<Section>     = emptyList(),
    val showCreateDialog: Boolean   = false,
    val sectionToEdit: Section?     = null,
    val sectionToDelete: Section?   = null
)

class SectionViewModel(
    private val repository: SectionRepository,
    private val notebookId: Long
) : ViewModel() {

    private val _uiState = MutableStateFlow(SectionsUiState())
    val uiState: StateFlow<SectionsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getSectionsByNotebook(notebookId).collect { sections ->
                _uiState.update { it.copy(sections = sections) }
            }
        }
    }

    fun createSection(name: String, color: String) {
        viewModelScope.launch {
            repository.insertSection(Section(notebookId = notebookId, name = name, color = color))
        }
        hideCreateDialog()
    }

    fun updateSection(section: Section, newName: String, newColor: String) {
        viewModelScope.launch {
            repository.updateSection(
                section.copy(
                    name      = newName,
                    color     = newColor,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
        hideEditDialog()
    }

    fun deleteSection(section: Section) {
        viewModelScope.launch {
            repository.deleteSection(section)
        }
        hideDeleteDialog()
    }

    fun showCreateDialog()               = _uiState.update { it.copy(showCreateDialog = true) }
    fun hideCreateDialog()               = _uiState.update { it.copy(showCreateDialog = false) }
    fun showEditDialog(s: Section)       = _uiState.update { it.copy(sectionToEdit = s) }
    fun hideEditDialog()                 = _uiState.update { it.copy(sectionToEdit = null) }
    fun showDeleteDialog(s: Section)     = _uiState.update { it.copy(sectionToDelete = s) }
    fun hideDeleteDialog()               = _uiState.update { it.copy(sectionToDelete = null) }

    companion object {
        fun factory(repository: SectionRepository, notebookId: Long): ViewModelProvider.Factory =
            viewModelFactory {
                initializer { SectionViewModel(repository, notebookId) }
            }
    }
}