package com.example.ui

import android.os.Environment
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun BrowserScreen(
    viewModel: FileManagerViewModel,
    onOpenTextFile: (String) -> Unit,
    onPreviewImage: (String) -> Unit,
    onPreviewAudio: (String) -> Unit,
    onPreviewPdf: (String) -> Unit,
    onPreviewApk: (String) -> Unit
) {
    val currentPath by viewModel.currentPath.collectAsState()
    val files by viewModel.files.collectAsState()
    val selectedPaths by viewModel.selectedPaths.collectAsState()
    val clipboardMode by viewModel.clipboardMode.collectAsState()
    val clipboardItems by viewModel.clipboardItems.collectAsState()
    val viewMode by viewModel.viewMode.collectAsState()
    val sortBy by viewModel.sortBy.collectAsState()
    val sortAscending by viewModel.sortAscending.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val filterType by viewModel.filterType.collectAsState()

    // Dialog trigger states
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var showCreateFileDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showCompressDialog by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<FileModel?>(null) }

    // Intercept back button to navigate up
    val sandboxRoot = viewModel.repository.sandboxRoot.absolutePath
    val externalRoot = Environment.getExternalStorageDirectory()?.absolutePath ?: sandboxRoot
    val currentWorkspace by viewModel.currentWorkspace.collectAsState()
    val activeRoot = if (currentWorkspace == WorkspaceType.SANDBOX) sandboxRoot else externalRoot

    BackHandler(enabled = currentPath != activeRoot) {
        viewModel.navigateUp()
    }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background)) {
                // Search Input Field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.updateSearchQuery(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .testTag("browser_search"),
                    placeholder = { Text("Search files & folders...") },
                    leadingIcon = { Icon(Icons.Default.Search, "Search") },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                Icon(Icons.Default.Clear, "Clear")
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                // Navigation Breadcrumbs
                BreadcrumbsRow(
                    currentPath = currentPath,
                    rootPath = activeRoot,
                    onBreadcrumbClick = { viewModel.navigateTo(it) }
                )

                // Configuration Bar (Sort, View Mode, Filter Clear)
                ConfigBarRow(
                    viewMode = viewMode,
                    sortBy = sortBy,
                    sortAscending = sortAscending,
                    filterType = filterType,
                    onToggleViewMode = {
                        viewModel.setViewMode(if (viewMode == ViewMode.LIST) ViewMode.GRID else ViewMode.LIST)
                    },
                    onSortChanged = { viewModel.updateSort(it) },
                    onClearFilter = { viewModel.updateFilterType(FilterType.ALL) }
                )
            }
        },
        floatingActionButton = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                FloatingActionButton(
                    onClick = { showCreateFileDialog = true },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.testTag("fab_create_file")
                ) {
                    Icon(Icons.Default.NoteAdd, "New File")
                }
                FloatingActionButton(
                    onClick = { showCreateFolderDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.testTag("fab_create_folder")
                ) {
                    Icon(Icons.Default.CreateNewFolder, "New Folder")
                }
            }
        },
        bottomBar = {
            AnimatedVisibility(
                visible = selectedPaths.isNotEmpty(),
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                SelectionActionBar(
                    selectedCount = selectedPaths.size,
                    onCopy = { viewModel.copySelected() },
                    onCut = { viewModel.cutSelected() },
                    onDelete = { viewModel.deleteSelected() },
                    onZip = { showCompressDialog = true },
                    onRename = {
                        val path = selectedPaths.first()
                        val target = files.find { it.path == path }
                        if (target != null) {
                            renameTarget = target
                            showRenameDialog = true
                        }
                    },
                    onShare = { /* Optional system intent share triggers in production */ },
                    onClear = { viewModel.clearSelection() }
                )
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (files.isEmpty()) {
                EmptyStateView()
            } else {
                if (viewMode == ViewMode.LIST) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(files) { file ->
                            val isSelected = selectedPaths.contains(file.path)
                            FileItemRow(
                                file = file,
                                isSelected = isSelected,
                                onClick = {
                                    if (selectedPaths.isNotEmpty()) {
                                        viewModel.toggleSelection(file.path)
                                    } else {
                                        handleFileClick(
                                            file = file,
                                            viewModel = viewModel,
                                            onOpenTextFile = onOpenTextFile,
                                            onPreviewImage = onPreviewImage,
                                            onPreviewAudio = onPreviewAudio,
                                            onPreviewPdf = onPreviewPdf,
                                            onPreviewApk = onPreviewApk
                                        )
                                    }
                                },
                                onLongClick = {
                                    viewModel.toggleSelection(file.path)
                                },
                                onBookmarkToggle = { viewModel.toggleBookmark(file) }
                            )
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 100.dp),
                        modifier = Modifier.fillMaxSize().padding(8.dp),
                        contentPadding = PaddingValues(4.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(files) { file ->
                            val isSelected = selectedPaths.contains(file.path)
                            FileItemGrid(
                                file = file,
                                isSelected = isSelected,
                                onClick = {
                                    if (selectedPaths.isNotEmpty()) {
                                        viewModel.toggleSelection(file.path)
                                    } else {
                                        handleFileClick(
                                            file = file,
                                            viewModel = viewModel,
                                            onOpenTextFile = onOpenTextFile,
                                            onPreviewImage = onPreviewImage,
                                            onPreviewAudio = onPreviewAudio,
                                            onPreviewPdf = onPreviewPdf,
                                            onPreviewApk = onPreviewApk
                                        )
                                    }
                                },
                                onLongClick = {
                                    viewModel.toggleSelection(file.path)
                                }
                            )
                        }
                    }
                }
            }

            // Floating Clipboard Paste Bar
            if (clipboardMode != null) {
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 24.dp)
                        .padding(horizontal = 16.dp)
                        .testTag("paste_bar"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "${if (clipboardMode == ClipboardMode.COPY) "Copy" else "Cut"} ${clipboardItems.size} items",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Button(
                            onClick = { viewModel.pasteClipboard() },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Paste Here")
                        }
                        TextButton(onClick = { viewModel.clearClipboard() }) {
                            Text("Cancel", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }

    // --- Create Folder Dialog ---
    if (showCreateFolderDialog) {
        var folderName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateFolderDialog = false },
            title = { Text("Create New Folder") },
            text = {
                OutlinedTextField(
                    value = folderName,
                    onValueChange = { folderName = it },
                    placeholder = { Text("Enter folder name") },
                    modifier = Modifier.testTag("dialog_folder_input")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (folderName.isNotBlank()) {
                            viewModel.createFolder(folderName)
                            showCreateFolderDialog = false
                        }
                    },
                    modifier = Modifier.testTag("dialog_folder_confirm")
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateFolderDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // --- Create File Dialog ---
    if (showCreateFileDialog) {
        var fileName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateFileDialog = false },
            title = { Text("Create New File") },
            text = {
                OutlinedTextField(
                    value = fileName,
                    onValueChange = { fileName = it },
                    placeholder = { Text("e.g. note.txt") },
                    modifier = Modifier.testTag("dialog_file_input")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (fileName.isNotBlank()) {
                            viewModel.createNewFile(fileName)
                            showCreateFileDialog = false
                        }
                    },
                    modifier = Modifier.testTag("dialog_file_confirm")
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateFileDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // --- Rename Dialog ---
    if (showRenameDialog && renameTarget != null) {
        var newName by remember { mutableStateOf(renameTarget!!.name) }
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename") },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    modifier = Modifier.testTag("dialog_rename_input")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newName.isNotBlank() && newName != renameTarget!!.name) {
                            viewModel.renameFile(renameTarget!!.path, newName)
                            viewModel.clearSelection()
                            showRenameDialog = false
                        }
                    },
                    modifier = Modifier.testTag("dialog_rename_confirm")
                ) {
                    Text("Rename")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // --- Zip Dialog ---
    if (showCompressDialog) {
        var archiveName by remember { mutableStateOf("Archive") }
        AlertDialog(
            onDismissRequest = { showCompressDialog = false },
            title = { Text("Compress to ZIP") },
            text = {
                OutlinedTextField(
                    value = archiveName,
                    onValueChange = { archiveName = it },
                    placeholder = { Text("Enter ZIP filename") },
                    modifier = Modifier.testTag("dialog_zip_input")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (archiveName.isNotBlank()) {
                            viewModel.compressSelected(archiveName)
                            showCompressDialog = false
                        }
                    },
                    modifier = Modifier.testTag("dialog_zip_confirm")
                ) {
                    Text("Zip")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCompressDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

fun handleFileClick(
    file: FileModel,
    viewModel: FileManagerViewModel,
    onOpenTextFile: (String) -> Unit,
    onPreviewImage: (String) -> Unit,
    onPreviewAudio: (String) -> Unit,
    onPreviewPdf: (String) -> Unit,
    onPreviewApk: (String) -> Unit
) {
    if (file.isDirectory) {
        viewModel.navigateTo(file.path)
    } else {
        viewModel.addToRecent(file)
        val ext = file.extension.lowercase()
        when {
            ext in listOf("txt", "json", "xml", "html", "md") -> onOpenTextFile(file.path)
            ext in listOf("jpg", "jpeg", "png", "gif", "webp") -> onPreviewImage(file.path)
            ext in listOf("mp3", "wav", "m4a") -> onPreviewAudio(file.path)
            ext == "pdf" -> onPreviewPdf(file.path)
            ext == "apk" -> onPreviewApk(file.path)
            ext == "zip" -> {
                viewModel.extractArchive(file.path)
            }
        }
    }
}

@Composable
fun BreadcrumbsRow(
    currentPath: String,
    rootPath: String,
    onBreadcrumbClick: (String) -> Unit
) {
    // Break path down relative to workspace root
    val relativePath = currentPath.removePrefix(rootPath).trim('/')
    val parts = if (relativePath.isEmpty()) emptyList() else relativePath.split('/')

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Root Label
        Text(
            text = "Root",
            fontWeight = FontWeight.Bold,
            color = if (parts.isEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .clickable { onBreadcrumbClick(rootPath) }
                .testTag("breadcrumb_root")
        )

        parts.forEachIndexed { index, part ->
            Icon(Icons.Default.ChevronRight, "Separator", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
            // Reconstruct path up to this part
            val subPath = rootPath + "/" + parts.take(index + 1).joinToString("/")
            Text(
                text = part,
                fontWeight = if (index == parts.lastIndex) FontWeight.Bold else FontWeight.Normal,
                color = if (index == parts.lastIndex) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .clickable { onBreadcrumbClick(subPath) }
                    .testTag("breadcrumb_$part")
            )
        }
    }
}

@Composable
fun ConfigBarRow(
    viewMode: ViewMode,
    sortBy: SortBy,
    sortAscending: Boolean,
    filterType: FilterType,
    onToggleViewMode: () -> Unit,
    onSortChanged: (SortBy) -> Unit,
    onClearFilter: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Sort trigger
            IconButton(onClick = { onSortChanged(SortBy.NAME) }, modifier = Modifier.testTag("sort_name")) {
                Icon(
                    imageVector = if (sortBy == SortBy.NAME) {
                        if (sortAscending) Icons.Default.SortByAlpha else Icons.Default.Sort
                    } else Icons.Default.Sort,
                    contentDescription = "Sort name",
                    tint = if (sortBy == SortBy.NAME) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            }
            IconButton(onClick = { onSortChanged(SortBy.SIZE) }, modifier = Modifier.testTag("sort_size")) {
                Icon(
                    imageVector = Icons.Default.VerticalAlignBottom,
                    contentDescription = "Sort size",
                    tint = if (sortBy == SortBy.SIZE) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            }
            IconButton(onClick = { onSortChanged(SortBy.DATE) }, modifier = Modifier.testTag("sort_date")) {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = "Sort date",
                    tint = if (sortBy == SortBy.DATE) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (filterType != FilterType.ALL) {
                AssistChip(
                    onClick = onClearFilter,
                    label = { Text(filterType.name) },
                    trailingIcon = { Icon(Icons.Default.Close, "Clear Filter", modifier = Modifier.size(14.dp)) },
                    modifier = Modifier.padding(end = 8.dp)
                )
            }

            IconButton(onClick = onToggleViewMode, modifier = Modifier.testTag("view_mode_toggle")) {
                Icon(
                    imageVector = if (viewMode == ViewMode.LIST) Icons.Default.GridView else Icons.Default.ViewList,
                    contentDescription = "View mode"
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileItemRow(
    file: FileModel,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onBookmarkToggle: () -> Unit
) {
    val (icon, tint) = getIconAndColorForExtension(file.extension)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else Color.Transparent)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .testTag("file_item_${file.name}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (file.isDirectory) MaterialTheme.colorScheme.primaryContainer else tint.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (file.isDirectory) Icons.Default.Folder else icon,
                contentDescription = file.name,
                tint = if (file.isDirectory) MaterialTheme.colorScheme.primary else tint
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = file.name,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = file.readableSize,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }

        IconButton(onClick = onBookmarkToggle) {
            Icon(
                imageVector = if (file.isBookmarked) Icons.Default.Star else Icons.Default.StarBorder,
                contentDescription = "Bookmark",
                tint = if (file.isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileItemGrid(
    file: FileModel,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val (icon, tint) = getIconAndColorForExtension(file.extension)
    Card(
        modifier = Modifier
            .aspectRatio(1f)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .testTag("file_grid_item_${file.name}"),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (file.isDirectory) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f) else tint.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (file.isDirectory) Icons.Default.Folder else icon,
                    contentDescription = file.name,
                    tint = if (file.isDirectory) MaterialTheme.colorScheme.primary else tint,
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = file.name,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall,
                fontSize = 11.sp
            )
            Text(
                text = file.readableSize,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 9.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
fun SelectionActionBar(
    selectedCount: Int,
    onCopy: () -> Unit,
    onCut: () -> Unit,
    onDelete: () -> Unit,
    onZip: () -> Unit,
    onRename: () -> Unit,
    onShare: () -> Unit,
    onClear: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().testTag("selection_action_bar"),
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClear, modifier = Modifier.testTag("clear_selection")) {
                    Icon(Icons.Default.Close, "Clear")
                }
                Text(
                    text = "$selectedCount selected",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (selectedCount == 1) {
                    IconButton(onClick = onRename, modifier = Modifier.testTag("rename_selection")) {
                        Icon(Icons.Default.Edit, "Rename")
                    }
                }
                IconButton(onClick = onCopy, modifier = Modifier.testTag("copy_selection")) {
                    Icon(Icons.Default.ContentCopy, "Copy")
                }
                IconButton(onClick = onCut, modifier = Modifier.testTag("cut_selection")) {
                    Icon(Icons.Default.ContentCut, "Cut")
                }
                IconButton(onClick = onZip, modifier = Modifier.testTag("zip_selection")) {
                    Icon(Icons.Default.FolderZip, "Zip")
                }
                IconButton(onClick = onDelete, modifier = Modifier.testTag("delete_selection")) {
                    Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
fun EmptyStateView() {
    Box(
        modifier = Modifier.fillMaxSize().testTag("empty_view"),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.FolderOpen,
                contentDescription = "Empty Folder",
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "This folder is empty",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Use the buttons below to create files or folders.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}
