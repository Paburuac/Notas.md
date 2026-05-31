package com.tuapp.notasmd.ui.screens.checklist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tuapp.notasmd.data.local.entity.Task
import com.tuapp.notasmd.data.local.entity.TaskCategory
import com.tuapp.notasmd.ui.components.DeleteConfirmDialog
import com.tuapp.notasmd.ui.components.NameColorDialog
import com.tuapp.notasmd.viewmodel.ChecklistUiState
import com.tuapp.notasmd.viewmodel.ChecklistView
import com.tuapp.notasmd.viewmodel.ChecklistViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChecklistScreen(viewModel: ChecklistViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var fabExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Checklist") },
                actions = {
                    IconButton(onClick = { viewModel.toggleView() }) {
                        Icon(
                            imageVector = if (uiState.view == ChecklistView.LIST)
                                Icons.Default.ViewKanban else Icons.Default.ViewList,
                            contentDescription = "Cambiar vista"
                        )
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
                        onClick = { fabExpanded = false; viewModel.showCreateCategoryDialog() },
                        icon    = { Icon(Icons.Default.Label, null) },
                        text    = { Text("Nueva categoría") },
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                    ExtendedFloatingActionButton(
                        onClick = { fabExpanded = false; viewModel.showCreateTaskDialog() },
                        icon    = { Icon(Icons.Default.Add, null) },
                        text    = { Text("Nueva tarea") },
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                }
                FloatingActionButton(
                    onClick        = { fabExpanded = !fabExpanded },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(
                        if (fabExpanded) Icons.Default.Close else Icons.Default.Add,
                        contentDescription = null
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (uiState.view == ChecklistView.LIST) {
            ListView(uiState, padding, viewModel)
        } else {
            KanbanView(uiState, padding, viewModel)
        }
    }

    if (uiState.showCreateTaskDialog) {
        TaskDialog(
            categories = uiState.categories,
            onConfirm  = { title, catId, recurring, unit, amount ->
                viewModel.createTask(title, catId, recurring, unit, amount)
            },
            onDismiss  = { viewModel.hideCreateTaskDialog() }
        )
    }

    uiState.taskToEdit?.let { task ->
        TaskDialog(
            categories   = uiState.categories,
            initialTask  = task,
            onConfirm    = { title, catId, recurring, unit, amount ->
                viewModel.saveEditedTask(task.copy(title = title, categoryId = catId, isRecurring = recurring, recurUnit = unit, recurAmount = amount))
            },
            onDismiss    = { viewModel.hideEditDialog() }
        )
    }

    if (uiState.showCreateCategoryDialog) {
        NameColorDialog(
            title     = "Nueva categoría",
            onConfirm = { name, color -> viewModel.createCategory(name, color) },
            onDismiss = { viewModel.hideCreateCategoryDialog() }
        )
    }

    uiState.taskToDelete?.let { task ->
        DeleteConfirmDialog(
            itemName  = task.title,
            onConfirm = { viewModel.deleteTask(task) },
            onDismiss = { viewModel.hideDeleteDialog() }
        )
    }
}

@Composable
private fun ListView(
    uiState: ChecklistUiState,
    padding: PaddingValues,
    viewModel: ChecklistViewModel
) {
    if (uiState.tasks.isEmpty() && uiState.categories.isEmpty()) {
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            Text(
                "Sin tareas. Toca + para agregar una.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val grouped = uiState.tasks.groupBy { it.categoryId }
    val categoryMap = uiState.categories.associateBy { it.id }

    LazyColumn(
        modifier       = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Todas las categorías siempre, más "Sin categoría" si hay tareas sin asignar
        val allCategoryIds: List<Long?> = uiState.categories.map { it.id as Long? } +
            if (grouped.containsKey(null)) listOf(null) else emptyList()

        allCategoryIds.forEach { catId ->
            val category = catId?.let { categoryMap[it] }
            val tasks    = grouped[catId] ?: emptyList()

            item(key = "header_${catId ?: "null"}") {
                CategoryHeader(category)
            }
            items(tasks, key = { "task_${it.id}" }) { task ->
                TaskItem(
                    task       = task,
                    onToggle   = { viewModel.toggleComplete(task) },
                    onEdit     = { viewModel.showEditDialog(task) },
                    onDelete   = { viewModel.showDeleteDialog(task) }
                )
            }
        }
    }
}

@Composable
private fun KanbanView(
    uiState: ChecklistUiState,
    padding: PaddingValues,
    viewModel: ChecklistViewModel
) {
    val grouped     = uiState.tasks.groupBy { it.categoryId }
    val categoryMap = uiState.categories.associateBy { it.id }
    val allColumns: List<Long?> = uiState.categories.map { it.id as Long? } +
        if (grouped.containsKey(null)) listOf(null) else emptyList()

    LazyRow(
        modifier       = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(allColumns, key = { it ?: "null" }) { catId ->
            val category = catId?.let { categoryMap[it] }
            val tasks    = grouped[catId] ?: emptyList()
            KanbanColumn(
                category  = category,
                tasks     = tasks,
                onToggle  = { viewModel.toggleComplete(it) },
                onEdit    = { viewModel.showEditDialog(it) },
                onDelete  = { viewModel.showDeleteDialog(it) }
            )
        }
    }
}

@Composable
private fun KanbanColumn(
    category: TaskCategory?,
    tasks: List<Task>,
    onToggle: (Task) -> Unit,
    onEdit: (Task) -> Unit,
    onDelete: (Task) -> Unit
) {
    val accentColor = category?.let {
        try { Color(android.graphics.Color.parseColor(it.color)) }
        catch (e: Exception) { MaterialTheme.colorScheme.primary }
    } ?: MaterialTheme.colorScheme.outline

    Card(
        modifier  = Modifier.width(260.dp).fillMaxHeight(),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(accentColor.copy(alpha = 0.15f))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(12.dp).background(accentColor, shape = MaterialTheme.shapes.small))
                Spacer(Modifier.width(8.dp))
                Text(
                    text  = category?.name ?: "Sin categoría",
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text  = "${tasks.size}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                tasks.forEach { task ->
                    TaskItem(task = task, onToggle = { onToggle(task) }, onEdit = { onEdit(task) }, onDelete = { onDelete(task) })
                }
            }
        }
    }
}

@Composable
private fun CategoryHeader(category: TaskCategory?) {
    val color = category?.let {
        try { Color(android.graphics.Color.parseColor(it.color)) }
        catch (e: Exception) { MaterialTheme.colorScheme.primary }
    } ?: MaterialTheme.colorScheme.outline

    Row(
        modifier          = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(10.dp).background(color, shape = MaterialTheme.shapes.small))
        Spacer(Modifier.width(8.dp))
        Text(
            text  = category?.name ?: "Sin categoría",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun TaskItem(
    task: Task,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier  = Modifier.fillMaxWidth(),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            modifier          = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = task.isCompleted, onCheckedChange = { onToggle() })
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text            = task.title,
                    style           = MaterialTheme.typography.bodyMedium,
                    maxLines        = 2,
                    overflow        = TextOverflow.Ellipsis,
                    textDecoration  = if (task.isCompleted) TextDecoration.LineThrough else null,
                    color           = if (task.isCompleted)
                        MaterialTheme.colorScheme.onSurfaceVariant
                    else
                        MaterialTheme.colorScheme.onSurface
                )
                if (task.isRecurring) {
                    Text(
                        text  = "↺ Cada ${task.recurAmount} ${task.recurUnit.lowercase()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = null, modifier = Modifier.size(18.dp))
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text        = { Text("Editar") },
                        leadingIcon = { Icon(Icons.Default.Edit, null) },
                        onClick     = { showMenu = false; onEdit() }
                    )
                    DropdownMenuItem(
                        text        = { Text("Eliminar") },
                        leadingIcon = { Icon(Icons.Default.Delete, null) },
                        onClick     = { showMenu = false; onDelete() }
                    )
                }
            }
        }
    }
}

@Composable
private fun TaskDialog(
    categories: List<TaskCategory>,
    initialTask: Task? = null,
    onConfirm: (title: String, categoryId: Long?, recurring: Boolean, unit: String, amount: Int) -> Unit,
    onDismiss: () -> Unit
) {
    var title        by remember { mutableStateOf(initialTask?.title ?: "") }
    var selectedCat  by remember { mutableStateOf(initialTask?.categoryId) }
    var isRecurring  by remember { mutableStateOf(initialTask?.isRecurring ?: false) }
    var recurUnit    by remember { mutableStateOf(initialTask?.recurUnit ?: "DAYS") }
    var recurAmount  by remember { mutableStateOf(initialTask?.recurAmount?.toString() ?: "1") }
    var unitExpanded by remember { mutableStateOf(false) }

    val units = listOf("HOURS" to "Horas", "DAYS" to "Días", "WEEKS" to "Semanas", "MONTHS" to "Meses", "YEARS" to "Años")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialTask == null) "Nueva tarea" else "Editar tarea") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value         = title,
                    onValueChange = { title = it },
                    label         = { Text("Título") },
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth()
                )

                if (categories.isNotEmpty()) {
                    Text("Categoría", style = MaterialTheme.typography.labelMedium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            FilterChip(
                                selected = selectedCat == null,
                                onClick  = { selectedCat = null },
                                label    = { Text("Sin categoría") }
                            )
                        }
                        items(categories, key = { it.id }) { cat ->
                            FilterChip(
                                selected = selectedCat == cat.id,
                                onClick  = { selectedCat = cat.id },
                                label    = { Text(cat.name) }
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isRecurring, onCheckedChange = { isRecurring = it })
                    Text("Tarea recurrente")
                }

                if (isRecurring) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Text("Cada", style = MaterialTheme.typography.bodyMedium)
                        OutlinedTextField(
                            value         = recurAmount,
                            onValueChange = { if (it.all(Char::isDigit)) recurAmount = it },
                            modifier      = Modifier.width(72.dp),
                            singleLine    = true
                        )
                        Box {
                            OutlinedButton(onClick = { unitExpanded = true }) {
                                Text(units.first { it.first == recurUnit }.second)
                            }
                            DropdownMenu(expanded = unitExpanded, onDismissRequest = { unitExpanded = false }) {
                                units.forEach { (key, label) ->
                                    DropdownMenuItem(
                                        text    = { Text(label) },
                                        onClick = { recurUnit = key; unitExpanded = false }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (title.isNotBlank()) {
                        onConfirm(title.trim(), selectedCat, isRecurring, recurUnit, recurAmount.toIntOrNull() ?: 1)
                    }
                }
            ) { Text(if (initialTask == null) "Crear" else "Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
