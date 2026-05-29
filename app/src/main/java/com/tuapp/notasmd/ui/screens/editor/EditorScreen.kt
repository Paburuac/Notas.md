package com.tuapp.notasmd.ui.screens.editor

import android.util.TypedValue
import android.widget.TextView
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tuapp.notasmd.R
import com.tuapp.notasmd.ui.components.ColorPicker
import com.tuapp.notasmd.ui.components.MarkdownToolbar
import com.tuapp.notasmd.ui.components.toFormattedDate
import com.tuapp.notasmd.viewmodel.EditorViewModel
import io.noties.markwon.Markwon
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.html.HtmlPlugin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    viewModel: EditorViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/markdown")
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        val exportContent = buildString {
            if (uiState.title.isNotBlank()) append("# ${uiState.title}\n\n")
            append(uiState.content)
        }
        context.contentResolver.openOutputStream(uri)?.use { stream ->
            stream.write(exportContent.toByteArray())
        }
    }

    var contentField    by remember { mutableStateOf(TextFieldValue("")) }
    var showColorDialog by remember { mutableStateOf(false) }
    var selectedColor   by remember { mutableStateOf("#E53935") }
    // líneas tal como estaban en el último guardado (sin timestamp), para detectar cambios
    var savedLines by remember { mutableStateOf<List<String>>(emptyList()) }

    LaunchedEffect(uiState.content) {
        if (contentField.text.isEmpty() && uiState.content.isNotEmpty()) {
            contentField = TextFieldValue(uiState.content)
            savedLines = uiState.content.split('\n').map { stripLineTimestamp(it) }
        }
    }

    // Aplica timestamps a las líneas modificadas y guarda
    fun saveWithTimestamps() {
        val now  = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        val lines = contentField.text.split('\n')
        val newLines = lines.mapIndexed { i, line ->
            val stripped  = stripLineTimestamp(line)
            val savedLine = savedLines.getOrNull(i) ?: ""
            when {
                stripped.isBlank()    -> stripped
                stripped != savedLine -> "$stripped [$now]"
                else                  -> line
            }
        }
        val newContent = newLines.joinToString("\n")
        val newSel = TextRange(
            contentField.selection.start.coerceAtMost(newContent.length),
            contentField.selection.end.coerceAtMost(newContent.length)
        )
        contentField = TextFieldValue(text = newContent, selection = newSel)
        savedLines   = newLines.map { stripLineTimestamp(it) }
        viewModel.onContentChange(newContent)
        viewModel.saveNote()
    }

    BackHandler {
        saveWithTimestamps()
        onNavigateBack()
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.onContentChange(contentField.text)
            viewModel.saveNote()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = {
                        saveWithTimestamps()
                        onNavigateBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                    }
                },
                title = {},
                actions = {
                    // Exportar a .md
                    IconButton(onClick = {
                        val filename = uiState.title.ifBlank { "nota" }.trim() + ".md"
                        exportLauncher.launch(filename)
                    }) {
                        Icon(
                            imageVector        = Icons.Default.FileDownload,
                            contentDescription = "Exportar"
                        )
                    }
                    // Alternar vista previa / edición
                    IconButton(onClick = { viewModel.togglePreview() }) {
                        Icon(
                            imageVector = if (uiState.isPreviewMode) Icons.Default.Edit
                            else Icons.Default.Visibility,
                            contentDescription = if (uiState.isPreviewMode) "Editar" else "Vista previa"
                        )
                    }
                    // Indicador de guardado / botón guardar
                    IconButton(onClick = {
                        saveWithTimestamps()
                    }) {
                        Icon(
                            imageVector = if (uiState.isSaved) Icons.Default.Check
                            else Icons.Default.Save,
                            contentDescription = "Guardar",
                            tint = if (uiState.isSaved) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TextField(
                value         = uiState.title,
                onValueChange = { viewModel.onTitleChange(it) },
                textStyle     = MaterialTheme.typography.headlineMedium.copy(
                    color = MaterialTheme.colorScheme.onBackground
                ),
                placeholder = {
                    Text(
                        "Título",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                colors     = transparentTextFieldColors(),
                singleLine = true,
                modifier   = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text  = "Creado: ${uiState.createdAt.toFormattedDate()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text  = "Modificado: ${uiState.updatedAt.toFormattedDate()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            HorizontalDivider()

            if (!uiState.isPreviewMode) {
                MarkdownToolbar(
                    onInsert     = { prefix, suffix ->
                        contentField = insertMarkdownSyntax(contentField, prefix, suffix)
                        viewModel.onContentChange(contentField.text)
                    },
                    onInsertLine = { prefix ->
                        contentField = insertAtLineStart(contentField, prefix)
                        viewModel.onContentChange(contentField.text)
                    },
                    onIndent = {
                        contentField = indentLine(contentField)
                        viewModel.onContentChange(contentField.text)
                    },
                    onDedent = {
                        contentField = dedentLine(contentField)
                        viewModel.onContentChange(contentField.text)
                    },
                    onColorRequest = { showColorDialog = true }
                )
                HorizontalDivider()
            }

            if (uiState.isPreviewMode) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    MarkdownPreview(content = uiState.content.split('\n').joinToString("\n") { stripLineTimestamp(it) })
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    TextField(
                        value         = contentField,
                        onValueChange = { newValue ->
                            val processed = autocontinueBulletList(contentField, newValue)
                            contentField = processed
                            viewModel.onContentChange(processed.text)
                        },
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onBackground
                        ),
                        placeholder = {
                            Text(
                                "Escribe en Markdown...",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        visualTransformation = MarkdownVisualTransformation(
                            timestampColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        colors   = transparentTextFieldColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 300.dp)
                    )
                }
            }
        }
    }

    if (showColorDialog) {
        AlertDialog(
            onDismissRequest = { showColorDialog = false },
            title = { Text("Color de texto") },
            text  = {
                ColorPicker(
                    selectedColor   = selectedColor,
                    onColorSelected = { selectedColor = it }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    contentField = insertMarkdownSyntax(
                        contentField,
                        "<span style=\"color:$selectedColor\">",
                        "</span>"
                    )
                    viewModel.onContentChange(contentField.text)
                    showColorDialog = false
                }) { Text("Aplicar") }
            },
            dismissButton = {
                TextButton(onClick = { showColorDialog = false }) { Text("Cancelar") }
            }
        )
    }
}

private fun insertMarkdownSyntax(
    field:  TextFieldValue,
    prefix: String,
    suffix: String = ""
): TextFieldValue {
    val text = field.text
    val sel  = field.selection
    return if (sel.collapsed) {
        val newText = text.substring(0, sel.start) + prefix + suffix + text.substring(sel.start)
        TextFieldValue(text = newText, selection = TextRange(sel.start + prefix.length))
    } else {
        val selected = text.substring(sel.start, sel.end)
        val newText  = text.substring(0, sel.start) + prefix + selected + suffix + text.substring(sel.end)
        TextFieldValue(
            text      = newText,
            selection = TextRange(sel.start + prefix.length, sel.start + prefix.length + selected.length)
        )
    }
}

private fun insertAtLineStart(
    field:  TextFieldValue,
    prefix: String
): TextFieldValue {
    val text = field.text
    val sel  = field.selection

    if (sel.collapsed) {
        val lineStart = (text.lastIndexOf('\n', sel.start - 1) + 1).coerceAtLeast(0)
        val newText   = text.substring(0, lineStart) + prefix + text.substring(lineStart)
        return TextFieldValue(text = newText, selection = TextRange(sel.start + prefix.length))
    }

    val selStart      = minOf(sel.start, sel.end)
    val selEnd        = maxOf(sel.start, sel.end)
    val firstLineStart = (text.lastIndexOf('\n', selStart - 1) + 1).coerceAtLeast(0)
    val section        = text.substring(firstLineStart, selEnd)
    val modified       = section.split('\n').joinToString("\n") { prefix + it }
    val newText        = text.substring(0, firstLineStart) + modified + text.substring(selEnd)
    val added          = modified.length - section.length
    return TextFieldValue(
        text      = newText,
        selection = TextRange(selStart + prefix.length, selEnd + added)
    )
}

private fun autocontinueBulletList(
    oldField: TextFieldValue,
    newField: TextFieldValue
): TextFieldValue {
    val oldText = oldField.text
    val newText = newField.text
    val cursor  = newField.selection.start

    if (newText.length != oldText.length + 1) return newField
    if (cursor <= 0 || newText[cursor - 1] != '\n') return newField

    val prevLineEnd   = cursor - 1
    val prevLineStart = (newText.lastIndexOf('\n', prevLineEnd - 1) + 1).coerceAtLeast(0)
    val prevLine      = newText.substring(prevLineStart, prevLineEnd)

    val indentation  = prevLine.takeWhile { it == ' ' }
    val trimmed      = prevLine.trimStart()

    val bulletPrefix = when {
        trimmed.startsWith("- [ ] ") || trimmed.startsWith("- [x] ") -> "- [ ] "
        trimmed.startsWith("- ")  -> "- "
        trimmed.startsWith("* ")  -> "* "
        trimmed.startsWith("> ")  -> "> "
        trimmed.matches(Regex("""^\d+\. .*""")) -> {
            val num = trimmed.substringBefore(". ").toIntOrNull() ?: 1
            "${num + 1}. "
        }
        else -> return newField
    }

    val fullPrefix = indentation + bulletPrefix

    // Si la línea anterior solo tenía el prefijo (vacía), cancelar la lista
    if (trimmed.removePrefix(bulletPrefix).isBlank()) {
        val cleaned = newText.substring(0, prevLineStart) + newText.substring(prevLineEnd)
        return TextFieldValue(text = cleaned, selection = TextRange(prevLineStart))
    }

    val inserted = newText.substring(0, cursor) + fullPrefix + newText.substring(cursor)
    return TextFieldValue(text = inserted, selection = TextRange(cursor + fullPrefix.length))
}

private fun indentLine(field: TextFieldValue): TextFieldValue {
    val text = field.text
    val sel  = field.selection

    if (sel.collapsed) {
        val lineStart = (text.lastIndexOf('\n', sel.start - 1) + 1).coerceAtLeast(0)
        val newText   = text.substring(0, lineStart) + "  " + text.substring(lineStart)
        return TextFieldValue(text = newText, selection = TextRange(sel.start + 2))
    }

    val selStart       = minOf(sel.start, sel.end)
    val selEnd         = maxOf(sel.start, sel.end)
    val firstLineStart = (text.lastIndexOf('\n', selStart - 1) + 1).coerceAtLeast(0)
    val section        = text.substring(firstLineStart, selEnd)
    val modified       = section.split('\n').joinToString("\n") { "  $it" }
    val added          = modified.length - section.length
    val newText        = text.substring(0, firstLineStart) + modified + text.substring(selEnd)
    return TextFieldValue(text = newText, selection = TextRange(selStart + 2, selEnd + added))
}

private fun dedentLine(field: TextFieldValue): TextFieldValue {
    val text = field.text
    val sel  = field.selection

    if (sel.collapsed) {
        val lineStart = (text.lastIndexOf('\n', sel.start - 1) + 1).coerceAtLeast(0)
        val lineEnd   = text.indexOf('\n', lineStart).let { if (it == -1) text.length else it }
        val spaces    = text.substring(lineStart, lineEnd).takeWhile { it == ' ' }.length.coerceAtMost(2)
        if (spaces == 0) return field
        val newText   = text.substring(0, lineStart) + text.substring(lineStart + spaces)
        return TextFieldValue(text = newText, selection = TextRange((sel.start - spaces).coerceAtLeast(lineStart)))
    }

    val selStart       = minOf(sel.start, sel.end)
    val selEnd         = maxOf(sel.start, sel.end)
    val firstLineStart = (text.lastIndexOf('\n', selStart - 1) + 1).coerceAtLeast(0)
    val section        = text.substring(firstLineStart, selEnd)
    val modified       = section.split('\n').joinToString("\n") { line ->
        val spaces = line.takeWhile { it == ' ' }.length.coerceAtMost(2)
        line.substring(spaces)
    }
    val removed  = section.length - modified.length
    val newText  = text.substring(0, firstLineStart) + modified + text.substring(selEnd)
    return TextFieldValue(text = newText, selection = TextRange(selStart, (selEnd - removed).coerceAtLeast(selStart)))
}

private fun stripLineTimestamp(line: String): String =
    line.replace(Regex(""" \[\d{2}:\d{2}]$"""), "")

private class MarkdownVisualTransformation(
    private val timestampColor: Color = Color.Gray
) : VisualTransformation {
    private val timestampRegex = Regex(""" \[\d{2}:\d{2}]""")

    override fun filter(text: AnnotatedString): TransformedText {
        val str    = text.text
        val spans  = mutableListOf<AnnotatedString.Range<SpanStyle>>()

        // Bold: **text**
        Regex("""\*\*(.+?)\*\*""", RegexOption.DOT_MATCHES_ALL).findAll(str).forEach { m ->
            spans.add(AnnotatedString.Range(SpanStyle(fontWeight = FontWeight.Bold), m.range.first, m.range.last + 1))
        }

        // Italic: *text* (not preceded/followed by another *)
        Regex("""(?<!\*)\*(?!\*)(.+?)(?<!\*)\*(?!\*)""", RegexOption.DOT_MATCHES_ALL).findAll(str).forEach { m ->
            spans.add(AnnotatedString.Range(SpanStyle(fontStyle = FontStyle.Italic), m.range.first, m.range.last + 1))
        }

        // Timestamps [HH:mm] → pequeño y gris
        timestampRegex.findAll(str).forEach { m ->
            spans.add(AnnotatedString.Range(
                SpanStyle(fontSize = 10.sp, color = timestampColor),
                m.range.first, m.range.last + 1
            ))
        }

        // Viñetas: reemplazar "- " al inicio de línea (con posible sangría) por "• "
        // Ambos son 2 caracteres → offset mapping Identity sigue siendo válido
        val transformed = buildString {
            var i = 0
            while (i < str.length) {
                // Detectar inicio de línea (posición 0 o después de \n)
                val atLineStart = i == 0 || str[i - 1] == '\n'
                if (atLineStart) {
                    // Saltar espacios de sangría
                    var j = i
                    while (j < str.length && str[j] == ' ') j++
                    // Verificar si sigue "- " (y no "- [ ]" que es checkbox)
                    if (j < str.length - 1 && str[j] == '-' && str[j + 1] == ' ' &&
                        !(j + 3 < str.length && str.substring(j, j + 4) == "- [")) {
                        // Copiar sangría
                        append(str.substring(i, j))
                        // Sustituir "- " → "• "
                        append("• ")
                        i = j + 2
                        continue
                    }
                }
                append(str[i])
                i++
            }
        }

        return TransformedText(
            AnnotatedString(text = transformed, spanStyles = spans),
            OffsetMapping.Identity
        )
    }
}

@Composable
private fun transparentTextFieldColors() = TextFieldDefaults.colors(
    focusedContainerColor   = Color.Transparent,
    unfocusedContainerColor = Color.Transparent,
    focusedIndicatorColor   = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent,
)

@Composable
private fun MarkdownPreview(
    content:  String,
    modifier: Modifier = Modifier
) {
    val context   = LocalContext.current
    val textColor = MaterialTheme.colorScheme.onBackground.toArgb()

    val markwon = remember {
        Markwon.builder(context)
            .usePlugin(HtmlPlugin.create())
            .usePlugin(TablePlugin.create(context))
            .build()
    }

    AndroidView(
        factory = { ctx ->
            TextView(ctx).apply {
                typeface = ResourcesCompat.getFont(ctx, R.font.courier_prime_regular)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
                setTextColor(textColor)
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                layoutParams = android.view.ViewGroup.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }
        },
        update = { textView ->
            textView.setTextColor(textColor)
            markwon.setMarkdown(textView, content)
        },
        modifier = modifier.fillMaxWidth()
    )
}