package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageAnalyzerScreen(viewModel: FileManagerViewModel) {
    val report by viewModel.storageReport.collectAsState()
    val isAnalyzing by viewModel.isAnalyzing.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }

    // Trigger analysis when entering
    LaunchedEffect(Unit) {
        viewModel.analyzeActiveStorage()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Storage Analyzer & Cleaner") },
                actions = {
                    IconButton(onClick = { viewModel.analyzeActiveStorage() }) {
                        Icon(Icons.Default.Refresh, "Re-analyze")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (isAnalyzing) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Scanning filesystem for space optimizations...", style = MaterialTheme.typography.bodyMedium)
                }
            }
        } else if (report == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Error compiling storage diagnostics.", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            val r = report!!
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp)
                    .testTag("analyzer_screen")
            ) {
                // Total Storage Overview Card
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Storage Diagnostic Result",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Total Analyzed Size: ${formatSize(r.totalCategorizedSize)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Icon(
                            Icons.Default.QueryStats,
                            "Stats",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                // Visual Bars category breakdown
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Size Category Breakdown",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        val categories = StorageCategory.values()
                        categories.forEach { cat ->
                            val size = r.categorySizes[cat] ?: 0L
                            val ratio = if (r.totalCategorizedSize > 0) size.toFloat() / r.totalCategorizedSize else 0f
                            val categoryColor = getCategoryColor(cat)

                            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(cat.name, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    Text(formatSize(size), fontSize = 12.sp)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = ratio,
                                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                    color = categoryColor,
                                    trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                                )
                            }
                        }
                    }
                }

                // Interactive Tabs: [Large Files, Duplicates]
                TabRow(
                    selectedTabIndex = selectedTab,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Large Files (>5MB)") },
                        modifier = Modifier.testTag("tab_large_files")
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Duplicates") },
                        modifier = Modifier.testTag("tab_duplicates")
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Box(modifier = Modifier.weight(1f)) {
                    if (selectedTab == 0) {
                        LargeFilesTab(largeFiles = r.largeFiles, onDelete = { path ->
                            viewModel.deleteFileDirectly(path)
                            viewModel.analyzeActiveStorage()
                        })
                    } else {
                        DuplicatesTab(duplicateGroups = r.duplicates, onDelete = { path ->
                            viewModel.deleteFileDirectly(path)
                            viewModel.analyzeActiveStorage()
                        })
                    }
                }
            }
        }
    }
}

@Composable
fun LargeFilesTab(largeFiles: List<FileModel>, onDelete: (String) -> Unit) {
    if (largeFiles.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No files exceed the 5MB threshold.", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(largeFiles) { file ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val (icon, tint) = getIconAndColorForExtension(file.extension)
                        Icon(icon, file.name, tint = tint, modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                file.name,
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(file.readableSize, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                        }
                        IconButton(onClick = { onDelete(file.path) }) {
                            Icon(Icons.Default.DeleteForever, "Clean space", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DuplicatesTab(duplicateGroups: List<List<FileModel>>, onDelete: (String) -> Unit) {
    if (duplicateGroups.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Fantastic! No duplicate files discovered.", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            items(duplicateGroups) { group ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            "Matching Duplicates (${group.size} files)",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        group.forEachIndexed { index, file ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        file.name,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        "Path: .../${file.path.split("/").takeLast(2).joinToString("/")}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                                Text(
                                    file.readableSize,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )
                                // Let user delete duplicate occurrences, keeping at least one is advised by clean UI.
                                IconButton(onClick = { onDelete(file.path) }) {
                                    Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                            if (index < group.lastIndex) {
                                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                            }
                        }
                    }
                }
            }
        }
    }
}

fun getCategoryColor(category: StorageCategory): Color {
    return when (category) {
        StorageCategory.IMAGES -> Color(0xFF38BDF8)
        StorageCategory.VIDEOS -> Color(0xFFEC4899)
        StorageCategory.AUDIO -> Color(0xFF10B981)
        StorageCategory.DOCUMENTS -> Color(0xFFF59E0B)
        StorageCategory.ARCHIVES -> Color(0xFF8B5CF6)
        StorageCategory.APKS -> Color(0xFFE11D48)
        StorageCategory.OTHER -> Color(0xFF94A3B8)
    }
}
