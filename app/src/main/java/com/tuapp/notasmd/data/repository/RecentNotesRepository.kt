package com.tuapp.notasmd.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.recentNotesDataStore by preferencesDataStore(name = "recent_notes")

class RecentNotesRepository(private val context: Context) {

    private val KEY = stringPreferencesKey("recent_ids")
    private val maxRecents = 10

    val recentNoteIds: Flow<List<Long>> = context.recentNotesDataStore.data.map { prefs ->
        prefs[KEY]?.split(",")?.mapNotNull { it.toLongOrNull() } ?: emptyList()
    }

    suspend fun registerOpen(noteId: Long) {
        context.recentNotesDataStore.edit { prefs ->
            val current = prefs[KEY]?.split(",")?.mapNotNull { it.toLongOrNull() }?.toMutableList()
                ?: mutableListOf()
            current.remove(noteId)
            current.add(0, noteId)
            prefs[KEY] = current.take(maxRecents).joinToString(",")
        }
    }

    suspend fun removeNote(noteId: Long) {
        context.recentNotesDataStore.edit { prefs ->
            val current = prefs[KEY]?.split(",")?.mapNotNull { it.toLongOrNull() }?.toMutableList()
                ?: return@edit
            current.remove(noteId)
            prefs[KEY] = current.joinToString(",")
        }
    }
}
