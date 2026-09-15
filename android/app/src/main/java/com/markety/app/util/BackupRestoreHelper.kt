package com.markety.app.util

import android.content.Context
import android.net.Uri
import com.markety.app.data.local.MarketyDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object BackupRestoreHelper {

    /**
     * Creates a compressed ZIP backup containing the SQLite database, -wal, and -shm files.
     */
    suspend fun createBackup(context: Context, database: MarketyDatabase): Result<File> = withContext(Dispatchers.IO) {
        try {
            // Checkpoint database to flush WAL into main DB file
            database.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").close()

            val dbFile = context.getDatabasePath("markety_database")
            val walFile = File(dbFile.path + "-wal")
            val shmFile = File(dbFile.path + "-shm")

            val backupDir = File(context.filesDir, "backups").apply { mkdirs() }
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val zipFile = File(backupDir, "Markety_Backup_$timeStamp.mbak")

            ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
                if (dbFile.exists()) {
                    addFileToZip(zos, dbFile, "markety_database")
                }
                if (walFile.exists()) {
                    addFileToZip(zos, walFile, "markety_database-wal")
                }
                if (shmFile.exists()) {
                    addFileToZip(zos, shmFile, "markety_database-shm")
                }
            }

            Result.success(zipFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Restores the database from a backup file.
     * Note: Must be invoked when no write operations are active.
     */
    suspend fun restoreBackup(context: Context, database: MarketyDatabase, backupFile: File): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Close database before replacing files
            database.close()

            val dbFile = context.getDatabasePath("markety_database")
            val walFile = File(dbFile.path + "-wal")
            val shmFile = File(dbFile.path + "-shm")

            // Delete existing WAL/SHM to prevent conflict
            if (walFile.exists()) walFile.delete()
            if (shmFile.exists()) shmFile.delete()

            ZipInputStream(FileInputStream(backupFile)).use { zis ->
                var entry: ZipEntry? = zis.nextEntry
                while (entry != null) {
                    val targetFile = when (entry.name) {
                        "markety_database" -> dbFile
                        "markety_database-wal" -> walFile
                        "markety_database-shm" -> shmFile
                        else -> null
                    }

                    if (targetFile != null) {
                        FileOutputStream(targetFile).use { fos ->
                            zis.copyTo(fos)
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun addFileToZip(zos: ZipOutputStream, file: File, entryName: String) {
        FileInputStream(file).use { fis ->
            val entry = ZipEntry(entryName)
            zos.putNextEntry(entry)
            fis.copyTo(zos)
            zos.closeEntry()
        }
    }
}
