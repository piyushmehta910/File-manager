package com.example.ui

import android.os.Environment
import java.util.Locale
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: FileManagerViewModel,
    onNavigateToBrowser: (String) -> Unit,
    onNavigateToAnalyzer: () -> Unit,
    onNavigateToVault: () -> Unit,
    onOpenTextFile: (String) -> Unit,
    onPreviewImage: (String) -> Unit,
    onPreviewAudio: (String) -> Unit,
    onPreviewPdf: (String) -> Unit,
    onPreviewApk: (String) -> Unit
) {
    val currentWorkspace by viewModel.currentWorkspace.collectAsState()
    val files by viewModel.files.collectAsState()
    val bookmarks by viewModel.bookmarks.collectAsState()
    val recentFiles by viewModel.recentFiles.collectAsState()

    // Calculate simulated or real disk space
    val rootFile = if (currentWorkspace == WorkspaceType.SANDBOX) {
        viewModel.repository.sandboxRoot
    } else {
        Environment.getStorageDirectory() // Fallback to sandboxed root if not accessible
    }
    
    val totalSpace = rootFile.totalSpace
    val freeSpace = rootFile.freeSpace
    val usableSpace = rootFile.usableSpace
    val usedSpace = totalSpace - freeSpace
    val useRatio = if (totalSpace > 0) usedSpace.toFloat() / totalSpace else 0f

    val readableTotal = formatSize(totalSpace)
    val readableUsed = formatSize(usedSpace)
    val readableFree = formatSize(freeSpace)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // 1. Workspace Selection Header
        item {
            WorkspaceHeaderCard(
                activeWorkspace = currentWorkspace,
                onWorkspaceChanged = { viewModel.toggleWorkspace(it) }
            )
        }

        // 2. Beautiful Storage Card
        item {
            StorageOverviewCard(
                usedRatio = useRatio,
                usedText = readableUsed,
                totalText = readableTotal,
                freeText = readableFree,
                onAnalyzeClick = onNavigateToAnalyzer
            )
        }

        // 3. Quick Navigation Categories
        item {
            Text(
                text = "Categories",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(10.dp))
            CategoriesGrid(
                onCategoryClick = { categoryFilter ->
                    viewModel.updateFilterType(categoryFilter)
                    val targetPath = if (currentWorkspace == WorkspaceType.SANDBOX) {
                        viewModel.repository.sandboxRoot.absolutePath
                    } else {
                        android.os.Environment.getExternalStorageDirectory()?.absolutePath ?: viewModel.repository.sandboxRoot.absolutePath
                    }
                    onNavigateToBrowser(targetPath)
                }
            )
        }

        // 4. Secure Vault & Bookmarks Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Secure Vault Card
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .height(110.dp)
                        .clickable { onNavigateToVault() }
                        .testTag("vault_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.EnhancedEncryption,
                            contentDescription = "Secure Vault",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Secure Vault",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                // Storage Analyzer Shortcut
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .height(110.dp)
                        .clickable { onNavigateToAnalyzer() }
                        .testTag("analyzer_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Analytics,
                            contentDescription = "Storage Analyzer",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Space Cleaner",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // 5. Bookmarked / Favorited Folders
        if (bookmarks.isNotEmpty()) {
            item {
                Text(
                    text = "Pinned Folder Favorites",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(bookmarks) { bookmark ->
                        Card(
                            modifier = Modifier
                                .width(140.dp)
                                .clickable { onNavigateToBrowser(bookmark.path) }
                                .testTag("bookmark_item_${bookmark.name}"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.Start
                            ) {
                                Icon(
                                    imageVector = if (bookmark.isFolder) Icons.Default.FolderSpecial else Icons.Default.Star,
                                    contentDescription = "Pinned",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = bookmark.name,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    fontWeight = FontWeight.SemiBold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = if (bookmark.isFolder) "Folder" else "File",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 6. Recent Files List
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Files",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                if (recentFiles.isNotEmpty()) {
                    TextButton(onClick = { viewModel.clearRecent() }) {
                        Text("Clear All")
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            if (recentFiles.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No recent file activity yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        recentFiles.take(6).forEach { recent ->
                            RecentFileRow(
                                file = recent,
                                onClick = {
                                    val file = File(recent.path)
                                    if (file.exists() && !file.isDirectory) {
                                        val ext = file.extension.lowercase()
                                        when {
                                            ext in listOf("txt", "json", "xml", "html", "md") -> onOpenTextFile(recent.path)
                                            ext in listOf("jpg", "jpeg", "png", "gif", "webp") -> onPreviewImage(recent.path)
                                            ext in listOf("mp3", "wav", "m4a") -> onPreviewAudio(recent.path)
                                            ext == "pdf" -> onPreviewPdf(recent.path)
                                            ext == "apk" -> onPreviewApk(recent.path)
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WorkspaceHeaderCard(
    activeWorkspace: WorkspaceType,
    onWorkspaceChanged: (WorkspaceType) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Current Workspace",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Text(
                    text = if (activeWorkspace == WorkspaceType.SANDBOX) "Sandbox Storage" else "Device Storage",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (activeWorkspace == WorkspaceType.SANDBOX) MaterialTheme.colorScheme.primary else Color.Transparent)
                        .clickable { onWorkspaceChanged(WorkspaceType.SANDBOX) }
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Sandbox",
                        fontWeight = FontWeight.Bold,
                        color = if (activeWorkspace == WorkspaceType.SANDBOX) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (activeWorkspace == WorkspaceType.DEVICE_ROOT) MaterialTheme.colorScheme.primary else Color.Transparent)
                        .clickable { onWorkspaceChanged(WorkspaceType.DEVICE_ROOT) }
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Device",
                        fontWeight = FontWeight.Bold,
                        color = if (activeWorkspace == WorkspaceType.DEVICE_ROOT) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
fun StorageOverviewCard(
    usedRatio: Float,
    usedText: String,
    totalText: String,
    freeText: String,
    onAnalyzeClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Storage Space",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                IconButton(onClick = onAnalyzeClick) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = "Speed check",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Linear Progress Indicator representing used ratio
            LinearProgressIndicator(
                progress = usedRatio.coerceIn(0f, 1f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(CircleShape),
                color = if (usedRatio > 0.9f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Used Space",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                    Text(
                        text = "$usedText / $totalText",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Free Space",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                    Text(
                        text = freeText,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun CategoriesGrid(onCategoryClick: (FilterType) -> Unit) {
    val items = listOf(
        CategoryItem("Images", Icons.Outlined.Image, Color(0xFF38BDF8), FilterType.IMAGES),
        CategoryItem("Videos", Icons.Outlined.VideoLibrary, Color(0xFFEC4899), FilterType.VIDEOS),
        CategoryItem("Audio", Icons.Outlined.AudioFile, Color(0xFF10B981), FilterType.AUDIO),
        CategoryItem("Documents", Icons.Outlined.Description, Color(0xFFF59E0B), FilterType.DOCUMENTS),
        CategoryItem("Archives", Icons.Outlined.FolderZip, Color(0xFF8B5CF6), FilterType.ARCHIVES),
        CategoryItem("Apps", Icons.Outlined.InstallMobile, Color(0xFFE11D48), FilterType.APKS)
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items.take(3).forEach { item ->
            CategoryCard(item = item, modifier = Modifier.weight(1f), onClick = { onCategoryClick(item.filter) })
        }
    }
    Spacer(modifier = Modifier.height(8.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items.drop(3).forEach { item ->
            CategoryCard(item = item, modifier = Modifier.weight(1f), onClick = { onCategoryClick(item.filter) })
        }
    }
}

@Composable
fun CategoryCard(item: CategoryItem, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(
        modifier = modifier
            .height(85.dp)
            .clickable { onClick() }
            .testTag("category_card_${item.title.lowercase()}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(item.color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.title,
                    tint = item.color,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = item.title,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun RecentFileRow(file: RecentFile, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(12.dp)
            .testTag("recent_file_row_${file.name}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val (icon, tint) = getIconAndColorForExtension(file.fileType)
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(tint.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = file.fileType, tint = tint)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = file.name,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = formatSize(file.fileSize),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = "Open file",
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
            modifier = Modifier.size(20.dp)
        )
    }
}

data class CategoryItem(
    val title: String,
    val icon: ImageVector,
    val color: Color,
    val filter: FilterType
)

fun formatSize(size: Long): String {
    if (size <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
    return String.format(Locale.US, "%.1f %s", size / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
}

fun getIconAndColorForExtension(ext: String): Pair<ImageVector, Color> {
    return when (ext.lowercase(Locale.ROOT)) {
        "pdf" -> Icons.Default.PictureAsPdf to Color(0xFFEF4444)
        in listOf("jpg", "jpeg", "png", "gif", "webp") -> Icons.Default.Image to Color(0xFF38BDF8)
        in listOf("mp4", "mkv", "avi") -> Icons.Default.VideoLibrary to Color(0xFFEC4899)
        in listOf("mp3", "wav", "m4a") -> Icons.Default.AudioFile to Color(0xFF10B981)
        in listOf("zip", "rar", "7z") -> Icons.Default.FolderZip to Color(0xFF8B5CF6)
        in listOf("txt", "json", "xml", "html", "md") -> Icons.Default.Description to Color(0xFFF59E0B)
        "apk" -> Icons.Default.InstallMobile to Color(0xFFE11D48)
        else -> Icons.Default.Article to Color(0xFF94A3B8)
    }
}
