package com.tuapp.notasmd.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.tuapp.notasmd.NotasMdApp
import com.tuapp.notasmd.ui.screens.calendar.CalendarScreen
import com.tuapp.notasmd.ui.screens.calendar.HabitsScreen
import com.tuapp.notasmd.ui.screens.checklist.ChecklistScreen
import com.tuapp.notasmd.ui.screens.clock.ClockScreen
import com.tuapp.notasmd.ui.screens.editor.EditorScreen
import com.tuapp.notasmd.ui.screens.notebooks.NotebooksScreen
import com.tuapp.notasmd.ui.screens.search.SearchScreen
import com.tuapp.notasmd.ui.screens.notes.NotesScreen
import com.tuapp.notasmd.ui.screens.sections.SectionsScreen
import com.tuapp.notasmd.ui.screens.settings.SettingsScreen
import com.tuapp.notasmd.viewmodel.ChecklistViewModel
import com.tuapp.notasmd.viewmodel.CalendarViewModel
import com.tuapp.notasmd.viewmodel.SearchViewModel
import com.tuapp.notasmd.viewmodel.ClockViewModel
import com.tuapp.notasmd.viewmodel.EditorViewModel
import com.tuapp.notasmd.viewmodel.NotebookViewModel
import com.tuapp.notasmd.viewmodel.NoteViewModel
import com.tuapp.notasmd.viewmodel.SectionViewModel
import com.tuapp.notasmd.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch

private data class BottomTab(
    val screen: Screen,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

private val tabs = listOf(
    BottomTab(Screen.Notebooks, "Notas",      Icons.Default.MenuBook),
    BottomTab(Screen.Checklist, "Checklist",  Icons.Default.CheckBox),
    BottomTab(Screen.Clock,     "Reloj",      Icons.Default.Schedule),
    BottomTab(Screen.Calendar,  "Calendario", Icons.Default.CalendarMonth)
)

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val app           = LocalContext.current.applicationContext as NotasMdApp
    val scope         = rememberCoroutineScope()
    val backStackEntry by navController.currentBackStackEntryAsState()

    Column(modifier = androidx.compose.ui.Modifier.fillMaxSize()) {
        NavHost(
            modifier         = androidx.compose.ui.Modifier.weight(1f),
            navController    = navController,
            startDestination = Screen.Notebooks.route
        ) {

            composable(Screen.Notebooks.route) {
                val viewModel: NotebookViewModel = viewModel(
                    factory = NotebookViewModel.factory(
                        repository        = app.notebookRepository,
                        noteRepository    = app.noteRepository,
                        sectionRepository = app.sectionRepository
                    )
                )
                NotebooksScreen(
                    viewModel            = viewModel,
                    onNavigateToSections = { notebookId ->
                        navController.navigate(Screen.Sections.createRoute(notebookId))
                    },
                    onNavigateToEditor   = { sectionId, noteId ->
                        navController.navigate(Screen.Editor.createRoute(sectionId, noteId))
                    },
                    onNavigateToSettings = {
                        navController.navigate(Screen.Settings.route)
                    },
                    onNavigateToSearch = {
                        navController.navigate(Screen.Search.route)
                    }
                )
            }

            composable(
                route     = Screen.Sections.route,
                arguments = listOf(navArgument("notebookId") { type = NavType.LongType })
            ) { backStack ->
                val notebookId = backStack.arguments?.getLong("notebookId") ?: return@composable
                val viewModel: SectionViewModel = viewModel(
                    factory = SectionViewModel.factory(app.sectionRepository, notebookId)
                )
                SectionsScreen(
                    viewModel         = viewModel,
                    onNavigateToNotes = { navController.navigate(Screen.Notes.createRoute(it)) },
                    onNavigateBack    = { navController.popBackStack() }
                )
            }

            composable(
                route     = Screen.Notes.route,
                arguments = listOf(navArgument("sectionId") { type = NavType.LongType })
            ) { backStack ->
                val sectionId = backStack.arguments?.getLong("sectionId") ?: return@composable
                val viewModel: NoteViewModel = viewModel(
                    factory = NoteViewModel.factory(
                        noteRepository     = app.noteRepository,
                        sectionRepository  = app.sectionRepository,
                        notebookRepository = app.notebookRepository,
                        sectionId          = sectionId
                    )
                )
                NotesScreen(
                    viewModel              = viewModel,
                    sectionId              = sectionId,
                    onNavigateToEditor     = { sid, noteId ->
                        navController.navigate(Screen.Editor.createRoute(sid, noteId))
                    },
                    onNavigateToSubSection = { navController.navigate(Screen.Notes.createRoute(it)) },
                    onNavigateBack         = { navController.popBackStack() }
                )
            }

            composable(
                route     = Screen.Editor.route,
                arguments = listOf(
                    navArgument("sectionId") { type = NavType.LongType },
                    navArgument("noteId")    { type = NavType.LongType; defaultValue = -1L }
                )
            ) { backStack ->
                val sectionId = backStack.arguments?.getLong("sectionId") ?: return@composable
                val noteId    = backStack.arguments?.getLong("noteId") ?: -1L
                val viewModel: EditorViewModel = viewModel(
                    factory = EditorViewModel.factory(app.noteRepository, app.tagRepository, sectionId, noteId)
                )
                EditorScreen(
                    viewModel        = viewModel,
                    onNavigateBack   = { navController.popBackStack() },
                    onNavigateToNote = { _, noteId ->
                        scope.launch {
                            val note = app.noteRepository.getNoteById(noteId) ?: return@launch
                            navController.navigate(Screen.Editor.createRoute(note.sectionId, noteId))
                        }
                    }
                )
            }

            composable(Screen.Settings.route) {
                val viewModel: SettingsViewModel = viewModel(
                    factory = SettingsViewModel.factory(app.themeRepository)
                )
                SettingsScreen(
                    viewModel      = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Checklist.route) {
                val viewModel: ChecklistViewModel = viewModel(
                    factory = ChecklistViewModel.factory(
                        taskRepository     = app.taskRepository,
                        categoryRepository = app.taskCategoryRepository,
                        context            = app
                    )
                )
                ChecklistScreen(viewModel = viewModel)
            }

            composable(Screen.Clock.route) {
                val viewModel: ClockViewModel = viewModel(
                    factory = ClockViewModel.factory(app.alarmRepository, app)
                )
                ClockScreen(viewModel = viewModel)
            }

            composable(Screen.Calendar.route) {
                val viewModel: CalendarViewModel = viewModel(
                    factory = CalendarViewModel.factory(app.habitRepository)
                )
                CalendarScreen(
                    viewModel           = viewModel,
                    onNavigateToHabits  = { navController.navigate(Screen.Habits.route) }
                )
            }

            composable(Screen.Search.route) {
                val viewModel: SearchViewModel = viewModel(
                    factory = SearchViewModel.factory(app.noteRepository, app.tagRepository)
                )
                SearchScreen(
                    viewModel        = viewModel,
                    onNavigateToNote = { sectionId, noteId ->
                        navController.navigate(Screen.Editor.createRoute(sectionId, noteId))
                    },
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Habits.route) {
                val viewModel: CalendarViewModel = viewModel(
                    factory = CalendarViewModel.factory(app.habitRepository)
                )
                HabitsScreen(
                    viewModel      = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }

        NavigationBar {
            tabs.forEach { tab ->
                val selected = backStackEntry?.destination?.hierarchy
                    ?.any { it.route == tab.screen.route } == true
                NavigationBarItem(
                    selected = selected,
                    onClick  = {
                        navController.navigate(tab.screen.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState    = true
                        }
                    },
                    icon  = { Icon(tab.icon, contentDescription = tab.label) },
                    label = { Text(tab.label) }
                )
            }
        }
    }
}
