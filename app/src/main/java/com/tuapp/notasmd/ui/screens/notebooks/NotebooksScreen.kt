package com.tuapp.notasmd.ui.screens.notebooks

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tuapp.notasmd.NotasMdApp
import com.tuapp.notasmd.data.local.entity.Notebook
import com.tuapp.notasmd.ui.components.DeleteConfirmDialog
import com.tuapp.notasmd.ui.components.NameColorDialog
import com.tuapp.notasmd.ui.components.toFormattedDate
import com.tuapp.notasmd.viewmodel.NotebookViewModel
import com.tuapp.notasmd.viewmodel.PinnedNoteItem
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotebooksScreen(
    viewModel: NotebookViewModel,
    onNavigateToSections: (notebookId: Long) -> Unit,
    onNavigateToEditor: (sectionId: Long, noteId: Long) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToSearch: () -> Unit = {}
) {
    val uiState  by viewModel.uiState.collectAsStateWithLifecycle()
    val context  = LocalContext.current
    val app      = context.applicationContext as NotasMdApp
    val scope    = rememberCoroutineScope()
    var notebookToExport by remember { mutableStateOf<Notebook?>(null) }
    var exportError      by remember { mutableStateOf<String?>(null) }

    val exportAllLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            runCatching {
                context.contentResolver.openOutputStream(uri)!!.use { app.exportManager.exportAll(it) }
            }.onFailure { exportError = "Error al exportar: ${it.message}" }
        }
    }

    val exportNotebookLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        val notebook = notebookToExport ?: return@rememberLauncherForActivityResult
        scope.launch {
            runCatching {
                context.contentResolver.openOutputStream(uri)!!.use { app.exportManager.exportNotebook(notebook, it) }
            }.onFailure { exportError = "Error al exportar: ${it.message}" }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title  = { Text("Mis Cuadernos") },
                actions = {
                    IconButton(onClick = onNavigateToSearch) {
                        Icon(Icons.Default.Search, contentDescription = "Buscar")
                    }
                    IconButton(onClick = { exportAllLauncher.launch("notas_export.zip") }) {
                        Icon(Icons.Default.FileDownload, contentDescription = "Exportar todo")
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Ajustes")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick        = { viewModel.showCreateDialog() },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Nuevo cuaderno")
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->

        val hasPinned    = uiState.pinnedNotes.isNotEmpty()
        val hasNotebooks = uiState.notebooks.isNotEmpty()

        if (!hasNotebooks && !hasPinned) {
            Box(
                modifier          = Modifier.fillMaxSize().padding(padding),
                contentAlignment  = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text  = "Sin cuadernos todavía",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text  = "Toca + para crear el primero",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier        = Modifier.fillMaxSize().padding(padding),
                contentPadding  = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (hasPinned) {
                    item {
                        Text(
                            text     = "Fijadas",
                            style    = MaterialTheme.typography.labelLarge,
                            color    = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                    items(uiState.pinnedNotes, key = { "pinned_${it.note.id}" }) { item ->
                        PinnedNoteCard(
                            item    = item,
                            onClick = { onNavigateToEditor(item.note.sectionId, item.note.id) }
                        )
                    }
                    item { Spacer(Modifier.height(4.dp)) }
                }

                if (hasNotebooks) {
                    item {
                        Text(
                            text     = "Cuadernos",
                            style    = MaterialTheme.typography.labelLarge,
                            color    = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                    items(uiState.notebooks, key = { it.id }) { notebook ->
                        NotebookCard(
                            notebook = notebook,
                            onClick  = { onNavigateToSections(notebook.id) },
                            onEdit   = { viewModel.showEditDialog(notebook) },
                            onDelete = { viewModel.showDeleteDialog(notebook) },
                            onExport = {
                                notebookToExport = notebook
                                exportNotebookLauncher.launch("${notebook.name}.zip")
                            }
                        )
                    }
                }
            }
        }
    }

    exportError?.let { msg ->
        AlertDialog(
            onDismissRequest = { exportError = null },
            title            = { Text("Error") },
            text             = { Text(msg) },
            confirmButton    = { TextButton(onClick = { exportError = null }) { Text("OK") } }
        )
    }

    if (uiState.showCreateDialog) {
        NameColorDialog(
            title     = "Nuevo cuaderno",
            onConfirm = { name, color -> viewModel.createNotebook(name, color) },
            onDismiss = { viewModel.hideCreateDialog() }
        )
    }

    uiState.notebookToEdit?.let { notebook ->
        NameColorDialog(
            title        = "Editar cuaderno",
            initialName  = notebook.name,
            initialColor = notebook.color,
            onConfirm    = { name, color -> viewModel.updateNotebook(notebook, name, color) },
            onDismiss    = { viewModel.hideEditDialog() }
        )
    }

    uiState.notebookToDelete?.let { notebook ->
        DeleteConfirmDialog(
            itemName  = notebook.name,
            onConfirm = { viewModel.deleteNotebook(notebook) },
            onDismiss = { viewModel.hideDeleteDialog() }
        )
    }
}

@Composable
private fun PinnedNoteCard(
    item: PinnedNoteItem,
    onClick: () -> Unit
) {
    Card(
        onClick   = onClick,
        modifier  = Modifier.fillMaxWidth(),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier          = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector        = Icons.Default.PushPin,
                contentDescription = null,
                tint               = MaterialTheme.colorScheme.primary,
                modifier           = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text     = item.note.title.ifBlank { "Sin título" },
                    style    = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text     = "${item.notebookName} › ${item.sectionName}",
                    style    = MaterialTheme.typography.labelSmall,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun NotebookCard(
    notebook: Notebook,
    onClick:  () -> Unit,
    onEdit:   () -> Unit,
    onDelete: () -> Unit,
    onExport: () -> Unit = {}
) {
    var showMenu by remember { mutableStateOf(false) }
    val accentColor = MaterialTheme.colorScheme.primary

    Card(
        onClick   = onClick,
        modifier  = Modifier.fillMaxWidth(),
        colors    = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            Box(
                modifier = Modifier
                    .width(8.dp)
                    .fillMaxHeight()
                    .background(accentColor)
            )
            Row(
                modifier            = Modifier.fillMaxWidth().padding(12.dp),
                verticalAlignment   = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text     = notebook.name,
                        style    = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text  = "Modificado: ${notebook.updatedAt.toFormattedDate()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Opciones")
                    }
                    DropdownMenu(
                        expanded          = showMenu,
                        onDismissRequest  = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text          = { Text("Editar") },
                            leadingIcon   = { Icon(Icons.Default.Edit, contentDescription = null) },
                            onClick       = { showMenu = false; onEdit() }
                        )
                        DropdownMenuItem(
                            text          = { Text("Exportar") },
                            leadingIcon   = { Icon(Icons.Default.FileDownload, contentDescription = null) },
                            onClick       = { showMenu = false; onExport() }
                        )
                        DropdownMenuItem(
                            text          = { Text("Eliminar") },
                            leadingIcon   = { Icon(Icons.Default.Delete, contentDescription = null) },
                            onClick       = { showMenu = false; onDelete() }
                        )
                    }
                }
            }
        }
    }
}
