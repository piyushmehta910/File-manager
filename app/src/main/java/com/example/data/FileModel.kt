package com.example.data

import java.io.File
import android.webkit.MimeTypeMap
import java.util.Locale

data class FileModel(
    val name: String,
    val path: String,
    val size: Long,
    val isDirectory: Boolean,
    val lastModified: Long,
    val mimeType: String,
    val itemCount: Int = 0,
    val isBookmarked: Boolean = false
) {
    val extension: String
        get() = if (isDirectory) "" else File(path).extension.lowercase(Locale.ROOT)

    val readableSize: String
        get() {
            if (isDirectory) {
                return if (itemCount == 1) "1 item" else "$itemCount items"
            }
            if (size <= 0) return "0 B"
            val units = arrayOf("B", "KB", "MB", "GB", "TB")
            val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
            return String.format(Locale.US, "%.1f %s", size / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
        }

    val iconType: IconType
        get() = when {
            isDirectory -> IconType.FOLDER
            extension == "pdf" -> IconType.PDF
            extension in listOf("jpg", "jpeg", "png", "gif", "bmp", "webp") -> IconType.IMAGE
            extension in listOf("mp4", "mkv", "avi", "mov", "wmv", "flv") -> IconType.VIDEO
            extension in listOf("mp3", "wav", "m4a", "ogg", "flac") -> IconType.AUDIO
            extension in listOf("zip", "rar", "7z", "tar", "gz") -> IconType.ARCHIVE
            extension in listOf("txt", "json", "xml", "html", "md", "js", "kt", "ktm", "java", "css") -> IconType.TEXT
            extension == "apk" -> IconType.APK
            else -> IconType.GENERIC
        }
}

enum class IconType {
    FOLDER, PDF, IMAGE, VIDEO, AUDIO, ARCHIVE, TEXT, APK, GENERIC
}
