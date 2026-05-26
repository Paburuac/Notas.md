package com.tuapp.notasmd

import android.app.Application
import com.tuapp.notasmd.data.local.database.AppDatabase
import com.tuapp.notasmd.data.repository.NoteRepository
import com.tuapp.notasmd.data.repository.NotebookRepository
import com.tuapp.notasmd.data.repository.SectionRepository
import com.tuapp.notasmd.data.repository.ThemeRepository

class NotasMdApp : Application() {
    val database         by lazy { AppDatabase.getInstance(this) }
    val notebookRepository by lazy { NotebookRepository(database.notebookDao()) }
    val sectionRepository  by lazy { SectionRepository(database.sectionDao()) }
    val noteRepository     by lazy { NoteRepository(database.noteDao()) }
    val themeRepository    by lazy { ThemeRepository(this) }
}