package com.tuapp.notasmd.ui.screens.editor

import android.text.Editable
import android.text.TextWatcher
import android.util.TypedValue
import android.widget.EditText
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tuapp.notasmd.R
import com.tuapp.notasmd.data.local.entity.Note
import com.tuapp.notasmd.ui.components.ColorPicker
import com.tuapp.notasmd.ui.components.MarkdownToolbar
import com.tuapp.notasmd.ui.components.toFormattedDate
import com.tuapp.notasmd.viewmodel.EditorViewModel
import io.noties.markwon.Markwon
import io.noties.markwon.editor.MarkwonEditor
import io.noties.markwon.editor.MarkwonEditorTextWatcher
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.html.HtmlPlugin
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    viewModel: EditorViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToNote: (sectionId: Long, noteId: Long) -> Unit = { _, _ -> }
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

    // Referencia mutable al EditText para que la toolbar pueda insertar texto
    val editTextRef = remember { mutableStateOf<EditText?>(null) }

    var showColorDialog by remember { mutableStateOf(false) }
    var selectedColor   by remember { mutableStateOf("#E53935") }

    // Líneas tal como estaban en el último guardado (sin timestamp), para detectar cambios
    var savedLines by remember { mutableStateOf<List<String>>(emptyList()) }

    fun saveWithTimestamps() {
        val editText = editTextRef.value ?: return
        val now  = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        val lines = editText.text.toString().split('\n')
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
        savedLines = newLines.map { stripLineTimestamp(it) }
        // Actualizar el EditText sin activar el TextWatcher
        val selStart = editText.selectionStart.coerceAtMost(newContent.length)
        val selEnd   = editText.selectionEnd.coerceAtMost(newContent.length)
        editText.removeTextChangedListener(editText.tag as? TextWatcher)
        editText.setText(newContent)
        editText.setSelection(selStart.coerceAtLeast(0), selEnd.coerceAtLeast(0))
        (editText.tag as? TextWatcher)?.let { editText.addTextChangedListener(it) }
        viewModel.onContentChange(newContent)
        viewModel.saveNote()
    }

    BackHandler {
        saveWithTimestamps()
        onNavigateBack()
    }

    DisposableEffect(Unit) {
        onDispose {
            editTextRef.value?.let { viewModel.onContentChange(it.text.toString()) }
            viewModel.saveNote()
        }
    }

    val scrollState   = rememberScrollState()
    val rootView      = LocalView.current
    val density       = LocalDensity.current

    var keyboardHeight by remember { mutableStateOf(0.dp) }
    DisposableEffect(rootView) {
        val listener = android.view.ViewTreeObserver.OnGlobalLayoutListener {
            val rect = android.graphics.Rect()
            rootView.getWindowVisibleDisplayFrame(rect)
            val diff = rootView.height - rect.bottom
            keyboardHeight = with(density) { diff.coerceAtLeast(0).toDp() }
        }
        rootView.viewTreeObserver.addOnGlobalLayoutListener(listener)
        onDispose { rootView.viewTreeObserver.removeOnGlobalLayoutListener(listener) }
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
                    IconButton(onClick = { viewModel.togglePin() }) {
                        Icon(
                            imageVector        = Icons.Default.PushPin,
                            contentDescription = if (uiState.isPinned) "Desfijar" else "Fijar",
                            tint               = if (uiState.isPinned) MaterialTheme.colorScheme.primary
                                                 else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = {
                        val filename = uiState.title.ifBlank { "nota" }.trim() + ".md"
                        exportLauncher.launch(filename)
                    }) {
                        Icon(Icons.Default.FileDownload, "Exportar")
                    }
                    IconButton(onClick = { saveWithTimestamps() }) {
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
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding())
                .padding(bottom = keyboardHeight)
        ) {
            // Título
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

            // Sugerencias de enlace
            if (uiState.showLinkPicker && uiState.linkSuggestions.isNotEmpty()) {
                Card(
                    modifier  = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    uiState.linkSuggestions.forEach { note ->
                        TextButton(
                            onClick  = {
                                editTextRef.value?.let { et ->
                                    insertNoteLinkInEditText(et, note)
                                    viewModel.onContentChange(et.text.toString())
                                }
                                viewModel.dismissLinkPicker()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text  = note.title.ifBlank { "Sin título" },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Editor WYSIWYG con MarkwonEditor
            val textColor = MaterialTheme.colorScheme.onBackground
            val hintColor = MaterialTheme.colorScheme.onSurfaceVariant
            val initialContent = uiState.content

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
            ) {
                AndroidView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    factory = { ctx ->
                        val markwon = Markwon.builder(ctx)
                            .usePlugin(HtmlPlugin.create())
                            .usePlugin(TablePlugin.create(ctx))
                            .build()
                        val editor = MarkwonEditor.create(markwon)

                        EditText(ctx).apply {
                            background    = null
                            typeface      = ResourcesCompat.getFont(ctx, R.font.courier_prime_regular)
                            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
                            setTextColor(textColor.toArgb())
                            setHintTextColor(hintColor.toArgb())
                            hint          = "Escribe en Markdown..."
                            isSingleLine  = false
                            minLines      = 20

                            if (initialContent.isNotEmpty()) {
                                setText(initialContent)
                                savedLines = initialContent.split('\n').map { stripLineTimestamp(it) }
                            }

                            val watcher = MarkwonEditorTextWatcher.withPreRender(
                                editor,
                                Executors.newSingleThreadExecutor(),
                                this
                            )

                            // TextWatcher extra para sincronizar con el ViewModel y manejar
                            // auto-continuar listas y el link picker ((
                            val syncWatcher = object : TextWatcher {
                                private var prevText = text.toString()
                                private var prevSel  = 0

                                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                                    prevText = s.toString()
                                    prevSel  = selectionStart
                                }
                                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                                override fun afterTextChanged(s: Editable?) {
                                    val current = s.toString()
                                    viewModel.onContentChange(current)

                                    // Auto-continuar listas al presionar Enter
                                    val cursor = selectionStart
                                    if (current.length == prevText.length + 1 &&
                                        cursor > 0 && current[cursor - 1] == '\n') {
                                        val prevLineEnd   = cursor - 1
                                        val prevLineStart = (current.lastIndexOf('\n', prevLineEnd - 1) + 1).coerceAtLeast(0)
                                        val prevLine      = current.substring(prevLineStart, prevLineEnd)
                                        val indentation   = prevLine.takeWhile { it == ' ' }
                                        val trimmed       = prevLine.trimStart()
                                        val bulletPrefix  = when {
                                            trimmed.startsWith("- [ ] ") || trimmed.startsWith("- [x] ") -> "- [ ] "
                                            trimmed.startsWith("- ")  -> "- "
                                            trimmed.startsWith("* ")  -> "* "
                                            trimmed.startsWith("> ")  -> "> "
                                            trimmed.matches(Regex("""^\d+\. .*""")) -> {
                                                val num = trimmed.substringBefore(". ").toIntOrNull() ?: 1
                                                "${num + 1}. "
                                            }
                                            else -> null
                                        }
                                        if (bulletPrefix != null) {
                                            val fullPrefix = indentation + bulletPrefix
                                            if (trimmed.removePrefix(bulletPrefix).isBlank()) {
                                                // Línea vacía con prefijo → cancelar lista
                                                s?.delete(prevLineStart, cursor)
                                            } else {
                                                s?.insert(cursor, fullPrefix)
                                            }
                                        }
                                    }

                                    // Link picker ((
                                    val cur2 = selectionStart.coerceAtMost(current.length)
                                    val beforeCursor = current.substring(0, cur2)
                                    val lastOpen     = beforeCursor.lastIndexOf("((")
                                    if (lastOpen != -1) {
                                        val afterOpen = beforeCursor.substring(lastOpen + 2)
                                        if (!afterOpen.contains(")")) {
                                            viewModel.onLinkQueryChange(afterOpen)
                                        } else {
                                            viewModel.dismissLinkPicker()
                                        }
                                    } else {
                                        viewModel.dismissLinkPicker()
                                    }
                                }
                            }

                            // Guardamos el syncWatcher en el tag para poder quitarlo en saveWithTimestamps
                            tag = syncWatcher
                            addTextChangedListener(watcher)
                            addTextChangedListener(syncWatcher)
                        }
                    },
                    update = { et ->
                        editTextRef.value = et
                        // Solo actualizamos el color (puede cambiar con el tema)
                        et.setTextColor(textColor.toArgb())
                    }
                )
            }

            HorizontalDivider()
            MarkdownToolbar(
                onInsert     = { prefix, suffix ->
                    editTextRef.value?.insertMarkdown(prefix, suffix)
                    editTextRef.value?.let { viewModel.onContentChange(it.text.toString()) }
                },
                onInsertLine = { prefix ->
                    editTextRef.value?.insertAtLineStart(prefix)
                    editTextRef.value?.let { viewModel.onContentChange(it.text.toString()) }
                },
                onIndent = {
                    editTextRef.value?.indentLine()
                    editTextRef.value?.let { viewModel.onContentChange(it.text.toString()) }
                },
                onDedent = {
                    editTextRef.value?.dedentLine()
                    editTextRef.value?.let { viewModel.onContentChange(it.text.toString()) }
                },
                onColorRequest = { showColorDialog = true }
            )
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
                    editTextRef.value?.insertMarkdown(
                        "<span style=\"color:$selectedColor\">",
                        "</span>"
                    )
                    editTextRef.value?.let { viewModel.onContentChange(it.text.toString()) }
                    showColorDialog = false
                }) { Text("Aplicar") }
            },
            dismissButton = {
                TextButton(onClick = { showColorDialog = false }) { Text("Cancelar") }
            }
        )
    }
}

// ── Extensiones de EditText para manipular texto ──────────────────────────────

private fun EditText.insertMarkdown(prefix: String, suffix: String = "") {
    val start    = selectionStart.coerceAtLeast(0)
    val end      = selectionEnd.coerceAtLeast(start)
    val selected = text.substring(start, end)
    text.replace(start, end, prefix + selected + suffix)
    setSelection(start + prefix.length, start + prefix.length + selected.length)
}

private fun EditText.insertAtLineStart(prefix: String) {
    val start     = selectionStart.coerceAtLeast(0)
    val end       = selectionEnd.coerceAtLeast(start)
    val str       = text.toString()
    val lineStart = (str.lastIndexOf('\n', start - 1) + 1).coerceAtLeast(0)
    if (start == end) {
        text.insert(lineStart, prefix)
        setSelection(start + prefix.length)
    } else {
        val firstLine = (str.lastIndexOf('\n', start - 1) + 1).coerceAtLeast(0)
        val section   = str.substring(firstLine, end)
        val modified  = section.split('\n').joinToString("\n") { prefix + it }
        text.replace(firstLine, end, modified)
        setSelection(start + prefix.length, end + (modified.length - section.length))
    }
}

private fun EditText.indentLine() {
    val start     = selectionStart.coerceAtLeast(0)
    val end       = selectionEnd.coerceAtLeast(start)
    val str       = text.toString()
    val lineStart = (str.lastIndexOf('\n', start - 1) + 1).coerceAtLeast(0)
    if (start == end) {
        text.insert(lineStart, "  ")
        setSelection(start + 2)
    } else {
        val section  = str.substring(lineStart, end)
        val modified = section.split('\n').joinToString("\n") { "  $it" }
        val added    = modified.length - section.length
        text.replace(lineStart, end, modified)
        setSelection(start + 2, end + added)
    }
}

private fun EditText.dedentLine() {
    val start     = selectionStart.coerceAtLeast(0)
    val end       = selectionEnd.coerceAtLeast(start)
    val str       = text.toString()
    val lineStart = (str.lastIndexOf('\n', start - 1) + 1).coerceAtLeast(0)
    if (start == end) {
        val lineEnd = str.indexOf('\n', lineStart).let { if (it == -1) str.length else it }
        val spaces  = str.substring(lineStart, lineEnd).takeWhile { it == ' ' }.length.coerceAtMost(2)
        if (spaces == 0) return
        text.delete(lineStart, lineStart + spaces)
        setSelection((start - spaces).coerceAtLeast(lineStart))
    } else {
        val section  = str.substring(lineStart, end)
        val modified = section.split('\n').joinToString("\n") { line ->
            val spaces = line.takeWhile { it == ' ' }.length.coerceAtMost(2)
            line.substring(spaces)
        }
        val removed = section.length - modified.length
        text.replace(lineStart, end, modified)
        setSelection(start, (end - removed).coerceAtLeast(start))
    }
}

private fun insertNoteLinkInEditText(editText: EditText, note: Note) {
    val text         = editText.text.toString()
    val cursor       = editText.selectionStart.coerceAtMost(text.length)
    val beforeCursor = text.substring(0, cursor)
    val lastOpen     = beforeCursor.lastIndexOf("((")
    if (lastOpen == -1) return
    val link = "((${note.id}|${note.title.ifBlank { "Sin título" }}))"
    editText.text.replace(lastOpen, cursor, link)
    editText.setSelection(lastOpen + link.length)
}

private fun stripLineTimestamp(line: String): String =
    line.replace(Regex(""" \[\d{2}:\d{2}]$"""), "")

@Composable
private fun transparentTextFieldColors() = TextFieldDefaults.colors(
    focusedContainerColor   = Color.Transparent,
    unfocusedContainerColor = Color.Transparent,
    focusedIndicatorColor   = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent,
)
