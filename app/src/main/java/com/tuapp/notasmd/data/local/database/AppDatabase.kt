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
import com.tuapp.notasmd.data.local.dao.AlarmDao
import com.tuapp.notasmd.data.local.dao.HabitDao
import com.tuapp.notasmd.data.local.dao.TagDao
import com.tuapp.notasmd.data.local.dao.TaskCategoryDao
import com.tuapp.notasmd.data.local.dao.TaskDao
import com.tuapp.notasmd.data.local.entity.Alarm
import com.tuapp.notasmd.data.local.entity.Habit
import com.tuapp.notasmd.data.local.entity.HabitEntry
import com.tuapp.notasmd.data.local.entity.Notebook
import com.tuapp.notasmd.data.local.entity.Note
import com.tuapp.notasmd.data.local.entity.NoteTag
import com.tuapp.notasmd.data.local.entity.Section
import com.tuapp.notasmd.data.local.entity.Tag
import com.tuapp.notasmd.data.local.entity.Task
import com.tuapp.notasmd.data.local.entity.TaskCategory

@Database(
    entities    = [Notebook::class, Section::class, Note::class, TaskCategory::class, Task::class, Alarm::class, Habit::class, HabitEntry::class, Tag::class, NoteTag::class],
    version     = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun notebookDao(): NotebookDao
    abstract fun sectionDao(): SectionDao
    abstract fun noteDao(): NoteDao
    abstract fun taskCategoryDao(): TaskCategoryDao
    abstract fun taskDao(): TaskDao
    abstract fun alarmDao(): AlarmDao
    abstract fun habitDao(): HabitDao
    abstract fun tagDao(): TagDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE sections ADD COLUMN parentSectionId INTEGER")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_sections_parentSectionId ON sections(parentSectionId)")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS tags (
                        id   INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_tags_name ON tags(name)")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS note_tags (
                        noteId INTEGER NOT NULL,
                        tagId  INTEGER NOT NULL,
                        PRIMARY KEY(noteId, tagId),
                        FOREIGN KEY(noteId) REFERENCES notes(id)  ON DELETE CASCADE,
                        FOREIGN KEY(tagId)  REFERENCES tags(id)   ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_note_tags_noteId ON note_tags(noteId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_note_tags_tagId  ON note_tags(tagId)")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS habits (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        color TEXT NOT NULL
                    )
                """.trimIndent())
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS habit_entries (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        habitId INTEGER NOT NULL,
                        date TEXT NOT NULL,
                        FOREIGN KEY(habitId) REFERENCES habits(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_habit_entries_habitId ON habit_entries(habitId)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_habit_entries_habitId_date ON habit_entries(habitId, date)")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS alarms (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        hour INTEGER NOT NULL,
                        minute INTEGER NOT NULL,
                        label TEXT NOT NULL DEFAULT '',
                        isEnabled INTEGER NOT NULL DEFAULT 1,
                        repeatDays TEXT NOT NULL DEFAULT ''
                    )
                """.trimIndent())
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS task_categories (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        color TEXT NOT NULL,
                        `order` INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS tasks (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        categoryId INTEGER,
                        title TEXT NOT NULL,
                        isCompleted INTEGER NOT NULL DEFAULT 0,
                        isRecurring INTEGER NOT NULL DEFAULT 0,
                        recurUnit TEXT NOT NULL DEFAULT 'DAYS',
                        recurAmount INTEGER NOT NULL DEFAULT 1,
                        nextDueAt INTEGER,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        FOREIGN KEY(categoryId) REFERENCES task_categories(id) ON DELETE SET NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_tasks_categoryId ON tasks(categoryId)")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "notasmd_db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
