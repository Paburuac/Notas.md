package com.tuapp.notasmd.ui.screens.notes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tuapp.notasmd.data.local.entity.Note
import com.tuapp.notasmd.data.local.entity.Section
import com.tuapp.notasmd.ui.components.DeleteConfirmDialog
import com.tuapp.notasmd.ui.components.NameColorDialog
import com.tuapp.notasmd.ui.components.toFormattedDate
import com.tuapp.notasmd.viewmodel.NoteViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(
    viewModel: NoteViewModel,
    sectionId: Long,
    onNavigateToEditor: (sectionId: Long, noteId: Long) -> Unit,
    onNavigateToSubSection: (sectionId: Long) -> Unit,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var fabExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notas") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (fabExpanded) {
                    ExtendedFloatingActionButton(
                        onClick = {
                            fabExpanded = false
                            viewModel.showCreateSubSectionDialog()
                        },
                        icon = { Icon(Icons.Default.CreateNewFolder, contentDescription = null) },
                        text = { Text("Nueva sección") },
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                    ExtendedFloatingActionButton(
                        onClick = {
                            fabExpanded = false
                            onNavigateToEditor(sectionId, -1L)
                        },
                        icon = { Icon(Icons.Default.Edit, contentDescription = null) },
                        text = { Text("Nueva nota") },
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                }
                FloatingActionButton(
                    onClick        = { fabExpanded = !fabExpanded },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(
                        if (fabExpanded) Icons.Default.Close else Icons.Default.Add,
                        contentDescription = if (fabExpanded) "Cerrar" else "Nuevo"
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->

        val isEmpty = uiState.subSections.isEmpty() && uiState.notes.isEmpty()

        if (isEmpty) {
            Box(
                modifier         = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text  = "Sección vacía",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text  = "Toca + para agregar una nota o sub-sección",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier       = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (uiState.subSections.isNotEmpty()) {
                    item {
                        Text(
                            text  = "Sub-secciones",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                    items(uiState.subSections, key = { "sec_${it.id}" }) { section ->
                        SubSectionCard(
                            section  = section,
                            onClick  = { onNavigateToSubSection(section.id) }
                        )
                    }
                    if (uiState.notes.isNotEmpty()) {
                        item {
                            Text(
                                text     = "Notas",
                                style    = MaterialTheme.typography.labelLarge,
                                color    = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                            )
                        }
                    }
                }
                items(uiState.notes, key = { "note_${it.id}" }) { note ->
                    NoteCard(
                        note     = note,
                        onClick  = { onNavigateToEditor(sectionId, note.id) },
                        onDelete = { viewModel.showDeleteDialog(note) }
                    )
                }
            }
        }
    }

    if (uiState.showCreateSubSectionDialog) {
        NameColorDialog(
            title     = "Nueva sub-sección",
            onConfirm = { name, color -> viewModel.createSubSection(name, color) },
            onDismiss = { viewModel.hideCreateSubSectionDialog() }
        )
    }

    uiState.noteToDelete?.let { note ->
        DeleteConfirmDialog(
            itemName  = note.title.ifBlank { "Nota sin título" },
            onConfirm = { viewModel.deleteNote(note) },
            onDismiss = { viewModel.hideDeleteDialog() }
        )
    }
}

@Composable
private fun SubSectionCard(
    section: Section,
    onClick: () -> Unit
) {
    val accentColor = remember(section.color) {
        try { Color(android.graphics.Color.parseColor(section.color)) }
        catch (e: Exception) { Color(0xFF8B6914.toInt()) }
    }

    Card(
        onClick   = onClick,
        modifier  = Modifier.fillMaxWidth(),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .background(accentColor)
            )
            Row(
                modifier          = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector        = Icons.Default.Folder,
                    contentDescription = null,
                    tint               = accentColor,
                    modifier           = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text     = section.name,
                    style    = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun NoteCard(
    note:     Note,
    onClick:  () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        onClick   = onClick,
        modifier  = Modifier.fillMaxWidth(),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier          = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
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
                    text  = "Modificado: ${note.updatedAt.toFormattedDate()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Opciones")
                }
                DropdownMenu(
                    expanded         = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text        = { Text("Eliminar") },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                        onClick     = { showMenu = false; onDelete() }
                    )
                }
            }
        }
    }
}
