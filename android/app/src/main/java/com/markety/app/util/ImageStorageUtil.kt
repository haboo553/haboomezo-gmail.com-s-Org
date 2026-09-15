package com.markety.app.util

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*

object ImageStorageUtil {

    private const val IMAGES_DIR = "product_images"

    /**
     * Get or create the local private directory for storing product photos.
     * Saved in internal storage: persists permanently across app restarts.
     */
    fun getImagesDirectory(context: Context): File {
        val dir = File(context.filesDir, IMAGES_DIR)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * Creates a new destination File and generates a secure FileProvider Uri
     * for passing to ActivityResultContracts.TakePicture()
     */
    fun createTempImageUri(context: Context): Pair<File, Uri> {
        val dir = getImagesDirectory(context)
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val file = File(dir, "MARKETY_IMG_${timeStamp}.jpg")
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        return Pair(file, uri)
    }

    /**
     * Copies an image picked from Gallery Uri to the app's internal private storage
     * so that the path remains permanently accessible even without temporary content Uri grants.
     */
    fun copyUriToLocalStorage(context: Context, sourceUri: Uri): String? {
        return try {
            val dir = getImagesDirectory(context)
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val destFile = File(dir, "MARKETY_IMG_${timeStamp}.jpg")

            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
            destFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Deletes a local image file when a user removes the photo or soft-deletes.
     */
    fun deleteImageFile(filePath: String?): Boolean {
        if (filePath.isNullOrBlank()) return false
        return try {
            val file = File(filePath)
            if (file.exists()) {
                file.delete()
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
