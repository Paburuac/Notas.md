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
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
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

    LaunchedEffect(uiState.content) {
        if (contentField.text.isEmpty() && uiState.content.isNotEmpty()) {
            contentField = TextFieldValue(uiState.content)
        }
    }

    BackHandler {
        viewModel.onContentChange(contentField.text)
        viewModel.saveNote()
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
                        viewModel.onContentChange(contentField.text)
                        viewModel.saveNote()
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
                        viewModel.onContentChange(contentField.text)
                        viewModel.saveNote()
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
                    MarkdownPreview(content = uiState.content)
                }
            } else {
                TextField(
                    value         = contentField,
                    onValueChange = { newValue ->
                        contentField = newValue
                        viewModel.onContentChange(newValue.text)
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
                    colors   = transparentTextFieldColors(),
                    modifier = Modifier.fillMaxSize()
                )
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
    val text      = field.text
    val cursor    = field.selection.start
    val lineStart = (text.lastIndexOf('\n', cursor - 1) + 1).coerceAtLeast(0)
    val newText   = text.substring(0, lineStart) + prefix + text.substring(lineStart)
    return TextFieldValue(text = newText, selection = TextRange(cursor + prefix.length))
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