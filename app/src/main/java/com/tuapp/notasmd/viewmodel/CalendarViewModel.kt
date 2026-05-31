package com.tuapp.notasmd.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.tuapp.notasmd.data.local.entity.Habit
import com.tuapp.notasmd.data.local.entity.HabitEntry
import com.tuapp.notasmd.data.repository.HabitRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

data class HabitStreak(val habit: Habit, val streak: Int)

data class CalendarUiState(
    val currentMonth: YearMonth                      = YearMonth.now(),
    val habits: List<Habit>                          = emptyList(),
    val entriesByDate: Map<String, List<HabitEntry>> = emptyMap(),
    val streaks: List<HabitStreak>                   = emptyList(),
    val selectedDate: LocalDate?                     = null,
    val selectedDateEntries: Set<Long>               = emptySet()
)

@OptIn(ExperimentalCoroutinesApi::class)
class CalendarViewModel(private val repository: HabitRepository) : ViewModel() {

    private val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    private val _month   = MutableStateFlow(YearMonth.now())
    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            _month.flatMapLatest { month ->
                val from = month.atDay(1).minusDays(6).format(fmt)
                val to   = month.atEndOfMonth().plusDays(6).format(fmt)
                combine(repository.allHabits, repository.getEntriesInRange(from, to)) { habits, entries ->
                    habits to entries
                }
            }.collect { (habits, entries) ->
                val byDate  = entries.groupBy { it.date }
                val streaks = habits.map { habit ->
                    val all = repository.getAllEntriesForHabit(habit.id)
                        .map { LocalDate.parse(it.date, fmt) }.toSet()
                    HabitStreak(habit, calculateStreak(all))
                }
                _uiState.update { it.copy(habits = habits, entriesByDate = byDate, streaks = streaks) }
            }
        }
    }

    fun goToPreviousMonth() {
        _month.update { it.minusMonths(1) }
        _uiState.update { it.copy(currentMonth = _month.value, selectedDate = null) }
    }

    fun goToNextMonth() {
        _month.update { it.plusMonths(1) }
        _uiState.update { it.copy(currentMonth = _month.value, selectedDate = null) }
    }

    fun selectDate(date: LocalDate) {
        val marked = _uiState.value.entriesByDate[date.format(fmt)]
            ?.map { it.habitId }?.toSet() ?: emptySet()
        _uiState.update { it.copy(selectedDate = date, selectedDateEntries = marked) }
    }

    fun dismissDateDialog() = _uiState.update { it.copy(selectedDate = null) }

    fun toggleHabitOnDate(habitId: Long) {
        val date = _uiState.value.selectedDate?.format(fmt) ?: return
        viewModelScope.launch {
            repository.toggleEntry(habitId, date)
            val marked = _uiState.value.selectedDateEntries.toMutableSet()
            if (habitId in marked) marked.remove(habitId) else marked.add(habitId)
            _uiState.update { it.copy(selectedDateEntries = marked) }
        }
    }

    fun createHabit(name: String, color: String) {
        viewModelScope.launch { repository.insertHabit(Habit(name = name, color = color)) }
    }

    fun updateHabit(habit: Habit, name: String, color: String) {
        viewModelScope.launch { repository.updateHabit(habit.copy(name = name, color = color)) }
    }

    fun deleteHabit(habit: Habit) {
        viewModelScope.launch { repository.deleteHabit(habit) }
    }

    private fun calculateStreak(dates: Set<LocalDate>): Int {
        var streak   = 0
        var expected = LocalDate.now()
        if (expected !in dates) expected = expected.minusDays(1)
        while (expected in dates) {
            streak++
            expected = expected.minusDays(1)
        }
        return streak
    }

    companion object {
        fun factory(repository: HabitRepository): ViewModelProvider.Factory =
            viewModelFactory { initializer { CalendarViewModel(repository) } }
    }
}
