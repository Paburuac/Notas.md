package com.tuapp.notasmd.ui.navigation

sealed class Screen(val route: String) {

    object Notebooks : Screen("notebooks")

    object Sections : Screen("sections/{notebookId}") {
        fun createRoute(notebookId: Long) = "sections/$notebookId"
    }

    object Notes : Screen("notes/{sectionId}") {
        fun createRoute(sectionId: Long) = "notes/$sectionId"
    }

    // noteId es opcional: -1 significa nota nueva
    object Editor : Screen("editor/{sectionId}?noteId={noteId}") {
        fun createRoute(sectionId: Long, noteId: Long = -1L) =
            "editor/$sectionId?noteId=$noteId"
    }

    object Settings : Screen("settings")
}