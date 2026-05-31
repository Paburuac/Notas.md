package com.tuapp.notasmd.data.repository

import com.tuapp.notasmd.data.local.dao.TaskCategoryDao
import com.tuapp.notasmd.data.local.entity.TaskCategory
import kotlinx.coroutines.flow.Flow

class TaskCategoryRepository(private val dao: TaskCategoryDao) {

    val allCategories: Flow<List<TaskCategory>> = dao.getAllCategories()

    suspend fun getCategoryById(id: Long): TaskCategory? = dao.getCategoryById(id)

    suspend fun insertCategory(category: TaskCategory): Long = dao.insertCategory(category)

    suspend fun updateCategory(category: TaskCategory) = dao.updateCategory(category)

    suspend fun deleteCategory(category: TaskCategory) = dao.deleteCategory(category)
}
