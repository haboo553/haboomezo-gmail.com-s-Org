package com.markety.app.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

object ShareHelper {

    /**
     * Shares a PDF or CSV file. If targetWhatsApp is true, attempts to open WhatsApp directly,
     * otherwise falls back to system chooser.
     */
    fun shareFile(
        context: Context,
        file: File,
        mimeType: String = "application/pdf",
        title: String = "مشاركة المستند",
        targetWhatsApp: Boolean = false
    ) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                if (targetWhatsApp) {
                    setPackage("com.whatsapp")
                }
            }

            val chooser = Intent.createChooser(shareIntent, title).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            // Fallback if WhatsApp is not installed
            if (targetWhatsApp) {
                shareFile(context, file, mimeType, title, targetWhatsApp = false)
            }
        }
    }
}
