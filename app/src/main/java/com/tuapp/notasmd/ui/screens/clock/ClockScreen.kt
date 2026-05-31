package com.tuapp.notasmd.ui.screens.clock

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tuapp.notasmd.data.local.entity.Alarm
import com.tuapp.notasmd.viewmodel.ClockViewModel
import com.tuapp.notasmd.viewmodel.TimerState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClockScreen(viewModel: ClockViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Cronómetro", "Temporizador", "Alarma")

    Scaffold(
        topBar = {
            TopAppBar(
                title  = { Text("Reloj") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        floatingActionButton = {
            if (selectedTab == 2) {
                FloatingActionButton(
                    onClick        = { viewModel.showCreateAlarmDialog() },
                    containerColor = MaterialTheme.colorScheme.primary
                ) { Icon(Icons.Default.Add, contentDescription = "Nueva alarma") }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick  = { selectedTab = index },
                        text     = { Text(title) }
                    )
                }
            }

            when (selectedTab) {
                0 -> StopwatchTab(
                    elapsedMs  = uiState.stopwatch.elapsedMs,
                    isRunning  = uiState.stopwatch.isRunning,
                    onStart    = { viewModel.startStopwatch() },
                    onPause    = { viewModel.pauseStopwatch() },
                    onReset    = { viewModel.resetStopwatch() }
                )
                1 -> TimerTab(
                    state   = uiState.timer,
                    onSet   = { h, m, s -> viewModel.setTimer(h, m, s) },
                    onStart = { viewModel.startTimer() },
                    onPause = { viewModel.pauseTimer() },
                    onReset = { viewModel.resetTimer() }
                )
                2 -> AlarmTab(
                    alarms    = uiState.alarm.alarms,
                    onToggle  = { viewModel.toggleAlarm(it) },
                    onDelete  = { viewModel.deleteAlarm(it) }
                )
            }
        }
    }

    if (uiState.alarm.showCreateDialog) {
        AlarmDialog(
            onConfirm = { h, m, label, days -> viewModel.createAlarm(h, m, label, days) },
            onDismiss = { viewModel.hideCreateAlarmDialog() }
        )
    }
}

// ── Cronómetro ─────────────────────────────────────────────────────────────

@Composable
private fun StopwatchTab(
    elapsedMs: Long,
    isRunning: Boolean,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onReset: () -> Unit
) {
    Column(
        modifier            = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(32.dp, Alignment.CenterVertically)
    ) {
        Box(contentAlignment = Alignment.Center) {
            StopwatchDial(elapsedMs = elapsedMs, modifier = Modifier.size(200.dp))
            Text(
                text  = formatStopwatch(elapsedMs),
                style = MaterialTheme.typography.headlineMedium.copy(fontSize = 36.sp)
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            OutlinedButton(onClick = onReset) { Text("Reiniciar") }
            Button(onClick = if (isRunning) onPause else onStart) {
                Text(if (isRunning) "Pausar" else "Iniciar")
            }
        }
    }
}

@Composable
private fun StopwatchDial(elapsedMs: Long, modifier: Modifier = Modifier) {
    val lapColors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.error
    )
    val trackColor   = MaterialTheme.colorScheme.surfaceVariant
    val lapCount     = (elapsedMs / 60_000L).toInt()
    val rawProgress  = (elapsedMs % 60_000L) / 60_000f
    val currentColor = lapColors[lapCount % lapColors.size]
    val prevColor    = if (lapCount > 0) lapColors[(lapCount - 1) % lapColors.size] else null

    val animatedProgress by animateFloatAsState(
        targetValue   = rawProgress,
        animationSpec = tween(durationMillis = 50),
        label         = "sw_progress"
    )

    Canvas(modifier = modifier) {
        val stroke      = 8.dp.toPx()
        val diameter    = size.minDimension - stroke
        val topLeft     = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
        val arcSize     = Size(diameter, diameter)
        val startAngle  = -90f

        // Pista de fondo (gris) — solo visible si no hay vueltas completadas
        drawArc(
            color      = trackColor,
            startAngle = startAngle,
            sweepAngle = 360f,
            useCenter  = false,
            topLeft    = topLeft,
            size       = arcSize,
            style      = Stroke(width = stroke)
        )

        // Vuelta anterior completa como fondo sólido (sin alpha)
        prevColor?.let {
            drawArc(
                color      = it,
                startAngle = startAngle,
                sweepAngle = 360f,
                useCenter  = false,
                topLeft    = topLeft,
                size       = arcSize,
                style      = Stroke(width = stroke)
            )
        }

        // Vuelta actual dibujada encima
        if (animatedProgress > 0f) {
            drawArc(
                color      = currentColor,
                startAngle = startAngle,
                sweepAngle = animatedProgress * 360f,
                useCenter  = false,
                topLeft    = topLeft,
                size       = arcSize,
                style      = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }
    }
}

private fun formatStopwatch(ms: Long): String {
    val h  = ms / 3_600_000L
    val m  = (ms % 3_600_000L) / 60_000L
    val s  = (ms % 60_000L) / 1_000L
    val cs = (ms % 1_000L) / 10L
    return if (h > 0) "%d:%02d:%02d.%02d".format(h, m, s, cs)
    else "%02d:%02d.%02d".format(m, s, cs)
}

// ── Temporizador ───────────────────────────────────────────────────────────

@Composable
private fun TimerTab(
    state: TimerState,
    onSet: (Int, Int, Int) -> Unit,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onReset: () -> Unit
) {
    var inputHours   by remember { mutableStateOf("0") }
    var inputMinutes by remember { mutableStateOf("5") }
    var inputSeconds by remember { mutableStateOf("0") }

    val progress by animateFloatAsState(
        targetValue   = if (state.totalMs > 0) state.remainingMs / state.totalMs.toFloat() else 0f,
        animationSpec = tween(durationMillis = 50),
        label         = "timer_progress"
    )

    Column(
        modifier            = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically)
    ) {
        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                progress       = { progress },
                modifier       = Modifier.size(200.dp),
                strokeWidth    = 8.dp,
                trackColor     = MaterialTheme.colorScheme.surfaceVariant,
                color          = if (state.isFinished) MaterialTheme.colorScheme.error
                                 else MaterialTheme.colorScheme.primary
            )
            if (state.totalMs > 0L) {
                Text(
                    text  = formatTimer(state.remainingMs),
                    style = MaterialTheme.typography.headlineMedium.copy(fontSize = 36.sp)
                )
            } else {
                Text(
                    text  = "00:00",
                    style = MaterialTheme.typography.headlineMedium.copy(fontSize = 36.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (state.isFinished) {
            Text("¡Tiempo!", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.titleLarge)
        }

        if (!state.isRunning && state.totalMs == 0L) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment     = Alignment.CenterVertically
            ) {
                TimeInput("H", inputHours)   { inputHours = it }
                Text(":", style = MaterialTheme.typography.headlineSmall)
                TimeInput("M", inputMinutes) { inputMinutes = it }
                Text(":", style = MaterialTheme.typography.headlineSmall)
                TimeInput("S", inputSeconds) { inputSeconds = it }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            OutlinedButton(onClick = {
                onReset()
                inputHours = "0"; inputMinutes = "5"; inputSeconds = "0"
            }) { Text("Reiniciar") }

            if (state.totalMs == 0L) {
                Button(onClick = {
                    onSet(
                        inputHours.toIntOrNull() ?: 0,
                        inputMinutes.toIntOrNull() ?: 0,
                        inputSeconds.toIntOrNull() ?: 0
                    )
                }) { Text("Configurar") }
            } else {
                Button(onClick = if (state.isRunning) onPause else onStart) {
                    Text(if (state.isRunning) "Pausar" else "Iniciar")
                }
            }
        }
    }
}

@Composable
private fun TimeInput(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value         = value,
        onValueChange = { if (it.length <= 2 && it.all(Char::isDigit)) onChange(it) },
        label         = { Text(label) },
        singleLine    = true,
        modifier      = Modifier.width(64.dp)
    )
}

private fun formatTimer(ms: Long): String {
    val h = ms / 3_600_000L
    val m = (ms % 3_600_000L) / 60_000L
    val s = (ms % 60_000L) / 1_000L
    return if (h > 0) "%d:%02d:%02d".format(h, m, s)
    else "%02d:%02d".format(m, s)
}

// ── Alarmas ────────────────────────────────────────────────────────────────

@Composable
private fun AlarmTab(
    alarms: List<Alarm>,
    onToggle: (Alarm) -> Unit,
    onDelete: (Alarm) -> Unit
) {
    if (alarms.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Sin alarmas. Toca + para agregar una.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    LazyColumn(
        modifier       = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(alarms, key = { it.id }) { alarm ->
            AlarmCard(alarm = alarm, onToggle = { onToggle(alarm) }, onDelete = { onDelete(alarm) })
        }
    }
}

@Composable
private fun AlarmCard(alarm: Alarm, onToggle: () -> Unit, onDelete: () -> Unit) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier  = Modifier.fillMaxWidth(),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier          = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text  = "%02d:%02d".format(alarm.hour, alarm.minute),
                    style = MaterialTheme.typography.headlineSmall
                )
                if (alarm.label.isNotBlank()) {
                    Text(alarm.label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (alarm.repeatDays.isNotEmpty()) {
                    Text(formatRepeatDays(alarm.repeatDays), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
            }
            Switch(checked = alarm.isEnabled, onCheckedChange = { onToggle() })
            Box {
                IconButton(onClick = { showMenu = true }) { Icon(Icons.Default.MoreVert, null) }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
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

private fun formatRepeatDays(days: String): String {
    val names = mapOf(1 to "Lun", 2 to "Mar", 3 to "Mié", 4 to "Jue", 5 to "Vie", 6 to "Sáb", 7 to "Dom")
    return days.split(",").mapNotNull { it.toIntOrNull()?.let { d -> names[d] } }.joinToString(", ")
}

@Composable
private fun AlarmDialog(
    onConfirm: (hour: Int, minute: Int, label: String, repeatDays: String) -> Unit,
    onDismiss: () -> Unit
) {
    var hour     by remember { mutableStateOf("08") }
    var minute   by remember { mutableStateOf("00") }
    var label    by remember { mutableStateOf("") }
    val dayNames = listOf("Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom")
    val selected = remember { mutableStateListOf<Int>() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nueva alarma") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value         = hour,
                        onValueChange = { if (it.length <= 2 && it.all(Char::isDigit)) hour = it },
                        label         = { Text("HH") },
                        singleLine    = true,
                        modifier      = Modifier.width(72.dp)
                    )
                    Text(":", style = MaterialTheme.typography.headlineSmall)
                    OutlinedTextField(
                        value         = minute,
                        onValueChange = { if (it.length <= 2 && it.all(Char::isDigit)) minute = it },
                        label         = { Text("MM") },
                        singleLine    = true,
                        modifier      = Modifier.width(72.dp)
                    )
                }
                OutlinedTextField(
                    value         = label,
                    onValueChange = { label = it },
                    label         = { Text("Etiqueta (opcional)") },
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth()
                )
                Text("Repetir", style = MaterialTheme.typography.labelMedium)
                @OptIn(ExperimentalLayoutApi::class)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    dayNames.forEachIndexed { index, name ->
                        val day = index + 1
                        FilterChip(
                            selected = day in selected,
                            onClick  = { if (day in selected) selected.remove(day) else selected.add(day) },
                            label    = { Text(name, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val h = hour.toIntOrNull()?.coerceIn(0, 23) ?: 8
                val m = minute.toIntOrNull()?.coerceIn(0, 59) ?: 0
                val repeatDays = selected.sorted().joinToString(",")
                onConfirm(h, m, label.trim(), repeatDays)
            }) { Text("Crear") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
