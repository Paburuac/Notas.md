package com.tuapp.notasmd.export

import com.tuapp.notasmd.data.local.entity.Note
import com.tuapp.notasmd.data.local.entity.Notebook
import com.tuapp.notasmd.data.local.entity.Section
import com.tuapp.notasmd.data.repository.NotebookRepository
import com.tuapp.notasmd.data.repository.NoteRepository
import com.tuapp.notasmd.data.repository.SectionRepository
import kotlinx.coroutines.flow.first
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ExportManager(
    private val notebookRepository: NotebookRepository,
    private val sectionRepository:  SectionRepository,
    private val noteRepository:     NoteRepository
) {

    suspend fun exportAll(outputStream: OutputStream) {
        val notebooks = notebookRepository.allNotebooks.first()
        ZipOutputStream(outputStream.buffered()).use { zip ->
            notebooks.forEach { notebook -> writeNotebook(notebook, zip) }
        }
    }

    suspend fun exportNotebook(notebook: Notebook, outputStream: OutputStream) {
        ZipOutputStream(outputStream.buffered()).use { zip ->
            writeNotebook(notebook, zip)
        }
    }

    suspend fun exportNotes(notes: List<Note>, outputStream: OutputStream) {
        ZipOutputStream(outputStream.buffered()).use { zip ->
            // Agrupar por sección para construir rutas con contexto
            val sectionIds = notes.map { it.sectionId }.distinct()
            val sections   = sectionIds.mapNotNull { sectionRepository.getSectionById(it) }
            val sectionMap = sections.associateBy { it.id }

            // Incluir antecesores para construir rutas completas
            val allSections = buildAncestorMap(sections, sectionMap.toMutableMap())

            notes.forEach { note ->
                val sectionPath = buildSectionPath(note.sectionId, allSections)
                val fileName    = sanitize(note.title.ifBlank { "Sin título" }) + ".md"
                zip.putNextEntry(ZipEntry("$sectionPath/$fileName"))
                zip.write(noteToMarkdown(note).toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
        }
    }

    private suspend fun writeNotebook(notebook: Notebook, zip: ZipOutputStream) {
        val sections   = sectionRepository.getSectionsByNotebookOnce(notebook.id)
        val sectionMap = sections.associateBy { it.id }
        val prefix     = sanitize(notebook.name)

        sections.forEach { section ->
            val sectionPath = "$prefix/${buildSectionPath(section.id, sectionMap)}"
            val notes       = noteRepository.getNotesBySectionOnce(section.id)
            notes.forEach { note ->
                val fileName = sanitize(note.title.ifBlank { "Sin título" }) + ".md"
                zip.putNextEntry(ZipEntry("$sectionPath/$fileName"))
                zip.write(noteToMarkdown(note).toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
        }
    }

    // Construye la ruta relativa de una sección respetando sub-secciones
    private fun buildSectionPath(sectionId: Long, sectionMap: Map<Long, Section>): String {
        val section = sectionMap[sectionId] ?: return "Sin sección"
        val parent  = section.parentSectionId
        return if (parent == null) {
            sanitize(section.name)
        } else {
            buildSectionPath(parent, sectionMap) + "/" + sanitize(section.name)
        }
    }

    // Cuando exportamos notas sueltas puede que el sectionMap no tenga los antecesores;
    // los cargamos aquí para que buildSectionPath funcione correctamente.
    private suspend fun buildAncestorMap(
        sections: List<Section>,
        map: MutableMap<Long, Section>
    ): Map<Long, Section> {
        val toFetch = sections.mapNotNull { it.parentSectionId }.filter { it !in map }
        if (toFetch.isEmpty()) return map
        val parents = toFetch.mapNotNull { sectionRepository.getSectionById(it) }
        parents.forEach { map[it.id] = it }
        return buildAncestorMap(parents, map)
    }

    private fun noteToMarkdown(note: Note) = buildString {
        if (note.title.isNotBlank()) append("# ${note.title}\n\n")
        append(note.contentMarkdown)
    }

    private fun sanitize(name: String) =
        name.replace(Regex("""[/\\:*?"<>|]"""), "_").trim().take(100).ifBlank { "sin_nombre" }
}
