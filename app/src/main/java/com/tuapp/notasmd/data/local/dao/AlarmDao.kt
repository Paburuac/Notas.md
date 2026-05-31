package com.tuapp.notasmd.data.local.dao

import androidx.room.*
import com.tuapp.notasmd.data.local.entity.Alarm
import kotlinx.coroutines.flow.Flow

@Dao
interface AlarmDao {

    @Query("SELECT * FROM alarms ORDER BY hour ASC, minute ASC")
    fun getAllAlarms(): Flow<List<Alarm>>

    @Query("SELECT * FROM alarms WHERE id = :id")
    suspend fun getAlarmById(id: Long): Alarm?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlarm(alarm: Alarm): Long

    @Update
    suspend fun updateAlarm(alarm: Alarm)

    @Delete
    suspend fun deleteAlarm(alarm: Alarm)
}
