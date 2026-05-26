package com.tuapp.notasmd.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.tuapp.notasmd.NotasMdApp
import com.tuapp.notasmd.ui.screens.editor.EditorScreen
import com.tuapp.notasmd.ui.screens.notebooks.NotebooksScreen
import com.tuapp.notasmd.ui.screens.notes.NotesScreen
import com.tuapp.notasmd.ui.screens.sections.SectionsScreen
import com.tuapp.notasmd.ui.screens.settings.SettingsScreen
import com.tuapp.notasmd.viewmodel.EditorViewModel
import com.tuapp.notasmd.viewmodel.NotebookViewModel
import com.tuapp.notasmd.viewmodel.NoteViewModel
import com.tuapp.notasmd.viewmodel.SectionViewModel
import com.tuapp.notasmd.viewmodel.SettingsViewModel

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val app = LocalContext.current.applicationContext as NotasMdApp

    NavHost(
        navController    = navController,
        startDestination = Screen.Notebooks.route
    ) {

        composable(Screen.Notebooks.route) {
            val viewModel: NotebookViewModel = viewModel(
                factory = NotebookViewModel.factory(app.notebookRepository)
            )
            NotebooksScreen(
                viewModel            = viewModel,
                onNavigateToSections = { notebookId ->
                    navController.navigate(Screen.Sections.createRoute(notebookId))
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }

        composable(
            route     = Screen.Sections.route,
            arguments = listOf(navArgument("notebookId") { type = NavType.LongType })
        ) { backStackEntry ->
            val notebookId = backStackEntry.arguments?.getLong("notebookId") ?: return@composable
            val viewModel: SectionViewModel = viewModel(
                factory = SectionViewModel.factory(app.sectionRepository, notebookId)
            )
            SectionsScreen(
                viewModel         = viewModel,
                onNavigateToNotes = { sectionId ->
                    navController.navigate(Screen.Notes.createRoute(sectionId))
                },
                onNavigateBack    = { navController.popBackStack() }
            )
        }

        composable(
            route     = Screen.Notes.route,
            arguments = listOf(navArgument("sectionId") { type = NavType.LongType })
        ) { backStackEntry ->
            val sectionId = backStackEntry.arguments?.getLong("sectionId") ?: return@composable
            val viewModel: NoteViewModel = viewModel(
                factory = NoteViewModel.factory(app.noteRepository, sectionId)
            )
            NotesScreen(
                viewModel          = viewModel,
                sectionId          = sectionId,
                onNavigateToEditor = { sid, noteId ->
                    navController.navigate(Screen.Editor.createRoute(sid, noteId))
                },
                onNavigateBack     = { navController.popBackStack() }
            )
        }

        composable(
            route     = Screen.Editor.route,
            arguments = listOf(
                navArgument("sectionId") { type = NavType.LongType },
                navArgument("noteId")    { type = NavType.LongType; defaultValue = -1L }
            )
        ) { backStackEntry ->
            val sectionId = backStackEntry.arguments?.getLong("sectionId") ?: return@composable
            val noteId    = backStackEntry.arguments?.getLong("noteId") ?: -1L
            val viewModel: EditorViewModel = viewModel(
                factory = EditorViewModel.factory(app.noteRepository, sectionId, noteId)
            )
            EditorScreen(
                viewModel      = viewModel,
                onNavigateBack = { navController.popBackStack() }
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
    }
}