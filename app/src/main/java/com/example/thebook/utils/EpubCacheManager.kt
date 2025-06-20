package com.example.thebook.utils

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import java.io.File

object EpubCacheManager {
    private const val TAG = "EpubCacheManager"
    const val TEMP_EPUB_FILENAME = "temp_current_book.epub"
    const val TEMP_EXTRACT_DIRNAME = "epub_extracted_temp"

    // Get temp epub file
    fun getTempEpubFile(context: Context): File {
        return File(context.cacheDir, TEMP_EPUB_FILENAME)
    }

    // Get temp extract directory name
    fun getTempExtractDir(context: Context): File {
        return File(context.cacheDir, TEMP_EXTRACT_DIRNAME)
    }

    // Check if temp epub file exist
    fun hasTempEpubFile(context: Context): Boolean {
        return getTempEpubFile(context).exists()
    }

    // Check if temp directory exist
    fun hasTempExtractDir(context: Context): Boolean {
        val dir = getTempExtractDir(context)
        return dir.exists() && dir.isDirectory && dir.listFiles()?.isNotEmpty() == true
    }

    // Get temp epub file size
    fun getTempEpubFileSize(context: Context): Long {
        val file = getTempEpubFile(context)
        return file.length()
    }

    // Get temp directory size
    fun getTempExtractDirSize(context: Context): Long {
        val dir = getTempExtractDir(context)
        return calculateDirSize(dir)
    }

    // Get total cache size
    fun getTotalCacheSize(context: Context): Long {
        return getTempEpubFileSize(context) + getTempExtractDirSize(context)
    }


    // Delete temp epub file
    fun clearTempEpubFile(context: Context): Boolean {
        return try {
            val file = getTempEpubFile(context)
            if (file.exists()) {
                val deleted = file.delete()
                if (deleted) {
                    Log.d(TAG, "Cleared temp EPUB file")
                }
                deleted
            } else {
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing temp EPUB file: ${e.message}")
            false
        }
    }

    // Delete temp extract directory
    fun clearTempExtractDir(context: Context): Boolean {
        return try {
            val dir = getTempExtractDir(context)
            if (dir.exists()) {
                val deleted = dir.deleteRecursively()
                if (deleted) {
                    Log.d(TAG, "Cleared temp extract directory")
                }
                deleted
            } else {
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing temp extract directory: ${e.message}")
            false
        }
    }

    // Delete all cache
    fun clearAllCache(context: Context): Boolean {
        val clearedEpub = clearTempEpubFile(context)
        val clearedExtract = clearTempExtractDir(context)

        if (clearedEpub && clearedExtract) {
            Log.d(TAG, "Cleared all EPUB cache")
            return true
        }
        return false
    }

    /**
     * Kiểm tra và cleanup cache nếu quá lớn
     * @param maxSizeBytes: Kích thước tối đa cho phép (bytes)
     */
    fun checkAndCleanupIfNeeded(context: Context, maxSizeBytes: Long = 100 * 1024 * 1024) { // Default 100MB
        val totalSize = getTotalCacheSize(context)
        if (totalSize > maxSizeBytes) {
            Log.d(TAG, "Cache size ($totalSize bytes) exceeds limit ($maxSizeBytes bytes), cleaning up...")
            clearAllCache(context)
        }
    }

    // Get cache information
    fun getCacheInfo(context: Context): CacheInfo {
        return CacheInfo(
            hasEpubFile = hasTempEpubFile(context),
            hasExtractDir = hasTempExtractDir(context),
            epubFileSize = getTempEpubFileSize(context),
            extractDirSize = getTempExtractDirSize(context),
            totalSize = getTotalCacheSize(context)
        )
    }

    // Calculate directory size
    private fun calculateDirSize(dir: File): Long {
        var size = 0L
        try {
            if (dir.exists() && dir.isDirectory) {
                dir.walkTopDown().forEach { file ->
                    if (file.isFile) {
                        size += file.length()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating directory size: ${e.message}")
        }
        return size
    }

    // Information about cache
    data class CacheInfo(
        val hasEpubFile: Boolean,
        val hasExtractDir: Boolean,
        val epubFileSize: Long,
        val extractDirSize: Long,
        val totalSize: Long
    ) {
        fun getFormattedTotalSize(): String {
            return formatFileSize(totalSize)
        }

        fun getFormattedEpubSize(): String {
            return formatFileSize(epubFileSize)
        }

        fun getFormattedExtractSize(): String {
            return formatFileSize(extractDirSize)
        }

        @SuppressLint("DefaultLocale")
        private fun formatFileSize(bytes: Long): String {
            return when {
                bytes < 1024 -> "$bytes B"
                bytes < 1024 * 1024 -> String.format("%.1f KB", bytes / 1024.0)
                bytes < 1024 * 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
                else -> String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0))
            }
        }
    }

}