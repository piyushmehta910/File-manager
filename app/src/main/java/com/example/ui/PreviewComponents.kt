package com.example.ui

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.media.MediaPlayer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import java.io.File
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImagePreviewScreen(path: String, onExit: () -> Unit) {
    val file = File(path)
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(file.name) },
                navigationIcon = {
                    IconButton(onClick = onExit) {
                        Icon(Icons.Default.ArrowBack, "Exit")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = file,
                contentDescription = "Image preview",
                modifier = Modifier.fillMaxSize().testTag("image_preview"),
                contentScale = ContentScale.Fit
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioPreviewScreen(path: String, onExit: () -> Unit) {
    val context = LocalContext.current
    val file = File(path)
    
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var duration by remember { mutableIntStateOf(0) }
    var currentPosition by remember { mutableIntStateOf(0) }

    LaunchedEffect(path) {
        val mp = MediaPlayer().apply {
            setDataSource(context, Uri.fromFile(file))
            prepare()
        }
        mediaPlayer = mp
        duration = mp.duration
        isPlaying = true
        mp.start()

        while (true) {
            if (mp.isPlaying) {
                currentPosition = mp.currentPosition
            }
            delay(500)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.release()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Audio Player") },
                navigationIcon = {
                    IconButton(onClick = onExit) {
                        Icon(Icons.Default.ArrowBack, "Exit")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp)
                .testTag("audio_preview"),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = "Audio track",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(64.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = file.name,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Seekbar slider tracker
            Slider(
                value = currentPosition.toFloat(),
                onValueChange = { newValue ->
                    currentPosition = newValue.toInt()
                    mediaPlayer?.seekTo(currentPosition)
                },
                valueRange = 0f..duration.toFloat(),
                modifier = Modifier.fillMaxWidth().testTag("audio_slider")
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(formatDuration(currentPosition), style = MaterialTheme.typography.bodySmall)
                Text(formatDuration(duration), style = MaterialTheme.typography.bodySmall)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Audio Player Action Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                IconButton(onClick = {
                    mediaPlayer?.seekTo((currentPosition - 5000).coerceAtLeast(0))
                }) {
                    Icon(Icons.Default.Replay5, "Replay 5s", modifier = Modifier.size(32.dp))
                }

                FloatingActionButton(
                    onClick = {
                        val mp = mediaPlayer ?: return@FloatingActionButton
                        if (mp.isPlaying) {
                            mp.pause()
                            isPlaying = false
                        } else {
                            mp.start()
                            isPlaying = true
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.testTag("audio_play_pause")
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause"
                    )
                }

                IconButton(onClick = {
                    mediaPlayer?.seekTo((currentPosition + 5000).coerceAtMost(duration))
                }) {
                    Icon(Icons.Default.Forward5, "Forward 5s", modifier = Modifier.size(32.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PDFPreviewScreen(path: String, onExit: () -> Unit) {
    val context = LocalContext.current
    val file = File(path)
    val pages = remember { mutableStateListOf<Bitmap>() }
    var errorMessage by remember { mutableStateOf("") }

    LaunchedEffect(path) {
        try {
            val fileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(fileDescriptor)
            val pageCount = renderer.pageCount
            for (i in 0 until pageCount) {
                val page = renderer.openPage(i)
                val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                pages.add(bitmap)
                page.close()
            }
            renderer.close()
        } catch (e: Exception) {
            errorMessage = "Could not render PDF preview: ${e.localizedMessage}"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(file.name) },
                navigationIcon = {
                    IconButton(onClick = onExit) {
                        Icon(Icons.Default.ArrowBack, "Exit")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (errorMessage.isNotEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Text(errorMessage, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
            }
        } else if (pages.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(Color.LightGray)
                    .testTag("pdf_preview")
            ) {
                items(pages) { bitmap ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "PDF Page",
                            modifier = Modifier.fillMaxWidth(),
                            contentScale = ContentScale.FillWidth
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApkPreviewScreen(path: String, onExit: () -> Unit) {
    val context = LocalContext.current
    val file = File(path)

    var appLabel by remember { mutableStateOf("Unknown App") }
    var packageName by remember { mutableStateOf("Unknown Package") }
    var versionString by remember { mutableStateOf("1.0") }
    var permissionsList by remember { mutableStateOf<List<String>>(emptyList()) }
    var errorMsg by remember { mutableStateOf("") }

    LaunchedEffect(path) {
        try {
            val pm = context.packageManager
            val packageInfo = pm.getPackageArchiveInfo(path, PackageManager.GET_PERMISSIONS)
            if (packageInfo != null) {
                val appInfo = packageInfo.applicationInfo
                if (appInfo != null) {
                    appInfo.sourceDir = path
                    appInfo.publicSourceDir = path
                    appLabel = pm.getApplicationLabel(appInfo).toString()
                }
                packageName = packageInfo.packageName
                versionString = "${packageInfo.versionName} (${packageInfo.versionCode})"
                permissionsList = packageInfo.requestedPermissions?.toList() ?: emptyList()
            } else {
                errorMsg = "Unable to analyze APK installer."
            }
        } catch (e: Exception) {
            errorMsg = "Failed parsing APK: ${e.localizedMessage}"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("App Installer Info") },
                navigationIcon = {
                    IconButton(onClick = onExit) {
                        Icon(Icons.Default.ArrowBack, "Exit")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (errorMsg.isNotEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Text(errorMsg, color = MaterialTheme.colorScheme.error)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
                    .testTag("apk_preview"),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.InstallMobile,
                        contentDescription = "APK Icon",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(appLabel, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                Text(packageName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))

                Spacer(modifier = Modifier.height(24.dp))

                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Package Version: $versionString", fontWeight = FontWeight.SemiBold)
                        Text("Target SDK: Android 11+ compatible", fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    "Requested Permissions (${permissionsList.size})",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (permissionsList.isEmpty()) {
                    Text(
                        "No specific permissions requested.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.align(Alignment.Start)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(permissionsList) { perm ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            ) {
                                Text(
                                    text = perm.removePrefix("android.permission."),
                                    modifier = Modifier.padding(8.dp),
                                    fontSize = 11.sp,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatDuration(ms: Int): String {
    val sec = (ms / 1000) % 60
    val min = (ms / 1000) / 60
    return String.format("%02d:%02d", min, sec)
}
