package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks ORDER BY addedAt DESC")
    fun getAllBookmarks(): Flow<List<Bookmark>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: Bookmark)

    @Delete
    suspend fun deleteBookmark(bookmark: Bookmark)

    @Query("DELETE FROM bookmarks WHERE path = :path")
    suspend fun deleteByPath(path: String)

    @Query("SELECT EXISTS(SELECT 1 FROM bookmarks WHERE path = :path LIMIT 1)")
    suspend fun isBookmarked(path: String): Boolean
}

@Dao
interface RecentDao {
    @Query("SELECT * FROM recent_files ORDER BY timestamp DESC LIMIT 30")
    fun getRecentFiles(): Flow<List<RecentFile>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecent(recent: RecentFile)

    @Query("DELETE FROM recent_files WHERE path = :path")
    suspend fun deleteRecentByPath(path: String)

    @Query("DELETE FROM recent_files")
    suspend fun clearAll()
}

@Dao
interface VaultDao {
    @Query("SELECT * FROM vault_config WHERE id = 1 LIMIT 1")
    suspend fun getConfig(): VaultConfig?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveConfig(config: VaultConfig)

    @Query("SELECT * FROM vault_files ORDER BY encryptedAt DESC")
    fun getAllVaultFiles(): Flow<List<VaultFile>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVaultFile(vaultFile: VaultFile)

    @Query("DELETE FROM vault_files WHERE id = :id")
    suspend fun deleteVaultFileById(id: String)
}
