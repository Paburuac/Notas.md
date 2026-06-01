package com.tuapp.notasmd.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.tuapp.notasmd.data.local.entity.Note
import com.tuapp.notasmd.data.repository.NoteRepository
import com.tuapp.notasmd.data.repository.TagRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import java.util.Calendar

private val TAG_REGEX = Regex("""#([a-zA-ZáéíóúÁÉÍÓÚüÜñÑ0-9_]+)""")

fun extractTags(content: String): List<String> =
    TAG_REGEX.findAll(content).map { it.groupValues[1].lowercase() }.distinct().toList()

data class SearchResults(
    val byTitle:     List<Note> = emptyList(),
    val byContent:   List<Note> = emptyList(),
    val byDayOfWeek: List<Note> = emptyList(),
    val byTag:       List<Note> = emptyList(),
    val activeTag:   String     = ""
) {
    val isEmpty get() = byTitle.isEmpty() && byContent.isEmpty() && byDayOfWeek.isEmpty() && byTag.isEmpty()
}

private val DAY_NAMES = mapOf(
    "lunes"     to Calendar.MONDAY,
    "martes"    to Calendar.TUESDAY,
    "miercoles" to Calendar.WEDNESDAY,
    "miércoles" to Calendar.WEDNESDAY,
    "jueves"    to Calendar.THURSDAY,
    "viernes"   to Calendar.FRIDAY,
    "sabado"    to Calendar.SATURDAY,
    "sábado"    to Calendar.SATURDAY,
    "domingo"   to Calendar.SUNDAY
)

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class SearchViewModel(
    private val noteRepository: NoteRepository,
    private val tagRepository:  TagRepository
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    // Todas las etiquetas únicas ordenadas por frecuencia, desde la DB
    val allTags: StateFlow<List<String>> = tagRepository.getAllTags()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val results: StateFlow<SearchResults> = _query
        .debounce(300)
        .flatMapLatest { q ->
            val trimmed = q.trim()
            when {
                trimmed.length < 2 -> flowOf(SearchResults())

                // Búsqueda explícita por #etiqueta — usa la DB directamente
                trimmed.startsWith("#") -> {
                    val tag = trimmed.removePrefix("#").lowercase()
                    if (tag.isEmpty()) flowOf(SearchResults()) else {
                        tagRepository.getNotesByTag(tag).map { notes ->
                            SearchResults(byTag = notes, activeTag = tag)
                        }
                    }
                }

                else -> {
                    val dayOfWeek = DAY_NAMES[trimmed.lowercase()]
                    val tagWord   = trimmed.lowercase()

                    // Combina: búsqueda texto + tag desde DB + (opcional) día de semana
                    val textFlow = noteRepository.searchNotes(trimmed)
                    val tagFlow  = tagRepository.getNotesByTag(tagWord)
                    val allFlow  = if (dayOfWeek != null) noteRepository.getAllNotes() else flowOf(emptyList())

                    combine(textFlow, tagFlow, allFlow) { textMatches, tagMatches, all ->
                        val textIds = textMatches.map { it.id }.toSet()
                        val tagIds  = tagMatches.map { it.id }.toSet()

                        val byTitle   = textMatches.filter { it.title.contains(trimmed, ignoreCase = true) }
                        val byContent = textMatches.filter { !it.title.contains(trimmed, ignoreCase = true) }
                        // Tags que no están ya en los resultados de texto
                        val byTagOnly = tagMatches.filter { it.id !in textIds }

                        val byDayOfWeek = if (dayOfWeek != null) {
                            all.filter { note ->
                                val cal = Calendar.getInstance().apply { timeInMillis = note.createdAt }
                                cal.get(Calendar.DAY_OF_WEEK) == dayOfWeek
                                    && note.id !in textIds
                                    && note.id !in tagIds
                            }
                        } else emptyList()

                        SearchResults(
                            byTitle     = byTitle,
                            byContent   = byContent,
                            byTag       = byTagOnly,
                            byDayOfWeek = byDayOfWeek,
                            activeTag   = if (byTagOnly.isNotEmpty()) tagWord else ""
                        )
                    }
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SearchResults())

    fun onQueryChange(value: String) { _query.value = value }
    fun onTagClick(tag: String)      { _query.value = "#$tag" }

    companion object {
        fun factory(
            noteRepository: NoteRepository,
            tagRepository:  TagRepository
        ): ViewModelProvider.Factory =
            viewModelFactory {
                initializer { SearchViewModel(noteRepository, tagRepository) }
            }
    }
}
