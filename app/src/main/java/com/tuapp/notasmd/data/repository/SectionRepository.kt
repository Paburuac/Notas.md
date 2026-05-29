package com.tuapp.notasmd.data.repository

import com.tuapp.notasmd.data.local.dao.SectionDao
import com.tuapp.notasmd.data.local.entity.Section
import kotlinx.coroutines.flow.Flow

class SectionRepository(private val sectionDao: SectionDao) {

    fun getSectionsByNotebook(notebookId: Long): Flow<List<Section>> =
        sectionDao.getSectionsByNotebook(notebookId)

    fun getSubSections(parentSectionId: Long): Flow<List<Section>> =
        sectionDao.getSubSections(parentSectionId)

    suspend fun getSectionById(id: Long): Section? =
        sectionDao.getSectionById(id)

    suspend fun insertSection(section: Section): Long =
        sectionDao.insertSection(section)

    suspend fun updateSection(section: Section) =
        sectionDao.updateSection(section)

    suspend fun deleteSection(section: Section) =
        sectionDao.deleteSection(section)
}