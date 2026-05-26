package com.tuapp.notasmd.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

val PRESET_COLORS = listOf(
    "#C0392B", "#E74C3C",
    "#E67E22", "#F39C12",
    "#27AE60", "#16A085",
    "#2980B9", "#8E44AD",
    "#795548", "#8B6914",
    "#546E7A", "#607D8B",
)

@Composable
fun ColorPicker(
    selectedColor: String,
    onColorSelected: (String) -> Unit
) {
    LazyVerticalGrid(
        columns            = GridCells.Fixed(6),
        modifier           = Modifier.fillMaxWidth().height(96.dp),
        contentPadding     = PaddingValues(4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement   = Arrangement.spacedBy(8.dp)
    ) {
        items(PRESET_COLORS) { colorHex ->
            val isSelected = selectedColor.equals(colorHex, ignoreCase = true)
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        color = Color(android.graphics.Color.parseColor(colorHex)),
                        shape = CircleShape
                    )
                    .border(
                        width = if (isSelected) 3.dp else 0.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.onBackground
                        else Color.Transparent,
                        shape = CircleShape
                    )
                    .clickable { onColorSelected(colorHex) }
            )
        }
    }
}