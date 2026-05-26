package com.tuapp.notasmd.ui.screens.notebooks

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tuapp.notasmd.data.local.entity.Notebook
import com.tuapp.notasmd.ui.components.DeleteConfirmDialog
import com.tuapp.notasmd.ui.components.NameColorDialog
import com.tuapp.notasmd.ui.components.toFormattedDate
import com.tuapp.notasmd.viewmodel.NotebookViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotebooksScreen(
    viewModel: NotebookViewModel,
    onNavigateToSections: (notebookId: Long) -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title  = { Text("Mis Cuadernos") },
                actions = {
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

        if (uiState.notebooks.isEmpty()) {
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
                items(uiState.notebooks, key = { it.id }) { notebook ->
                    NotebookCard(
                        notebook = notebook,
                        onClick  = { onNavigateToSections(notebook.id) },
                        onEdit   = { viewModel.showEditDialog(notebook) },
                        onDelete = { viewModel.showDeleteDialog(notebook) }
                    )
                }
            }
        }
    }

    // Diálogo crear
    if (uiState.showCreateDialog) {
        NameColorDialog(
            title     = "Nuevo cuaderno",
            onConfirm = { name, color -> viewModel.createNotebook(name, color) },
            onDismiss = { viewModel.hideCreateDialog() }
        )
    }

    // Diálogo editar
    uiState.notebookToEdit?.let { notebook ->
        NameColorDialog(
            title        = "Editar cuaderno",
            initialName  = notebook.name,
            initialColor = notebook.color,
            onConfirm    = { name, color -> viewModel.updateNotebook(notebook, name, color) },
            onDismiss    = { viewModel.hideEditDialog() }
        )
    }

    // Diálogo eliminar
    uiState.notebookToDelete?.let { notebook ->
        DeleteConfirmDialog(
            itemName  = notebook.name,
            onConfirm = { viewModel.deleteNotebook(notebook) },
            onDismiss = { viewModel.hideDeleteDialog() }
        )
    }
}

@Composable
private fun NotebookCard(
    notebook: Notebook,
    onClick:  () -> Unit,
    onEdit:   () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val accentColor = remember(notebook.color) {
        try { Color(android.graphics.Color.parseColor(notebook.color)) }
        catch (e: Exception) { Color(0xFF8B6914.toInt()) }
    }

    Card(
        onClick   = onClick,
        modifier  = Modifier.fillMaxWidth(),
        colors    = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            // Franja de color lateral
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