package com.example.data

import android.content.Context
import android.os.Environment
import android.webkit.MimeTypeMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.io.*
import java.security.MessageDigest
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class FileRepository(
    private val context: Context,
    private val bookmarkDao: BookmarkDao,
    private val recentDao: RecentDao,
    private val vaultDao: VaultDao
) {
    // Standard Roots
    val sandboxRoot: File = File(context.filesDir, "Sandbox").apply { if (!exists()) mkdirs() }
    val externalRoot: File? = Environment.getExternalStorageDirectory()

    init {
        // Initialize sample workspace files to ensure a rich first-run experience
        initializeSampleWorkspace()
    }

    private fun initializeSampleWorkspace() {
        try {
            val welcomeFile = File(sandboxRoot, "Welcome_Guide.txt")
            if (!welcomeFile.exists()) {
                welcomeFile.writeText(
                    """==================================================
🌟 WELCOME TO PRO FILES - MATERIAL DESIGN FILE MANAGER 🌟
==================================================

Pro Files is an elegant, lightning-fast, offline-first, and security-focused file manager. 
It combines visual beauty (Material You, adaptive themes) with power-user utility (dual-pane navigation, deep storage analysis, real-time file previews).

Core Key Capabilities:
--------------------
1. 📁 Instant Navigation: Easily browse local files, standard downloads, documents, and bookmarks.
2. 🎛️ Dual-Pane Layout: Turn your tablet or landscape phone into an advanced file organizer. Drag and drop, synchronize, or compare directories side-by-side.
3. ⚡ Storage Analyzer & Optimizer: Interactively explore size usage with percentage bars, identify space-hogging large files, old downloads, and duplicate files.
4. 🔐 Secure Vault: Hide sensitive documents with an encrypted PIN lock.
5. ✍️ Built-in Text Editor: Edit logs, text documents, or code with line numbers, search/replace, and word-wrap.
6. 📄 Professional Preview Engine: View images, play audio, and analyze APK installer permissions and bundle metadata natively!

This workspace is fully simulated inside the App's secure private storage. It works instantly without needing general system storage permission prompts!

Feel free to create files, rename folders, zip/unzip archives, or move items around. Have fun organizing!
"""
                )

                // Create subfolders
                val docsDir = File(sandboxRoot, "Documents").apply { mkdirs() }
                val downloadsDir = File(sandboxRoot, "Downloads").apply { mkdirs() }
                val codeDir = File(sandboxRoot, "Development").apply { mkdirs() }
                val archivesDir = File(sandboxRoot, "Archives").apply { mkdirs() }

                // Populate documents
                File(docsDir, "Monthly_Report.json").writeText(
                    """{
  "title": "Monthly Productivity Report",
  "period": "July 2026",
  "status": "Completed",
  "summary": {
    "tasks_resolved": 142,
    "efficiency_rate": 0.965,
    "highlight": "File Manager android applet build succeeded in record time."
  }
}"""
                )
                File(docsDir, "Travel_Notes.md").writeText(
                    """# ✈️ Travel Planning 2026

## Destinations
- **Tokyo, Japan**: Mount Fuji hike, ramen tours, and digital art museums.
- **Geneva, Switzerland**: Alpine lakes, particle physics tours, and cheese fondue.

## Packing Checklist
- [x] Passport & visas
- [x] Multi-port universal adapter
- [ ] Lightweight rain jacket
- [ ] Comfortable walking sneakers
"""
                )

                // Populate Code
                File(codeDir, "Main.kt").writeText(
                    """package com.example.app

import kotlin.system.exitProcess

fun main() {
    println("Initializing File Manager Engine...")
    val totalMemory = Runtime.getRuntime().totalMemory()
    println("Total VM Memory: ${"$"}{totalMemory / 1024 / 1024} MB")
    
    // Developer debug logs
    for (i in 1..5) {
        println("Self-test check ${"$"}i: SUCCESS")
    }
}"""
                )

                // Populate duplicates to showcase the duplicate finder
                File(downloadsDir, "Receipt_Temp.txt").writeText("Amount: \$250.00\nMerchant: Android Cafe\nDate: 2026-07-31")
                File(docsDir, "Receipt_Backup.txt").writeText("Amount: \$250.00\nMerchant: Android Cafe\nDate: 2026-07-31") // Identical file content

                // Create a sample zip
                val zipTarget = File(archivesDir, "Sample_Backup.zip")
                ZipOutputStream(BufferedOutputStream(FileOutputStream(zipTarget))).use { zos ->
                    val filesToZip = listOf("Backup_Document.txt" to "Backup containing server configs.")
                    for ((name, content) in filesToZip) {
                        zos.putNextEntry(ZipEntry(name))
                        zos.write(content.toByteArray())
                        zos.closeEntry()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // --- Directory Traversal ---
    suspend fun getFiles(path: String): List<FileModel> = withContext(Dispatchers.IO) {
        val directory = File(path)
        if (!directory.exists() || !directory.isDirectory) return@withContext emptyList()

        val files = directory.listFiles() ?: return@withContext emptyList()
        val flowBookmarks = bookmarkDao.getAllBookmarks().firstOrNull() ?: emptyList()
        val bookmarkedPaths = flowBookmarks.map { it.path }.toSet()

        files.map { file ->
            val isDir = file.isDirectory
            val childCount = if (isDir) file.listFiles()?.size ?: 0 else 0
            val size = if (isDir) 0L else file.length()
            val extension = file.extension.lowercase(Locale.ROOT)
            val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "application/octet-stream"

            FileModel(
                name = file.name,
                path = file.absolutePath,
                size = size,
                isDirectory = isDir,
                lastModified = file.lastModified(),
                mimeType = mimeType,
                itemCount = childCount,
                isBookmarked = bookmarkedPaths.contains(file.absolutePath)
            )
        }.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
    }

    // --- File Operations ---
    suspend fun createFolder(parentPath: String, folderName: String): Boolean = withContext(Dispatchers.IO) {
        val folder = File(parentPath, folderName)
        if (folder.exists()) return@withContext false
        folder.mkdirs()
    }

    suspend fun createFile(parentPath: String, fileName: String, content: String = ""): Boolean = withContext(Dispatchers.IO) {
        val file = File(parentPath, fileName)
        if (file.exists()) return@withContext false
        try {
            file.writeText(content)
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun deleteFile(path: String): Boolean = withContext(Dispatchers.IO) {
        val file = File(path)
        if (!file.exists()) return@withContext false
        val success = file.deleteRecursively()
        if (success) {
            bookmarkDao.deleteByPath(path)
            recentDao.deleteRecentByPath(path)
        }
        success
    }

    suspend fun renameFile(path: String, newName: String): Boolean = withContext(Dispatchers.IO) {
        val file = File(path)
        if (!file.exists()) return@withContext false
        val parent = file.parentFile ?: return@withContext false
        val destFile = File(parent, newName)
        if (destFile.exists()) return@withContext false
        val success = file.renameTo(destFile)
        if (success) {
            bookmarkDao.deleteByPath(path)
            recentDao.deleteRecentByPath(path)
        }
        success
    }

    suspend fun copyFiles(
        sources: List<String>,
        destFolder: String,
        onProgress: suspend (currentFileName: String, progress: Float) -> Unit = { _, _ -> }
    ): Boolean = withContext(Dispatchers.IO) {
        val destDir = File(destFolder)
        if (!destDir.exists() || !destDir.isDirectory) return@withContext false

        var totalFiles = 0
        var processedFiles = 0

        // Count files first
        fun countFiles(file: File) {
            if (file.isDirectory) {
                file.listFiles()?.forEach { countFiles(it) }
            } else {
                totalFiles++
            }
        }
        sources.forEach { countFiles(File(it)) }
        if (totalFiles == 0) totalFiles = 1

        suspend fun copySingleFile(srcFile: File, dstFile: File) {
            if (srcFile.isDirectory) {
                dstFile.mkdirs()
                srcFile.listFiles()?.forEach { child ->
                    copySingleFile(child, File(dstFile, child.name))
                }
            } else {
                // Ensure parent dir exists
                dstFile.parentFile?.mkdirs()
                BufferedInputStream(FileInputStream(srcFile)).use { bis ->
                    BufferedOutputStream(FileOutputStream(dstFile)).use { bos ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        while (bis.read(buffer).also { bytesRead = it } != -1) {
                            bos.write(buffer, 0, bytesRead)
                        }
                    }
                }
                processedFiles++
                onProgress(srcFile.name, processedFiles.toFloat() / totalFiles)
            }
        }

        try {
            for (srcPath in sources) {
                val srcFile = File(srcPath)
                if (!srcFile.exists()) continue
                
                // Conflict resolution: append "_Copy" if destination exists
                var dstFile = File(destDir, srcFile.name)
                var counter = 1
                while (dstFile.exists()) {
                    val baseName = srcFile.nameWithoutExtension
                    val ext = srcFile.extension
                    val suffix = if (ext.isNotEmpty()) ".$ext" else ""
                    dstFile = File(destDir, "${baseName}_Copy$counter$suffix")
                    counter++
                }

                copySingleFile(srcFile, dstFile)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun moveFiles(
        sources: List<String>,
        destFolder: String,
        onProgress: suspend (currentFileName: String, progress: Float) -> Unit = { _, _ -> }
    ): Boolean = withContext(Dispatchers.IO) {
        // We first copy and then delete sources
        val success = copyFiles(sources, destFolder, onProgress)
        if (success) {
            sources.forEach { path ->
                File(path).deleteRecursively()
                bookmarkDao.deleteByPath(path)
                recentDao.deleteRecentByPath(path)
            }
        }
        success
    }

    // --- Archive Manager (ZIP/UNZIP) ---
    suspend fun zipFiles(sources: List<String>, zipFilePath: String): Boolean = withContext(Dispatchers.IO) {
        val zipFile = File(zipFilePath)
        try {
            ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile))).use { zos ->
                fun addFileToZip(file: File, relativePath: String) {
                    if (file.isDirectory) {
                        file.listFiles()?.forEach { child ->
                            val pathSuffix = if (relativePath.isNotEmpty()) "$relativePath/" else ""
                            addFileToZip(child, "$pathSuffix${child.name}")
                        }
                    } else {
                        zos.putNextEntry(ZipEntry(relativePath))
                        BufferedInputStream(FileInputStream(file)).use { bis ->
                            val buffer = ByteArray(8192)
                            var count: Int
                            while (bis.read(buffer).also { count = it } != -1) {
                                zos.write(buffer, 0, count)
                            }
                        }
                        zos.closeEntry()
                    }
                }

                for (sourcePath in sources) {
                    val srcFile = File(sourcePath)
                    if (srcFile.exists()) {
                        addFileToZip(srcFile, srcFile.name)
                    }
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun unzipFile(zipFilePath: String, destFolder: String): Boolean = withContext(Dispatchers.IO) {
        val destDir = File(destFolder).apply { mkdirs() }
        try {
            ZipInputStream(BufferedInputStream(FileInputStream(zipFilePath))).use { zis ->
                var entry = zis.nextEntry
                val buffer = ByteArray(8192)
                while (entry != null) {
                    val file = File(destDir, entry.name)
                    if (entry.isDirectory) {
                        file.mkdirs()
                    } else {
                        // Ensure parent directories exist
                        file.parentFile?.mkdirs()
                        BufferedOutputStream(FileOutputStream(file)).use { bos ->
                            var count: Int
                            while (zis.read(buffer).also { count = it } != -1) {
                                bos.write(buffer, 0, count)
                            }
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // --- Storage Analysis ---
    suspend fun analyzeStorage(rootPath: String): StorageAnalysisReport = withContext(Dispatchers.IO) {
        val rootDir = File(rootPath)
        val report = StorageAnalysisReport()
        if (!rootDir.exists() || !rootDir.isDirectory) return@withContext report

        var totalFilesCount = 0L
        val largeFilesThreshold = 5 * 1024 * 1024 // 5 MB

        val sizeByMD5 = mutableMapOf<String, MutableList<String>>()

        fun calculateMD5(file: File): String {
            return try {
                val digest = MessageDigest.getInstance("MD5")
                val stream = FileInputStream(file)
                val buffer = ByteArray(4096)
                var read = stream.read(buffer)
                while (read != -1) {
                    digest.update(buffer, 0, read)
                    read = stream.read(buffer)
                }
                stream.close()
                val hashBytes = digest.digest()
                val builder = StringBuilder()
                for (b in hashBytes) {
                    builder.append(String.format("%02x", b))
                }
                builder.toString()
            } catch (e: Exception) {
                file.name + file.length() // Fallback string representation
            }
        }

        fun traverse(file: File) {
            if (file.isDirectory) {
                file.listFiles()?.forEach { traverse(it) }
            } else {
                totalFilesCount++
                val size = file.length()
                val ext = file.extension.lowercase(Locale.ROOT)
                val category = when {
                    ext in listOf("jpg", "jpeg", "png", "gif", "bmp", "webp") -> StorageCategory.IMAGES
                    ext in listOf("mp4", "mkv", "avi", "mov", "wmv", "flv") -> StorageCategory.VIDEOS
                    ext in listOf("mp3", "wav", "m4a", "ogg", "flac") -> StorageCategory.AUDIO
                    ext in listOf("pdf", "txt", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "json", "xml", "html", "md") -> StorageCategory.DOCUMENTS
                    ext in listOf("zip", "rar", "7z", "tar", "gz") -> StorageCategory.ARCHIVES
                    ext == "apk" -> StorageCategory.APKS
                    else -> StorageCategory.OTHER
                }

                report.addSize(category, size)

                // Large File detection
                if (size >= largeFilesThreshold) {
                    report.largeFiles.add(
                        FileModel(
                            name = file.name,
                            path = file.absolutePath,
                            size = size,
                            isDirectory = false,
                            lastModified = file.lastModified(),
                            mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "application/octet-stream"
                        )
                    )
                }

                // Check duplicates by MD5 hash
                val hash = calculateMD5(file)
                if (size > 10) { // skip tiny files
                    val list = sizeByMD5.getOrPut(hash) { mutableListOf() }
                    list.add(file.absolutePath)
                }
            }
        }

        traverse(rootDir)

        // Find duplicates
        sizeByMD5.forEach { (hash, paths) ->
            if (paths.size > 1) {
                val duplicateGroup = paths.map { path ->
                    val file = File(path)
                    FileModel(
                        name = file.name,
                        path = file.absolutePath,
                        size = file.length(),
                        isDirectory = false,
                        lastModified = file.lastModified(),
                        mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.extension) ?: "application/octet-stream"
                    )
                }
                report.duplicates.add(duplicateGroup)
            }
        }

        // Sort large files
        report.largeFiles.sortByDescending { it.size }
        report.totalSpace = rootDir.totalSpace
        report.freeSpace = rootDir.freeSpace
        report.usableSpace = rootDir.usableSpace
        report
    }
}

// --- Storage Analyzer helper data classes ---
enum class StorageCategory {
    IMAGES, VIDEOS, AUDIO, DOCUMENTS, ARCHIVES, APKS, OTHER
}

class StorageAnalysisReport {
    var totalSpace: Long = 0L
    var freeSpace: Long = 0L
    var usableSpace: Long = 0L
    val categorySizes = mutableMapOf<StorageCategory, Long>()
    val largeFiles = mutableListOf<FileModel>()
    val duplicates = mutableListOf<List<FileModel>>() // Groups of identical files

    fun addSize(category: StorageCategory, size: Long) {
        categorySizes[category] = (categorySizes[category] ?: 0L) + size
    }

    val totalCategorizedSize: Long
        get() = categorySizes.values.sum()
}
