package com.tuapp.notasmd.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alarms")
data class Alarm(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val hour: Int,
    val minute: Int,
    val label: String = "",
    val isEnabled: Boolean = true,
    val repeatDays: String = ""   // "1,2,3,4,5" = Lun-Vie; vacío = una sola vez
)
