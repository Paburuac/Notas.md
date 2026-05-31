package com.tuapp.notasmd.data.repository

import com.tuapp.notasmd.data.local.dao.HabitDao
import com.tuapp.notasmd.data.local.entity.Habit
import com.tuapp.notasmd.data.local.entity.HabitEntry
import kotlinx.coroutines.flow.Flow

class HabitRepository(private val dao: HabitDao) {

    val allHabits: Flow<List<Habit>> = dao.getAllHabits()

    fun getEntriesInRange(from: String, to: String): Flow<List<HabitEntry>> =
        dao.getEntriesInRange(from, to)

    suspend fun getAllEntriesForHabit(habitId: Long): List<HabitEntry> =
        dao.getAllEntriesForHabit(habitId)

    suspend fun toggleEntry(habitId: Long, date: String) {
        if (dao.getEntry(habitId, date) != null) {
            dao.deleteEntry(habitId, date)
        } else {
            dao.insertEntry(HabitEntry(habitId = habitId, date = date))
        }
    }

    suspend fun insertHabit(habit: Habit): Long = dao.insertHabit(habit)

    suspend fun updateHabit(habit: Habit) = dao.updateHabit(habit)

    suspend fun deleteHabit(habit: Habit) = dao.deleteHabit(habit)
}
