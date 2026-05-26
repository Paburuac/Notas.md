package com.tuapp.notasmd.data.repository

import com.tuapp.notasmd.data.local.dao.NotebookDao
import com.tuapp.notasmd.data.local.entity.Notebook
import kotlinx.coroutines.flow.Flow

class NotebookRepository(private val notebookDao: NotebookDao) {

    val allNotebooks: Flow<List<Notebook>> = notebookDao.getAllNotebooks()

    suspend fun getNotebookById(id: Long): Notebook? =
        notebookDao.getNotebookById(id)

    suspend fun insertNotebook(notebook: Notebook): Long =
        notebookDao.insertNotebook(notebook)

    suspend fun updateNotebook(notebook: Notebook) =
        notebookDao.updateNotebook(notebook)

    suspend fun deleteNotebook(notebook: Notebook) =
        notebookDao.deleteNotebook(notebook)
}