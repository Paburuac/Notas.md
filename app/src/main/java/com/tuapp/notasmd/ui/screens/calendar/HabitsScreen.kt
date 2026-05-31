package com.tuapp.notasmd.ui.screens.calendar

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
import com.tuapp.notasmd.data.local.entity.Habit
import com.tuapp.notasmd.ui.components.DeleteConfirmDialog
import com.tuapp.notasmd.ui.components.NameColorDialog
import com.tuapp.notasmd.viewmodel.CalendarViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitsScreen(
    viewModel: CalendarViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showCreateDialog by remember { mutableStateOf(false) }
    var habitToDelete    by remember { mutableStateOf<Habit?>(null) }
    var habitToEdit      by remember { mutableStateOf<Habit?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mis hábitos") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick        = { showCreateDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) { Icon(Icons.Default.Add, contentDescription = "Nuevo hábito") }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (uiState.habits.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Sin hábitos. Toca + para crear uno.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier       = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.habits, key = { it.id }) { habit ->
                    HabitManageCard(
                        habit    = habit,
                        onEdit   = { habitToEdit = habit },
                        onDelete = { habitToDelete = habit }
                    )
                }
            }
        }
    }

    if (showCreateDialog) {
        NameColorDialog(
            title     = "Nuevo hábito",
            onConfirm = { name, color -> viewModel.createHabit(name, color); showCreateDialog = false },
            onDismiss = { showCreateDialog = false }
        )
    }

    habitToEdit?.let { habit ->
        NameColorDialog(
            title        = "Editar hábito",
            initialName  = habit.name,
            initialColor = habit.color,
            onConfirm    = { name, color -> viewModel.updateHabit(habit, name, color); habitToEdit = null },
            onDismiss    = { habitToEdit = null }
        )
    }

    habitToDelete?.let { habit ->
        DeleteConfirmDialog(
            itemName  = habit.name,
            onConfirm = { viewModel.deleteHabit(habit); habitToDelete = null },
            onDismiss = { habitToDelete = null }
        )
    }
}

@Composable
private fun HabitManageCard(habit: Habit, onEdit: () -> Unit, onDelete: () -> Unit) {
    var showMenu by remember { mutableStateOf(false) }
    val accentColor = remember(habit.color) {
        try { Color(android.graphics.Color.parseColor(habit.color)) }
        catch (e: Exception) { Color.Gray }
    }

    Card(
        modifier  = Modifier.fillMaxWidth(),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier          = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.width(6.dp).fillMaxHeight().background(accentColor))
            Row(
                modifier          = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text     = habit.name,
                    modifier = Modifier.weight(1f),
                    style    = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = null)
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
}
