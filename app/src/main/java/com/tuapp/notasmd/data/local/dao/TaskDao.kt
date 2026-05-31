package com.tuapp.notasmd.data.local.dao

import androidx.room.*
import com.tuapp.notasmd.data.local.entity.Task
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    @Query("SELECT * FROM tasks ORDER BY isCompleted ASC, updatedAt DESC")
    fun getAllTasks(): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getTaskById(id: Long): Task?

    @Query("SELECT * FROM tasks WHERE isRecurring = 1 AND nextDueAt IS NOT NULL")
    suspend fun getRecurringTasks(): List<Task>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: Task): Long

    @Update
    suspend fun updateTask(task: Task)

    @Delete
    suspend fun deleteTask(task: Task)
}
