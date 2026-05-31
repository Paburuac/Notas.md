package com.tuapp.notasmd

import android.app.Application
import com.tuapp.notasmd.data.local.database.AppDatabase
import com.tuapp.notasmd.data.repository.AlarmRepository
import com.tuapp.notasmd.data.repository.HabitRepository
import com.tuapp.notasmd.data.repository.NoteRepository
import com.tuapp.notasmd.data.repository.NotebookRepository
import com.tuapp.notasmd.data.repository.RecentNotesRepository
import com.tuapp.notasmd.data.repository.SectionRepository
import com.tuapp.notasmd.data.repository.TaskCategoryRepository
import com.tuapp.notasmd.data.repository.TaskRepository
import com.tuapp.notasmd.data.repository.ThemeRepository

class NotasMdApp : Application() {
    val database              by lazy { AppDatabase.getInstance(this) }
    val notebookRepository    by lazy { NotebookRepository(database.notebookDao()) }
    val sectionRepository     by lazy { SectionRepository(database.sectionDao()) }
    val noteRepository        by lazy { NoteRepository(database.noteDao()) }
    val themeRepository       by lazy { ThemeRepository(this) }
    val recentNotesRepository by lazy { RecentNotesRepository(this) }
    val taskCategoryRepository by lazy { TaskCategoryRepository(database.taskCategoryDao()) }
    val taskRepository         by lazy { TaskRepository(database.taskDao()) }
    val alarmRepository        by lazy { AlarmRepository(database.alarmDao()) }
    val habitRepository        by lazy { HabitRepository(database.habitDao()) }
}
