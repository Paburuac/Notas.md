package com.tuapp.notasmd.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.tuapp.notasmd.data.local.entity.Task
import com.tuapp.notasmd.data.local.entity.TaskCategory
import com.tuapp.notasmd.data.repository.TaskCategoryRepository
import com.tuapp.notasmd.data.repository.TaskRepository
import com.tuapp.notasmd.worker.RecurringTaskWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ChecklistView { LIST, KANBAN }

data class ChecklistUiState(
    val tasks: List<Task>                   = emptyList(),
    val categories: List<TaskCategory>      = emptyList(),
    val view: ChecklistView                 = ChecklistView.LIST,
    val showCreateTaskDialog: Boolean       = false,
    val showCreateCategoryDialog: Boolean   = false,
    val taskToDelete: Task?                 = null,
    val taskToEdit: Task?                   = null
)

class ChecklistViewModel(
    private val taskRepository: TaskRepository,
    private val categoryRepository: TaskCategoryRepository,
    private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChecklistUiState())
    val uiState: StateFlow<ChecklistUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(taskRepository.allTasks, categoryRepository.allCategories) { tasks, cats ->
                tasks to cats
            }.collect { (tasks, cats) ->
                _uiState.update { it.copy(tasks = tasks, categories = cats) }
            }
        }
    }

    fun toggleView() = _uiState.update {
        it.copy(view = if (it.view == ChecklistView.LIST) ChecklistView.KANBAN else ChecklistView.LIST)
    }

    fun toggleComplete(task: Task) {
        viewModelScope.launch {
            taskRepository.updateTask(task.copy(isCompleted = !task.isCompleted, updatedAt = System.currentTimeMillis()))
        }
    }

    fun createTask(
        title: String,
        categoryId: Long?,
        isRecurring: Boolean,
        recurUnit: String,
        recurAmount: Int
    ) {
        viewModelScope.launch {
            val nextDueAt = if (isRecurring)
                RecurringTaskWorker.computeNextDue(recurUnit, recurAmount) else null
            val id = taskRepository.insertTask(
                Task(
                    title       = title,
                    categoryId  = categoryId,
                    isRecurring = isRecurring,
                    recurUnit   = recurUnit,
                    recurAmount = recurAmount,
                    nextDueAt   = nextDueAt
                )
            )
            if (isRecurring && nextDueAt != null) {
                RecurringTaskWorker.scheduleNext(context, id, title, recurUnit, recurAmount, nextDueAt)
            }
        }
        hideCreateTaskDialog()
    }

    fun saveEditedTask(task: Task) {
        viewModelScope.launch {
            val nextDueAt = if (task.isRecurring)
                RecurringTaskWorker.computeNextDue(task.recurUnit, task.recurAmount) else null
            val updated = task.copy(nextDueAt = nextDueAt, updatedAt = System.currentTimeMillis())
            taskRepository.updateTask(updated)
            RecurringTaskWorker.cancel(context, task.id)
            if (task.isRecurring && nextDueAt != null) {
                RecurringTaskWorker.scheduleNext(context, task.id, task.title, task.recurUnit, task.recurAmount, nextDueAt)
            }
        }
        _uiState.update { it.copy(taskToEdit = null) }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            RecurringTaskWorker.cancel(context, task.id)
            taskRepository.deleteTask(task)
        }
        hideDeleteDialog()
    }

    fun createCategory(name: String, color: String) {
        viewModelScope.launch {
            categoryRepository.insertCategory(TaskCategory(name = name, color = color))
        }
        hideCreateCategoryDialog()
    }

    fun deleteCategory(category: TaskCategory) {
        viewModelScope.launch { categoryRepository.deleteCategory(category) }
    }

    fun showCreateTaskDialog()      = _uiState.update { it.copy(showCreateTaskDialog = true) }
    fun hideCreateTaskDialog()      = _uiState.update { it.copy(showCreateTaskDialog = false) }
    fun showCreateCategoryDialog()  = _uiState.update { it.copy(showCreateCategoryDialog = true) }
    fun hideCreateCategoryDialog()  = _uiState.update { it.copy(showCreateCategoryDialog = false) }
    fun showDeleteDialog(task: Task) = _uiState.update { it.copy(taskToDelete = task) }
    fun hideDeleteDialog()           = _uiState.update { it.copy(taskToDelete = null) }
    fun showEditDialog(task: Task)   = _uiState.update { it.copy(taskToEdit = task) }
    fun hideEditDialog()             = _uiState.update { it.copy(taskToEdit = null) }

    companion object {
        fun factory(
            taskRepository: TaskRepository,
            categoryRepository: TaskCategoryRepository,
            context: Context
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer { ChecklistViewModel(taskRepository, categoryRepository, context) }
        }
    }
}
