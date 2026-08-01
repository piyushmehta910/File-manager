package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bookmarks")
data class Bookmark(
    @PrimaryKey val path: String,
    val name: String,
    val isFolder: Boolean,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "recent_files")
data class RecentFile(
    @PrimaryKey val path: String,
    val name: String,
    val timestamp: Long = System.currentTimeMillis(),
    val fileType: String,
    val fileSize: Long
)

@Entity(tableName = "vault_config")
data class VaultConfig(
    @PrimaryKey val id: Int = 1,
    val pinHash: String, // SHA-256 hashed PIN
    val backupQuestion: String,
    val backupAnswerHash: String
)

@Entity(tableName = "vault_files")
data class VaultFile(
    @PrimaryKey val id: String, // Unique encrypted/mapped filename in private space
    val originalName: String,
    val originalPath: String,
    val mimeType: String,
    val size: Long,
    val encryptedAt: Long = System.currentTimeMillis()
)
