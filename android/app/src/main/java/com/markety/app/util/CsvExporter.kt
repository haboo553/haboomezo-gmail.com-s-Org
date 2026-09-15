package com.markety.app.util

import android.content.Context
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets

object CsvExporter {

    /**
     * Writes CSV with UTF-8 BOM so Excel opens Arabic text accurately without character corruption.
     */
    fun exportToCsv(
        context: Context,
        prefix: String,
        headers: List<String>,
        rows: List<List<String>>
    ): File {
        val outputDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(outputDir, "${prefix}_${System.currentTimeMillis()}.csv")

        FileOutputStream(file).use { fos ->
            // Write UTF-8 BOM: EF BB BF
            fos.write(0xEF)
            fos.write(0xBB)
            fos.write(0xBF)

            OutputStreamWriter(fos, StandardCharsets.UTF_8).use { writer ->
                // Write Header
                writer.write(headers.joinToString(",") { escapeCsvCell(it) } + "\n")

                // Write Rows
                for (row in rows) {
                    writer.write(row.joinToString(",") { escapeCsvCell(it) } + "\n")
                }
                writer.flush()
            }
        }

        return file
    }

    private fun escapeCsvCell(cell: String): String {
        var escaped = cell.replace("\"", "\"\"")
        if (escaped.contains(",") || escaped.contains("\n") || escaped.contains("\"")) {
            escaped = "\"$escaped\""
        }
        return escaped
    }
}
