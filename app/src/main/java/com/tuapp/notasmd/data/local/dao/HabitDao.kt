package com.tuapp.notasmd.data.local.dao

import androidx.room.*
import com.tuapp.notasmd.data.local.entity.Habit
import com.tuapp.notasmd.data.local.entity.HabitEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {

    @Query("SELECT * FROM habits ORDER BY name ASC")
    fun getAllHabits(): Flow<List<Habit>>

    @Query("SELECT * FROM habit_entries WHERE date BETWEEN :from AND :to")
    fun getEntriesInRange(from: String, to: String): Flow<List<HabitEntry>>

    @Query("SELECT * FROM habit_entries WHERE habitId = :habitId ORDER BY date DESC")
    suspend fun getAllEntriesForHabit(habitId: Long): List<HabitEntry>

    @Query("SELECT * FROM habit_entries WHERE habitId = :habitId AND date = :date LIMIT 1")
    suspend fun getEntry(habitId: Long, date: String): HabitEntry?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertEntry(entry: HabitEntry)

    @Query("DELETE FROM habit_entries WHERE habitId = :habitId AND date = :date")
    suspend fun deleteEntry(habitId: Long, date: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabit(habit: Habit): Long

    @Update
    suspend fun updateHabit(habit: Habit)

    @Delete
    suspend fun deleteHabit(habit: Habit)
}
