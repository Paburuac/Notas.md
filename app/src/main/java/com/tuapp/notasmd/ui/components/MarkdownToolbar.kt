package com.tuapp.notasmd.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun MarkdownToolbar(
    onInsert:      (prefix: String, suffix: String) -> Unit,
    onInsertLine:  (prefix: String) -> Unit,
    onIndent:      () -> Unit,
    onDedent:      () -> Unit,
    onColorRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment   = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        // Negrita
        ToolbarIconBtn(onClick = { onInsert("**", "**") }) {
            Icon(Icons.Default.FormatBold, "Negrita")
        }
        // Cursiva
        ToolbarIconBtn(onClick = { onInsert("*", "*") }) {
            Icon(Icons.Default.FormatItalic, "Cursiva")
        }

        ToolbarSep()

        // H1
        ToolbarTextBtn("H1") { onInsertLine("# ") }
        // H2
        ToolbarTextBtn("H2") { onInsertLine("## ") }

        ToolbarSep()

        // Lista viñetas
        ToolbarIconBtn(onClick = { onInsertLine("- ") }) {
            Icon(Icons.Default.FormatListBulleted, "Lista viñetas")
        }
        // Lista numerada
        ToolbarIconBtn(onClick = { onInsertLine("1. ") }) {
            Icon(Icons.Default.FormatListNumbered, "Lista numerada")
        }
        // Checkbox
        ToolbarIconBtn(onClick = { onInsertLine("- [ ] ") }) {
            Icon(Icons.Default.CheckBox, "Tarea")
        }
        // Aumentar sangría
        ToolbarIconBtn(onClick = onIndent) {
            Icon(Icons.Default.FormatIndentIncrease, "Aumentar sangría")
        }
        // Reducir sangría
        ToolbarIconBtn(onClick = onDedent) {
            Icon(Icons.Default.FormatIndentDecrease, "Reducir sangría")
        }

        ToolbarSep()

        // Cita
        ToolbarIconBtn(onClick = { onInsertLine("> ") }) {
            Icon(Icons.Default.FormatQuote, "Cita")
        }
        // Código inline
        ToolbarTextBtn("`ab`") { onInsert("`", "`") }
        // Bloque de código
        ToolbarTextBtn("```") { onInsert("```\n", "\n```") }

        ToolbarSep()

        // Separador horizontal
        ToolbarTextBtn("—") { onInsert("\n---\n", "") }
        // Enlace
        ToolbarIconBtn(onClick = { onInsert("[", "](url)") }) {
            Icon(Icons.Default.Link, "Enlace")
        }

        ToolbarSep()

        // Color de texto
        ToolbarIconBtn(onClick = onColorRequest) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .background(Color(0xFFE53935), CircleShape)
            )
        }
    }
}

@Composable
private fun ToolbarIconBtn(onClick: () -> Unit, content: @Composable () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(40.dp)) { content() }
}

@Composable
private fun ToolbarTextBtn(label: String, onClick: () -> Unit) {
    TextButton(
        onClick        = onClick,
        modifier       = Modifier.height(40.dp),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ToolbarSep() {
    VerticalDivider(modifier = Modifier.height(24.dp))
}