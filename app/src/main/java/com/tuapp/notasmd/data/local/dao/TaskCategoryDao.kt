package com.tuapp.notasmd.data.local.dao

import androidx.room.*
import com.tuapp.notasmd.data.local.entity.TaskCategory
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskCategoryDao {

    @Query("SELECT * FROM task_categories ORDER BY `order` ASC, name ASC")
    fun getAllCategories(): Flow<List<TaskCategory>>

    @Query("SELECT * FROM task_categories WHERE id = :id")
    suspend fun getCategoryById(id: Long): TaskCategory?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: TaskCategory): Long

    @Update
    suspend fun updateCategory(category: TaskCategory)

    @Delete
    suspend fun deleteCategory(category: TaskCategory)
}
