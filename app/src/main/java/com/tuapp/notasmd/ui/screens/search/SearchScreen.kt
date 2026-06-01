package com.tuapp.notasmd.ui.screens.search

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tuapp.notasmd.data.local.entity.Note
import com.tuapp.notasmd.ui.components.toFormattedDate
import com.tuapp.notasmd.viewmodel.SearchResults
import com.tuapp.notasmd.viewmodel.SearchViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onNavigateToNote: (sectionId: Long, noteId: Long) -> Unit,
    onNavigateBack: () -> Unit
) {
    val query          by viewModel.query.collectAsStateWithLifecycle()
    val results        by viewModel.results.collectAsStateWithLifecycle()
    val allTags        by viewModel.allTags.collectAsStateWithLifecycle()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                title = {
                    TextField(
                        value         = query,
                        onValueChange = viewModel::onQueryChange,
                        placeholder   = { Text("Buscar notas o #etiqueta...") },
                        singleLine    = true,
                        modifier      = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor   = MaterialTheme.colorScheme.background,
                            unfocusedContainerColor = MaterialTheme.colorScheme.background,
                            focusedIndicatorColor   = MaterialTheme.colorScheme.primary,
                            unfocusedIndicatorColor = MaterialTheme.colorScheme.outline
                        ),
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null)
                        },
                        trailingIcon = {
                            if (query.isNotEmpty()) {
                                IconButton(onClick = { viewModel.onQueryChange("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Limpiar")
                                }
                            }
                        }
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        when {
            query.trim().length < 2 -> {
                TagBrowser(
                    tags      = allTags,
                    modifier  = Modifier.fillMaxSize().padding(padding),
                    onTagClick = viewModel::onTagClick
                )
            }
            results.isEmpty -> {
                Box(
                    modifier         = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Sin resultados para \"$query\"",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            else -> {
                SearchResultsList(
                    results  = results,
                    modifier = Modifier.fillMaxSize().padding(padding),
                    onClick  = { note -> onNavigateToNote(note.sectionId, note.id) }
                )
            }
        }
    }
}

@Composable
private fun TagBrowser(
    tags: List<String>,
    modifier: Modifier,
    onTagClick: (String) -> Unit
) {
    if (tags.isEmpty()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text(
                "Aún no hay etiquetas. Usá #etiqueta en tus notas.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    LazyColumn(
        modifier            = modifier,
        contentPadding      = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector        = Icons.Default.Tag,
                    contentDescription = null,
                    tint               = MaterialTheme.colorScheme.primary,
                    modifier           = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text  = "Etiquetas en tus notas",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        // Chips en filas horizontales scrolleables de a ~4
        val rows = tags.chunked(4)
        items(rows) { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                row.forEach { tag ->
                    SuggestionChip(
                        onClick = { onTagClick(tag) },
                        label   = { Text("#$tag") }
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchResultsList(
    results: SearchResults,
    modifier: Modifier,
    onClick: (Note) -> Unit
) {
    val listState = rememberLazyListState()

    LazyColumn(
        state               = listState,
        modifier            = modifier,
        contentPadding      = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (results.byTag.isNotEmpty() && results.byTitle.isEmpty() && results.byContent.isEmpty()) {
            // Búsqueda exclusiva por #tag
            item {
                SectionHeader("Etiqueta #${results.activeTag} (${results.byTag.size})")
            }
            items(results.byTag, key = { "tag_${it.id}" }) { note ->
                NoteResultCard(note = note, onClick = { onClick(note) })
            }
        } else {
            if (results.byTitle.isNotEmpty()) {
                item { SectionHeader("En el título (${results.byTitle.size})") }
                items(results.byTitle, key = { "title_${it.id}" }) { note ->
                    NoteResultCard(note = note, onClick = { onClick(note) })
                }
            }
            if (results.byContent.isNotEmpty()) {
                item {
                    SectionHeader(
                        "En el contenido (${results.byContent.size})",
                        topPadding = results.byTitle.isNotEmpty()
                    )
                }
                items(results.byContent, key = { "content_${it.id}" }) { note ->
                    NoteResultCard(note = note, onClick = { onClick(note) })
                }
            }
            if (results.byTag.isNotEmpty()) {
                item {
                    SectionHeader(
                        "Por etiqueta #${results.activeTag} (${results.byTag.size})",
                        topPadding = results.byTitle.isNotEmpty() || results.byContent.isNotEmpty()
                    )
                }
                items(results.byTag, key = { "tag_${it.id}" }) { note ->
                    NoteResultCard(note = note, onClick = { onClick(note) })
                }
            }
            if (results.byDayOfWeek.isNotEmpty()) {
                item {
                    SectionHeader(
                        "Escritas ese día de la semana (${results.byDayOfWeek.size})",
                        topPadding = results.byTitle.isNotEmpty() || results.byContent.isNotEmpty() || results.byTag.isNotEmpty()
                    )
                }
                items(results.byDayOfWeek, key = { "day_${it.id}" }) { note ->
                    NoteResultCard(note = note, onClick = { onClick(note) })
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String, topPadding: Boolean = false) {
    Text(
        text     = text,
        style    = MaterialTheme.typography.labelLarge,
        color    = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = if (topPadding) 12.dp else 0.dp, bottom = 4.dp)
    )
}

@Composable
private fun NoteResultCard(note: Note, onClick: () -> Unit) {
    Card(
        onClick   = onClick,
        modifier  = Modifier.fillMaxWidth(),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text     = note.title.ifBlank { "Sin título" },
                style    = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (note.contentMarkdown.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text     = note.contentMarkdown,
                    style    = MaterialTheme.typography.bodyMedium,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text  = "Creada: ${note.createdAt.toFormattedDate()}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
