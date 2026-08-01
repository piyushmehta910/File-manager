package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextEditorScreen(viewModel: FileManagerViewModel, onExit: () -> Unit) {
    val filePath by viewModel.editorFilePath.collectAsState()
    val initialContent by viewModel.editorContent.collectAsState()

    var contentState by remember { mutableStateOf("") }
    var wordWrap by remember { mutableStateOf(true) }
    var searchKeyword by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }

    LaunchedEffect(initialContent) {
        contentState = initialContent
    }

    val fileName = filePath?.let { File(it).name } ?: "Editor"

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Column {
                            Text(fileName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            filePath?.let {
                                Text(
                                    it.split("/").takeLast(3).joinToString("/"),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onExit, modifier = Modifier.testTag("editor_back")) {
                            Icon(Icons.Default.ArrowBack, "Exit")
                        }
                    },
                    actions = {
                        // Word wrap Toggle
                        IconButton(onClick = { wordWrap = !wordWrap }) {
                            Icon(
                                imageVector = if (wordWrap) Icons.Default.WrapText else Icons.Default.FormatAlignLeft,
                                contentDescription = "Word Wrap Toggle",
                                tint = if (wordWrap) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Search Toggle
                        IconButton(onClick = { isSearching = !isSearching }) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search keyword",
                                tint = if (isSearching) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Save File Button
                        IconButton(onClick = { viewModel.saveEditorContent(contentState) }, modifier = Modifier.testTag("editor_save")) {
                            Icon(Icons.Default.Save, "Save", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                )

                // Inline Keyword Search Bar
                AnimatedVisibility(visible = isSearching) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = searchKeyword,
                            onValueChange = { searchKeyword = it },
                            placeholder = { Text("Find keyword...") },
                            modifier = Modifier.weight(1f).testTag("editor_find_input"),
                            singleLine = true,
                            trailingIcon = {
                                if (searchKeyword.isNotEmpty()) {
                                    IconButton(onClick = { searchKeyword = "" }) {
                                        Icon(Icons.Default.Clear, "Clear")
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        val lines = contentState.split("\n")
        val lineCount = lines.size

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Line numbers column
            Column(
                modifier = Modifier
                    .width(42.dp)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 12.dp, horizontal = 4.dp),
                horizontalAlignment = Alignment.End
            ) {
                for (i in 1..lineCount) {
                    Text(
                        text = "$i",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
            }

            Divider(modifier = Modifier.fillMaxHeight().width(1.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

            // Main Editor Input Field
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                BasicTextField(
                    value = contentState,
                    onValueChange = { contentState = it },
                    textStyle = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    ),
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .testTag("editor_text_field")
                )

                // Floating saved tip
                if (contentState == initialContent && initialContent.isNotEmpty()) {
                    Card(
                        modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                    ) {
                        Text("Synced with Disk", modifier = Modifier.padding(8.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
