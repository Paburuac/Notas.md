package com.tuapp.notasmd.data.repository

import com.tuapp.notasmd.data.local.dao.AlarmDao
import com.tuapp.notasmd.data.local.entity.Alarm
import kotlinx.coroutines.flow.Flow

class AlarmRepository(private val dao: AlarmDao) {

    val allAlarms: Flow<List<Alarm>> = dao.getAllAlarms()

    suspend fun getAlarmById(id: Long): Alarm? = dao.getAlarmById(id)

    suspend fun insertAlarm(alarm: Alarm): Long = dao.insertAlarm(alarm)

    suspend fun updateAlarm(alarm: Alarm) = dao.updateAlarm(alarm)

    suspend fun deleteAlarm(alarm: Alarm) = dao.deleteAlarm(alarm)
}
