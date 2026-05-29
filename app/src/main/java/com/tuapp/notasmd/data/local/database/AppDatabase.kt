package com.tuapp.notasmd.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.tuapp.notasmd.data.local.dao.NotebookDao
import com.tuapp.notasmd.data.local.dao.NoteDao
import com.tuapp.notasmd.data.local.dao.SectionDao
import com.tuapp.notasmd.data.local.entity.Notebook
import com.tuapp.notasmd.data.local.entity.Note
import com.tuapp.notasmd.data.local.entity.Section

@Database(
    entities    = [Notebook::class, Section::class, Note::class],
    version     = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun notebookDao(): NotebookDao
    abstract fun sectionDao(): SectionDao
    abstract fun noteDao(): NoteDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE sections ADD COLUMN parentSectionId INTEGER")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_sections_parentSectionId ON sections(parentSectionId)")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "notasmd_db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}