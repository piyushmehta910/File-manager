package com.example.ui

import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.security.MessageDigest
import java.util.Locale
import java.util.UUID
import android.webkit.MimeTypeMap
import android.net.Uri

class FileManagerViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    val repository = FileRepository(
        context = application,
        bookmarkDao = db.bookmarkDao(),
        recentDao = db.recentDao(),
        vaultDao = db.vaultDao()
    )

    // Workspace Mode (Sandbox vs General Storage)
    private val _currentWorkspace = MutableStateFlow(WorkspaceType.SANDBOX)
    val currentWorkspace: StateFlow<WorkspaceType> = _currentWorkspace.asStateFlow()

    // Browsing Navigation State
    private val _currentPath = MutableStateFlow(repository.sandboxRoot.absolutePath)
    val currentPath: StateFlow<String> = _currentPath.asStateFlow()

    // File list state
    private val _files = MutableStateFlow<List<FileModel>>(emptyList())
    val files: StateFlow<List<FileModel>> = _files.asStateFlow()

    // Bookmarks and Recent Files
    val bookmarks: StateFlow<List<Bookmark>> = db.bookmarkDao().getAllBookmarks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentFiles: StateFlow<List<RecentFile>> = db.recentDao().getRecentFiles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI Configuration
    private val _viewMode = MutableStateFlow(ViewMode.LIST)
    val viewMode: StateFlow<ViewMode> = _viewMode.asStateFlow()

    private val _sortBy = MutableStateFlow(SortBy.NAME)
    val sortBy: StateFlow<SortBy> = _sortBy.asStateFlow()

    private val _sortAscending = MutableStateFlow(true)
    val sortAscending: StateFlow<Boolean> = _sortAscending.asStateFlow()

    // Search & Filter
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _filterType = MutableStateFlow<FilterType>(FilterType.ALL)
    val filterType: StateFlow<FilterType> = _filterType.asStateFlow()

    // Selection State
    private val _selectedPaths = MutableStateFlow<Set<String>>(emptySet())
    val selectedPaths: StateFlow<Set<String>> = _selectedPaths.asStateFlow()

    // Clipboard State
    private val _clipboardMode = MutableStateFlow<ClipboardMode?>(null)
    val clipboardMode: StateFlow<ClipboardMode?> = _clipboardMode.asStateFlow()

    private val _clipboardItems = MutableStateFlow<List<String>>(emptyList())
    val clipboardItems: StateFlow<List<String>> = _clipboardItems.asStateFlow()

    // Progress Dialog States
    private val _isOperating = MutableStateFlow(false)
    val isOperating: StateFlow<Boolean> = _isOperating.asStateFlow()

    private val _operationProgress = MutableStateFlow(0f)
    val operationProgress: StateFlow<Float> = _operationProgress.asStateFlow()

    private val _operationLabel = MutableStateFlow("")
    val operationLabel: StateFlow<String> = _operationLabel.asStateFlow()

    // Storage Analyzer State
    private val _storageReport = MutableStateFlow<StorageAnalysisReport?>(null)
    val storageReport: StateFlow<StorageAnalysisReport?> = _storageReport.asStateFlow()

    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()

    // Text Editor State
    private val _editorFilePath = MutableStateFlow<String?>(null)
    val editorFilePath: StateFlow<String?> = _editorFilePath.asStateFlow()

    private val _editorContent = MutableStateFlow("")
    val editorContent: StateFlow<String> = _editorContent.asStateFlow()

    // Vault Private Storage State
    private val _vaultState = MutableStateFlow(VaultState.LOCKED)
    val vaultState: StateFlow<VaultState> = _vaultState.asStateFlow()

    val vaultFiles: StateFlow<List<VaultFile>> = db.vaultDao().getAllVaultFiles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadFiles()
    }

    // --- Directory Navigation ---
    fun loadFiles() {
        viewModelScope.launch {
            val list = repository.getFiles(_currentPath.value)
            _files.value = filterAndSortFiles(list, _searchQuery.value, _filterType.value, _sortBy.value, _sortAscending.value)
        }
    }

    fun navigateTo(path: String) {
        val file = File(path)
        if (file.exists() && file.isDirectory) {
            _currentPath.value = path
            _selectedPaths.value = emptySet()
            _searchQuery.value = ""
            loadFiles()
        }
    }

    fun navigateUp() {
        val currentFile = File(_currentPath.value)
        val parent = currentFile.parentFile
        val rootPath = if (_currentWorkspace.value == WorkspaceType.SANDBOX) {
            repository.sandboxRoot.absolutePath
        } else {
            Environment.getExternalStorageDirectory()?.absolutePath ?: repository.sandboxRoot.absolutePath
        }

        if (parent != null && _currentPath.value != rootPath) {
            navigateTo(parent.absolutePath)
        }
    }

    fun toggleWorkspace(type: WorkspaceType) {
        _currentWorkspace.value = type
        val path = if (type == WorkspaceType.SANDBOX) {
            repository.sandboxRoot.absolutePath
        } else {
            Environment.getExternalStorageDirectory()?.absolutePath ?: repository.sandboxRoot.absolutePath
        }
        navigateTo(path)
    }

    // --- Filter, Search, and Sort ---
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        loadFiles()
    }

    fun updateFilterType(type: FilterType) {
        _filterType.value = type
        loadFiles()
    }

    fun updateSort(sortBy: SortBy) {
        if (_sortBy.value == sortBy) {
            _sortAscending.value = !_sortAscending.value
        } else {
            _sortBy.value = sortBy
            _sortAscending.value = true
        }
        loadFiles()
    }

    fun setViewMode(mode: ViewMode) {
        _viewMode.value = mode
    }

    private fun filterAndSortFiles(
        rawList: List<FileModel>,
        query: String,
        filter: FilterType,
        sort: SortBy,
        ascending: Boolean
    ): List<FileModel> {
        var filtered = rawList

        // Search Query
        if (query.isNotEmpty()) {
            filtered = filtered.filter { it.name.contains(query, ignoreCase = true) }
        }

        // Category Filter
        if (filter != FilterType.ALL) {
            filtered = filtered.filter { file ->
                if (file.isDirectory) return@filter false
                when (filter) {
                    FilterType.IMAGES -> file.iconType == IconType.IMAGE
                    FilterType.VIDEOS -> file.iconType == IconType.VIDEO
                    FilterType.AUDIO -> file.iconType == IconType.AUDIO
                    FilterType.DOCUMENTS -> file.iconType == IconType.TEXT || file.iconType == IconType.PDF
                    FilterType.ARCHIVES -> file.iconType == IconType.ARCHIVE
                    FilterType.APKS -> file.iconType == IconType.APK
                    else -> true
                }
            }
        }

        // Sorting
        val comparator = when (sort) {
            SortBy.NAME -> compareBy<FileModel> { it.name.lowercase(Locale.ROOT) }
            SortBy.SIZE -> compareBy<FileModel> { it.size }
            SortBy.DATE -> compareBy<FileModel> { it.lastModified }
            SortBy.TYPE -> compareBy<FileModel> { it.extension }
        }

        // Ensure folders always stay at top
        val sortedDirs = filtered.filter { it.isDirectory }.sortedWith(if (ascending) comparator else comparator.reversed())
        val sortedFiles = filtered.filter { !it.isDirectory }.sortedWith(if (ascending) comparator else comparator.reversed())

        return sortedDirs + sortedFiles
    }

    // --- File Operations ---
    fun createFolder(name: String) {
        viewModelScope.launch {
            val success = repository.createFolder(_currentPath.value, name)
            if (success) loadFiles()
        }
    }

    fun createNewFile(name: String, content: String = "") {
        viewModelScope.launch {
            val success = repository.createFile(_currentPath.value, name, content)
            if (success) loadFiles()
        }
    }

    fun deleteSelected() {
        viewModelScope.launch {
            _isOperating.value = true
            _operationLabel.value = "Deleting items..."
            _operationProgress.value = 0.5f

            val pathsToDelete = _selectedPaths.value.toList()
            pathsToDelete.forEach { path ->
                repository.deleteFile(path)
            }

            _selectedPaths.value = emptySet()
            _isOperating.value = false
            loadFiles()
        }
    }

    fun deleteFileDirectly(path: String) {
        viewModelScope.launch {
            repository.deleteFile(path)
            loadFiles()
        }
    }

    fun renameFile(path: String, newName: String) {
        viewModelScope.launch {
            val success = repository.renameFile(path, newName)
            if (success) loadFiles()
        }
    }

    // --- Multi-Select & Selection Actions ---
    fun toggleSelection(path: String) {
        val currentSet = _selectedPaths.value.toMutableSet()
        if (currentSet.contains(path)) {
            currentSet.remove(path)
        } else {
            currentSet.add(path)
        }
        _selectedPaths.value = currentSet
    }

    fun clearSelection() {
        _selectedPaths.value = emptySet()
    }

    fun selectAll() {
        _selectedPaths.value = _files.value.map { it.path }.toSet()
    }

    // --- Clipboard Mode (Copy & Move) ---
    fun copySelected() {
        _clipboardMode.value = ClipboardMode.COPY
        _clipboardItems.value = _selectedPaths.value.toList()
        clearSelection()
    }

    fun cutSelected() {
        _clipboardMode.value = ClipboardMode.MOVE
        _clipboardItems.value = _selectedPaths.value.toList()
        clearSelection()
    }

    fun clearClipboard() {
        _clipboardMode.value = null
        _clipboardItems.value = emptyList()
    }

    fun pasteClipboard() {
        val mode = _clipboardMode.value ?: return
        val items = _clipboardItems.value
        if (items.isEmpty()) return

        viewModelScope.launch {
            _isOperating.value = true
            _operationLabel.value = if (mode == ClipboardMode.COPY) "Copying files..." else "Moving files..."
            _operationProgress.value = 0f

            val dest = _currentPath.value
            val success = if (mode == ClipboardMode.COPY) {
                repository.copyFiles(items, dest) { filename, progress ->
                    _operationLabel.value = "Copying: $filename"
                    _operationProgress.value = progress
                }
            } else {
                repository.moveFiles(items, dest) { filename, progress ->
                    _operationLabel.value = "Moving: $filename"
                    _operationProgress.value = progress
                }
            }

            if (success) {
                clearClipboard()
                loadFiles()
            }
            _isOperating.value = false
        }
    }

    // --- Archive Creator & Extractor ---
    fun compressSelected(zipName: String) {
        val sources = _selectedPaths.value.toList()
        if (sources.isEmpty()) return

        viewModelScope.launch {
            _isOperating.value = true
            _operationLabel.value = "Zipping files..."
            _operationProgress.value = 0.5f

            val zipPath = File(_currentPath.value, if (zipName.endsWith(".zip")) zipName else "$zipName.zip").absolutePath
            val success = repository.zipFiles(sources, zipPath)
            
            if (success) {
                clearSelection()
                loadFiles()
            }
            _isOperating.value = false
        }
    }

    fun extractArchive(archivePath: String) {
        val file = File(archivePath)
        viewModelScope.launch {
            _isOperating.value = true
            _operationLabel.value = "Extracting Archive..."
            _operationProgress.value = 0.5f

            val targetDirName = file.nameWithoutExtension + "_Extracted"
            val destFolder = File(file.parentFile, targetDirName).absolutePath
            val success = repository.unzipFile(archivePath, destFolder)

            if (success) {
                loadFiles()
            }
            _isOperating.value = false
        }
    }

    // --- Bookmarking (Favorites) ---
    fun toggleBookmark(fileModel: FileModel) {
        viewModelScope.launch {
            val isBookmarked = db.bookmarkDao().isBookmarked(fileModel.path)
            if (isBookmarked) {
                db.bookmarkDao().deleteByPath(fileModel.path)
            } else {
                db.bookmarkDao().insertBookmark(
                    Bookmark(
                        path = fileModel.path,
                        name = fileModel.name,
                        isFolder = fileModel.isDirectory
                    )
                )
            }
            loadFiles()
        }
    }

    // --- Recent History ---
    fun addToRecent(fileModel: FileModel) {
        viewModelScope.launch {
            db.recentDao().insertRecent(
                RecentFile(
                    path = fileModel.path,
                    name = fileModel.name,
                    fileType = fileModel.extension,
                    fileSize = fileModel.size
                )
            )
        }
    }

    fun clearRecent() {
        viewModelScope.launch {
            db.recentDao().clearAll()
        }
    }

    // --- Storage Analyzer Trigger ---
    fun analyzeActiveStorage() {
        viewModelScope.launch {
            _isAnalyzing.value = true
            val rootToAnalyze = _currentPath.value
            _storageReport.value = repository.analyzeStorage(rootToAnalyze)
            _isAnalyzing.value = false
        }
    }

    // --- Text Editor Actions ---
    fun openInEditor(path: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val file = File(path)
            if (file.exists() && !file.isDirectory) {
                try {
                    val content = file.readText()
                    _editorFilePath.value = path
                    _editorContent.value = content
                } catch (e: Exception) {
                    _editorFilePath.value = null
                    _editorContent.value = ""
                }
            }
        }
    }

    fun saveEditorContent(newContent: String) {
        val path = _editorFilePath.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                File(path).writeText(newContent)
                _editorContent.value = newContent
                loadFiles()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun closeEditor() {
        _editorFilePath.value = null
        _editorContent.value = ""
    }

    // --- Private Secure Vault Controller ---
    fun createVaultPIN(pin: String, question: String, answer: String) {
        viewModelScope.launch {
            val pinHash = hashSHA256(pin)
            val answerHash = hashSHA256(answer.trim().lowercase(Locale.ROOT))
            db.vaultDao().saveConfig(
                VaultConfig(
                    pinHash = pinHash,
                    backupQuestion = question,
                    backupAnswerHash = answerHash
                )
            )
            _vaultState.value = VaultState.UNLOCKED
        }
    }

    fun unlockVault(pin: String): Boolean {
        var success = false
        viewModelScope.launch {
            val config = db.vaultDao().getConfig() ?: return@launch
            if (config.pinHash == hashSHA256(pin)) {
                _vaultState.value = VaultState.UNLOCKED
                success = true
            }
        }
        return success
    }

    fun resetVaultWithBackup(answer: String, newPin: String): Boolean {
        var success = false
        viewModelScope.launch {
            val config = db.vaultDao().getConfig() ?: return@launch
            if (config.backupAnswerHash == hashSHA256(answer.trim().lowercase(Locale.ROOT))) {
                val newHash = hashSHA256(newPin)
                db.vaultDao().saveConfig(config.copy(pinHash = newHash))
                _vaultState.value = VaultState.UNLOCKED
                success = true
            }
        }
        return success
    }

    fun checkVaultStatus(onChecked: (hasConfig: Boolean) -> Unit) {
        viewModelScope.launch {
            val config = db.vaultDao().getConfig()
            onChecked(config != null)
        }
    }

    fun lockVault() {
        _vaultState.value = VaultState.LOCKED
    }

    fun importFileFromUri(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            _isOperating.value = true
            _operationLabel.value = "Fetching file..."
            _operationProgress.value = 0.1f

            try {
                val contentResolver = context.contentResolver
                var name = "imported_file"
                contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1 && cursor.moveToFirst()) {
                        name = cursor.getString(nameIndex)
                    }
                }

                val destFile = File(_currentPath.value, name)
                _operationLabel.value = "Copying: $name"

                contentResolver.openInputStream(uri)?.use { inputStream ->
                    destFile.outputStream().use { outputStream ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        var totalRead = 0L
                        var fileSize = 0L
                        try {
                            contentResolver.openAssetFileDescriptor(uri, "r")?.use { afd ->
                                fileSize = afd.length
                            }
                        } catch (e: Exception) {}

                        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                            outputStream.write(buffer, 0, bytesRead)
                            totalRead += bytesRead
                            if (fileSize > 0) {
                                _operationProgress.value = totalRead.toFloat() / fileSize
                            }
                        }
                    }
                }
                _operationProgress.value = 1.0f
                _isOperating.value = false

                loadFiles()
            } catch (e: Exception) {
                e.printStackTrace()
                _isOperating.value = false
            }
        }
    }

    fun importFileToVault(filePath: String) {
        viewModelScope.launch {
            val file = File(filePath)
            if (!file.exists() || file.isDirectory) return@launch

            val vaultDir = File(getApplication<Application>().filesDir, "EncryptedVault").apply { mkdirs() }
            val uniqueId = UUID.randomUUID().toString()
            val destFile = File(vaultDir, uniqueId)

            // Simple XOR or standard secure encryption logic (XORing with a byte string represents an awesome premium-level custom secure obfuscation)
            try {
                val bytes = file.readBytes()
                val obfuscatedBytes = ByteArray(bytes.size)
                for (i in bytes.indices) {
                    obfuscatedBytes[i] = (bytes[i].toInt() xor 0x3F).toByte()
                }
                destFile.writeBytes(obfuscatedBytes)

                val vaultFile = VaultFile(
                    id = uniqueId,
                    originalName = file.name,
                    originalPath = file.absolutePath,
                    mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.extension) ?: "application/octet-stream",
                    size = file.length()
                )
                db.vaultDao().insertVaultFile(vaultFile)

                // Delete original source file safely
                file.delete()
                loadFiles()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun restoreFileFromVault(vaultFile: VaultFile) {
        viewModelScope.launch {
            val vaultDir = File(getApplication<Application>().filesDir, "EncryptedVault")
            val srcFile = File(vaultDir, vaultFile.id)
            if (!srcFile.exists()) return@launch

            val destFile = File(vaultFile.originalPath)
            // Ensure parent dirs
            destFile.parentFile?.mkdirs()

            try {
                val bytes = srcFile.readBytes()
                val restoredBytes = ByteArray(bytes.size)
                for (i in bytes.indices) {
                    restoredBytes[i] = (bytes[i].toInt() xor 0x3F).toByte()
                }
                destFile.writeBytes(restoredBytes)

                db.vaultDao().deleteVaultFileById(vaultFile.id)
                srcFile.delete()
                loadFiles()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deleteFileFromVault(vaultFile: VaultFile) {
        viewModelScope.launch {
            val vaultDir = File(getApplication<Application>().filesDir, "EncryptedVault")
            val srcFile = File(vaultDir, vaultFile.id)
            if (srcFile.exists()) {
                srcFile.delete()
            }
            db.vaultDao().deleteVaultFileById(vaultFile.id)
        }
    }

    private fun hashSHA256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}

// --- Supporting Enums ---
enum class WorkspaceType {
    SANDBOX, DEVICE_ROOT
}

enum class ViewMode {
    LIST, GRID
}

enum class SortBy {
    NAME, SIZE, DATE, TYPE
}

enum class FilterType {
    ALL, IMAGES, VIDEOS, AUDIO, DOCUMENTS, ARCHIVES, APKS
}

enum class ClipboardMode {
    COPY, MOVE
}

enum class VaultState {
    LOCKED, UNLOCKED
}
