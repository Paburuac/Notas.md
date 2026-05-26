package com.tuapp.notasmd.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.tuapp.notasmd.data.local.dao.NotebookDao
import com.tuapp.notasmd.data.local.dao.NoteDao
import com.tuapp.notasmd.data.local.dao.SectionDao
import com.tuapp.notasmd.data.local.entity.Notebook
import com.tuapp.notasmd.data.local.entity.Note
import com.tuapp.notasmd.data.local.entity.Section

@Database(
    entities    = [Notebook::class, Section::class, Note::class],
    version     = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun notebookDao(): NotebookDao
    abstract fun sectionDao(): SectionDao
    abstract fun noteDao(): NoteDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            // Solo crea una instancia en toda la vida de la app
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "notasmd_db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}