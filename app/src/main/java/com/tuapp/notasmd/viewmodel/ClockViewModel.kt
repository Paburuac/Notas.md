package com.tuapp.notasmd.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.tuapp.notasmd.data.local.entity.Alarm
import com.tuapp.notasmd.data.repository.AlarmRepository
import com.tuapp.notasmd.worker.AlarmWorker
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ── Stopwatch ──────────────────────────────────────────────────────────────
data class StopwatchState(
    val elapsedMs: Long    = 0L,
    val isRunning: Boolean = false
)

// ── Timer ──────────────────────────────────────────────────────────────────
data class TimerState(
    val totalMs:     Long    = 0L,
    val remainingMs: Long    = 0L,
    val isRunning:   Boolean = false,
    val isFinished:  Boolean = false
)

// ── Alarm ──────────────────────────────────────────────────────────────────
data class AlarmUiState(
    val alarms: List<Alarm>       = emptyList(),
    val showCreateDialog: Boolean = false
)

data class ClockUiState(
    val stopwatch: StopwatchState = StopwatchState(),
    val timer:     TimerState     = TimerState(),
    val alarm:     AlarmUiState   = AlarmUiState()
)

class ClockViewModel(
    private val alarmRepository: AlarmRepository,
    private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(ClockUiState())
    val uiState: StateFlow<ClockUiState> = _uiState.asStateFlow()

    private var stopwatchJob: Job? = null
    private var timerJob: Job?     = null

    init {
        viewModelScope.launch {
            alarmRepository.allAlarms.collect { alarms ->
                _uiState.update { it.copy(alarm = it.alarm.copy(alarms = alarms)) }
            }
        }
    }

    // ── Stopwatch ──────────────────────────────────────────────────────────
    fun startStopwatch() {
        if (_uiState.value.stopwatch.isRunning) return
        _uiState.update { it.copy(stopwatch = it.stopwatch.copy(isRunning = true)) }
        stopwatchJob = viewModelScope.launch {
            while (true) {
                delay(10L)
                _uiState.update { it.copy(stopwatch = it.stopwatch.copy(elapsedMs = it.stopwatch.elapsedMs + 10L)) }
            }
        }
    }

    fun pauseStopwatch() {
        stopwatchJob?.cancel()
        _uiState.update { it.copy(stopwatch = it.stopwatch.copy(isRunning = false)) }
    }

    fun resetStopwatch() {
        stopwatchJob?.cancel()
        _uiState.update { it.copy(stopwatch = StopwatchState()) }
    }

    // ── Timer ──────────────────────────────────────────────────────────────
    fun setTimer(hours: Int, minutes: Int, seconds: Int) {
        val totalMs = (hours * 3600L + minutes * 60L + seconds) * 1000L
        _uiState.update { it.copy(timer = TimerState(totalMs = totalMs, remainingMs = totalMs)) }
    }

    fun startTimer() {
        val state = _uiState.value.timer
        if (state.isRunning || state.remainingMs <= 0L) return
        _uiState.update { it.copy(timer = it.timer.copy(isRunning = true, isFinished = false)) }
        timerJob = viewModelScope.launch {
            while (true) {
                delay(10L)
                val remaining = _uiState.value.timer.remainingMs - 10L
                if (remaining <= 0L) {
                    _uiState.update { it.copy(timer = it.timer.copy(remainingMs = 0L, isRunning = false, isFinished = true)) }
                    break
                }
                _uiState.update { it.copy(timer = it.timer.copy(remainingMs = remaining)) }
            }
        }
    }

    fun pauseTimer() {
        timerJob?.cancel()
        _uiState.update { it.copy(timer = it.timer.copy(isRunning = false)) }
    }

    fun resetTimer() {
        timerJob?.cancel()
        _uiState.update { it.copy(timer = it.timer.copy(remainingMs = it.timer.totalMs, isRunning = false, isFinished = false)) }
    }

    // ── Alarms ─────────────────────────────────────────────────────────────
    fun createAlarm(hour: Int, minute: Int, label: String, repeatDays: String) {
        viewModelScope.launch {
            val id = alarmRepository.insertAlarm(Alarm(hour = hour, minute = minute, label = label, repeatDays = repeatDays))
            AlarmWorker.schedule(context, id, hour, minute, label, repeatDays)
        }
        hideCreateAlarmDialog()
    }

    fun toggleAlarm(alarm: Alarm) {
        viewModelScope.launch {
            val updated = alarm.copy(isEnabled = !alarm.isEnabled)
            alarmRepository.updateAlarm(updated)
            if (updated.isEnabled) {
                AlarmWorker.schedule(context, alarm.id, alarm.hour, alarm.minute, alarm.label, alarm.repeatDays)
            } else {
                AlarmWorker.cancel(context, alarm.id)
            }
        }
    }

    fun deleteAlarm(alarm: Alarm) {
        viewModelScope.launch {
            AlarmWorker.cancel(context, alarm.id)
            alarmRepository.deleteAlarm(alarm)
        }
    }

    fun showCreateAlarmDialog() = _uiState.update { it.copy(alarm = it.alarm.copy(showCreateDialog = true)) }
    fun hideCreateAlarmDialog() = _uiState.update { it.copy(alarm = it.alarm.copy(showCreateDialog = false)) }

    override fun onCleared() {
        stopwatchJob?.cancel()
        timerJob?.cancel()
    }

    companion object {
        fun factory(alarmRepository: AlarmRepository, context: Context): ViewModelProvider.Factory =
            viewModelFactory { initializer { ClockViewModel(alarmRepository, context) } }
    }
}
