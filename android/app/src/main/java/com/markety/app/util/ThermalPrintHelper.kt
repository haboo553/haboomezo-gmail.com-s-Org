package com.markety.app.util

import com.markety.app.data.local.dao.InvoiceWithItems
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.*

enum class PaperWidth(val charsPerLine: Int) {
    MM58(32),
    MM80(48)
}

object ThermalPrintHelper {

    // ESC/POS Command Constants
    private val ESC: Byte = 0x1B
    private val GS: Byte = 0x1D
    private val INIT_PRINTER = byteArrayOf(ESC, 0x40)
    private val ALIGN_LEFT = byteArrayOf(ESC, 0x61, 0x00)
    private val ALIGN_CENTER = byteArrayOf(ESC, 0x61, 0x01)
    private val ALIGN_RIGHT = byteArrayOf(ESC, 0x61, 0x02)
    private val BOLD_ON = byteArrayOf(ESC, 0x45, 0x01)
    private val BOLD_OFF = byteArrayOf(ESC, 0x45, 0x00)
    private val FEED_AND_CUT = byteArrayOf(GS, 0x56, 0x41, 0x03)

    fun generateReceiptBytes(invoiceWithItems: InvoiceWithItems, paperWidth: PaperWidth = PaperWidth.MM80): ByteArray {
        val invoice = invoiceWithItems.invoice
        val items = invoiceWithItems.items
        val lineLength = paperWidth.charsPerLine

        val stream = ByteArrayOutputStream()

        stream.write(INIT_PRINTER)

        // Center Header
        stream.write(ALIGN_CENTER)
        stream.write(BOLD_ON)
        stream.write("MARKETY SUPERMARKET\n".toByteArray(Charsets.US_ASCII))
        stream.write("سوبر ماركت ماركتي\n".toByteArray(Charsets.UTF_8))
        stream.write(BOLD_OFF)
        stream.write("--------------------------------\n".take(lineLength).toByteArray(Charsets.US_ASCII))

        // Invoice Meta
        stream.write(ALIGN_LEFT)
        val dateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.US)
        stream.write("Inv: ${invoice.invoiceNumber}\n".toByteArray(Charsets.US_ASCII))
        stream.write("Date: ${dateFormat.format(Date(invoice.dateTime))}\n".toByteArray(Charsets.US_ASCII))
        stream.write("Payment: ${invoice.paymentType.name}\n".toByteArray(Charsets.US_ASCII))
        stream.write("--------------------------------\n".take(lineLength).toByteArray(Charsets.US_ASCII))

        // Items
        for (item in items) {
            val name = if (item.productNameSnapshot.length > 16) item.productNameSnapshot.take(16) else item.productNameSnapshot
            val qtyPrice = "${item.quantity} x ${String.format(Locale.US, "%.1f", item.unitSellingPrice)}"
            val totalStr = String.format(Locale.US, "%.2f", item.total)
            stream.write("$name\n".toByteArray(Charsets.UTF_8))
            stream.write(formatTwoColumns(qtyPrice, totalStr, lineLength).toByteArray(Charsets.UTF_8))
        }

        stream.write("--------------------------------\n".take(lineLength).toByteArray(Charsets.US_ASCII))

        // Totals
        stream.write(formatTwoColumns("Subtotal:", String.format(Locale.US, "%.2f EGP", invoice.subtotal), lineLength).toByteArray(Charsets.UTF_8))
        if (invoice.discount > 0) {
            stream.write(formatTwoColumns("Discount:", String.format(Locale.US, "%.2f EGP", invoice.discount), lineLength).toByteArray(Charsets.UTF_8))
        }
        stream.write(BOLD_ON)
        stream.write(formatTwoColumns("TOTAL:", String.format(Locale.US, "%.2f EGP", invoice.total), lineLength).toByteArray(Charsets.UTF_8))
        stream.write(BOLD_OFF)
        stream.write(formatTwoColumns("Paid:", String.format(Locale.US, "%.2f EGP", invoice.paidAmount), lineLength).toByteArray(Charsets.UTF_8))

        if (invoice.remainingAmount > 0) {
            stream.write(formatTwoColumns("Remaining (Credit):", String.format(Locale.US, "%.2f EGP", invoice.remainingAmount), lineLength).toByteArray(Charsets.UTF_8))
        }

        stream.write("--------------------------------\n".take(lineLength).toByteArray(Charsets.US_ASCII))
        stream.write(ALIGN_CENTER)
        stream.write("شكراً لزيارتكم!\n\n\n".toByteArray(Charsets.UTF_8))

        // Feed & Paper Cut
        stream.write(FEED_AND_CUT)

        return stream.toByteArray()
    }

    private fun formatTwoColumns(left: String, right: String, totalWidth: Int): String {
        val spacesCount = (totalWidth - left.length - right.length).coerceAtLeast(1)
        val spaces = " ".repeat(spacesCount)
        return "$left$spaces$right\n"
    }
}
