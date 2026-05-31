package com.tuapp.notasmd.data.repository

import com.tuapp.notasmd.data.local.dao.TaskDao
import com.tuapp.notasmd.data.local.entity.Task
import kotlinx.coroutines.flow.Flow

class TaskRepository(private val dao: TaskDao) {

    val allTasks: Flow<List<Task>> = dao.getAllTasks()

    suspend fun getTaskById(id: Long): Task? = dao.getTaskById(id)

    suspend fun getRecurringTasks(): List<Task> = dao.getRecurringTasks()

    suspend fun insertTask(task: Task): Long = dao.insertTask(task)

    suspend fun updateTask(task: Task) = dao.updateTask(task)

    suspend fun deleteTask(task: Task) = dao.deleteTask(task)
}
