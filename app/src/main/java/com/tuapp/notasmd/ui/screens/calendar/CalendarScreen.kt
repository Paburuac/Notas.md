package com.tuapp.notasmd.ui.screens.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tuapp.notasmd.data.local.entity.Habit
import com.tuapp.notasmd.viewmodel.CalendarViewModel
import com.tuapp.notasmd.viewmodel.HabitStreak
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val DAY_HEADERS = listOf("Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom")
private val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel,
    onNavigateToHabits: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val today = LocalDate.now()

    Scaffold(
        topBar = {
            TopAppBar(
                title  = { Text("Calendario") },
                actions = {
                    IconButton(onClick = onNavigateToHabits) {
                        Icon(Icons.Default.Settings, contentDescription = "Gestionar hábitos")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier       = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                MonthHeader(
                    month      = uiState.currentMonth,
                    onPrevious = { viewModel.goToPreviousMonth() },
                    onNext     = { viewModel.goToNextMonth() }
                )
            }
            item { DayHeaders() }
            item {
                MonthGrid(
                    month        = uiState.currentMonth,
                    today        = today,
                    entriesByDate = uiState.entriesByDate,
                    habits        = uiState.habits,
                    onDayClick    = { viewModel.selectDate(it) }
                )
            }
            if (uiState.streaks.isNotEmpty()) {
                item {
                    Text(
                        text     = "Rachas",
                        style    = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                items(uiState.streaks, key = { it.habit.id }) { streakItem ->
                    StreakCard(streakItem)
                }
                item { Spacer(Modifier.height(8.dp)) }
            }
        }
    }

    uiState.selectedDate?.let { date ->
        DayDialog(
            date       = date,
            habits     = uiState.habits,
            checkedIds = uiState.selectedDateEntries,
            onToggle   = { viewModel.toggleHabitOnDate(it) },
            onDismiss  = { viewModel.dismissDateDialog() }
        )
    }
}

@Composable
private fun MonthHeader(month: YearMonth, onPrevious: () -> Unit, onNext: () -> Unit) {
    Row(
        modifier          = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = onPrevious) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Mes anterior")
        }
        Text(
            text  = month.month.getDisplayName(TextStyle.FULL, Locale("es"))
                .replaceFirstChar { it.uppercase() } + " ${month.year}",
            style = MaterialTheme.typography.titleLarge
        )
        IconButton(onClick = onNext) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Mes siguiente")
        }
    }
}

@Composable
private fun DayHeaders() {
    Row(modifier = Modifier.fillMaxWidth()) {
        DAY_HEADERS.forEach { day ->
            Text(
                text      = day,
                modifier  = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style     = MaterialTheme.typography.labelMedium,
                color     = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MonthGrid(
    month: YearMonth,
    today: LocalDate,
    entriesByDate: Map<String, List<Any>>,
    habits: List<Habit>,
    onDayClick: (LocalDate) -> Unit
) {
    val habitColors = habits.associate { habit ->
        habit.id to try { Color(android.graphics.Color.parseColor(habit.color)) }
        catch (e: Exception) { Color.Gray }
    }
    val days = buildCalendarDays(month)

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        days.chunked(7).forEach { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                week.forEach { (date, inCurrentMonth) ->
                    val dateStr    = date.format(fmt)
                    val dayEntries = entriesByDate[dateStr] ?: emptyList()
                    val dotColors  = dayEntries
                        .mapNotNull { entry ->
                            (entry as? com.tuapp.notasmd.data.local.entity.HabitEntry)
                                ?.let { habitColors[it.habitId] }
                        }
                        .distinct()
                        .take(4)

                    DayCell(
                        date           = date,
                        inCurrentMonth = inCurrentMonth,
                        isToday        = date == today,
                        dotColors      = dotColors,
                        modifier       = Modifier.weight(1f),
                        onClick        = { onDayClick(date) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    inCurrentMonth: Boolean,
    isToday: Boolean,
    dotColors: List<Color>,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val textColor = when {
        isToday        -> MaterialTheme.colorScheme.onPrimary
        inCurrentMonth -> MaterialTheme.colorScheme.onBackground
        else           -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
    }

    Column(
        modifier          = modifier
            .aspectRatio(1f)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (isToday) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
            Text(
                text      = date.dayOfMonth.toString(),
                color     = textColor,
                style     = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }
        if (dotColors.isNotEmpty()) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.padding(top = 2.dp)
            ) {
                dotColors.forEach { color ->
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(color)
                    )
                }
            }
        }
    }
}

@Composable
private fun StreakCard(item: HabitStreak) {
    val accentColor = remember(item.habit.color) {
        try { Color(android.graphics.Color.parseColor(item.habit.color)) }
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
                    text     = item.habit.name,
                    style    = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text  = if (item.streak > 0) "🔥 ${item.streak} día${if (item.streak != 1) "s" else ""}" else "Sin racha",
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 13.sp),
                    color = if (item.streak > 0) accentColor else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun DayDialog(
    date: LocalDate,
    habits: List<Habit>,
    checkedIds: Set<Long>,
    onToggle: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val dayLabel = "${date.dayOfMonth} de ${
        date.month.getDisplayName(TextStyle.FULL, Locale("es")).replaceFirstChar { it.uppercase() }
    }"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(dayLabel) },
        text = {
            if (habits.isEmpty()) {
                Text(
                    "No hay hábitos creados. Créalos desde el icono de ajustes.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    habits.forEach { habit ->
                        Row(
                            modifier          = Modifier
                                .fillMaxWidth()
                                .clickable { onToggle(habit.id) }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked         = habit.id in checkedIds,
                                onCheckedChange = { onToggle(habit.id) }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(habit.name, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Listo") }
        }
    )
}

private fun buildCalendarDays(month: YearMonth): List<Pair<LocalDate, Boolean>> {
    val firstDay    = month.atDay(1)
    val startOffset = (firstDay.dayOfWeek.value - DayOfWeek.MONDAY.value + 7) % 7
    return (0 until 42).map { i ->
        val date           = firstDay.minusDays(startOffset.toLong()).plusDays(i.toLong())
        val inCurrentMonth = date.month == month.month && date.year == month.year
        date to inCurrentMonth
    }
}
