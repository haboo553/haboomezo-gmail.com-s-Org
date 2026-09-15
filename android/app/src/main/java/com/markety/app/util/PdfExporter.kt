package com.markety.app.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.markety.app.data.local.dao.InvoiceWithItems
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object PdfExporter {

    /**
     * Generates a thermal-receipt style PDF (80mm width: ~226 pt x variable height)
     */
    fun exportInvoicePdf(context: Context, invoiceWithItems: InvoiceWithItems): File {
        val invoice = invoiceWithItems.invoice
        val items = invoiceWithItems.items

        val pageWidth = 384 // ~80mm at 120 DPI
        val estimatedHeight = 600 + (items.size * 50)

        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, estimatedHeight, 1).create()
        val page = document.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val paintText = Paint().apply {
            color = Color.BLACK
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }

        val paintTitle = Paint().apply {
            color = Color.BLACK
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }

        val paintBold = Paint().apply {
            color = Color.BLACK
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val paintLine = Paint().apply {
            color = Color.DKGRAY
            strokeWidth = 1.5f
        }

        var y = 40f

        // Header
        canvas.drawText("سوبر ماركت ماركتي", pageWidth / 2f, y, paintTitle)
        y += 25f

        val paintCenterSub = Paint(paintText).apply { textAlign = Paint.Align.CENTER }
        canvas.drawText("فاتورة مبيعات نقدية / ضريبية مبسطة", pageWidth / 2f, y, paintCenterSub)
        y += 20f

        val dateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
        val dateStr = dateFormat.format(Date(invoice.dateTime))

        canvas.drawLine(16f, y, pageWidth - 16f, y, paintLine)
        y += 20f

        canvas.drawText("رقم الفاتورة: ${invoice.invoiceNumber}", 20f, y, paintBold)
        y += 20f
        canvas.drawText("التاريخ: $dateStr", 20f, y, paintText)
        y += 20f
        canvas.drawText("طريقة الدفع: ${if (invoice.paymentType.name == "CASH") "نقدي (كاش)" else "آجل (حساب عميل)"}", 20f, y, paintText)
        y += 20f

        canvas.drawLine(16f, y, pageWidth - 16f, y, paintLine)
        y += 22f

        // Table Header
        canvas.drawText("الصنف", 20f, y, paintBold)
        canvas.drawText("الكمية", 180f, y, paintBold)
        canvas.drawText("السعر", 250f, y, paintBold)
        canvas.drawText("الإجمالي", 310f, y, paintBold)
        y += 12f
        canvas.drawLine(16f, y, pageWidth - 16f, y, paintLine)
        y += 20f

        // Items
        for (item in items) {
            val name = if (item.productNameSnapshot.length > 18) item.productNameSnapshot.take(18) + ".." else item.productNameSnapshot
            canvas.drawText(name, 20f, y, paintText)
            canvas.drawText("${item.quantity}", 185f, y, paintText)
            canvas.drawText(String.format(Locale.US, "%.1f", item.unitSellingPrice), 250f, y, paintText)
            canvas.drawText(String.format(Locale.US, "%.2f", item.total), 310f, y, paintBold)
            y += 24f
        }

        canvas.drawLine(16f, y, pageWidth - 16f, y, paintLine)
        y += 24f

        // Totals
        canvas.drawText("المجموع: ${String.format(Locale.US, "%.2f", invoice.subtotal)} ج.م", 180f, y, paintText)
        y += 22f
        if (invoice.discount > 0) {
            canvas.drawText("الخصم: ${String.format(Locale.US, "%.2f", invoice.discount)} ج.م", 180f, y, paintText)
            y += 22f
        }
        canvas.drawText("الإجمالي النهائي: ${String.format(Locale.US, "%.2f", invoice.total)} ج.م", 160f, y, paintBold)
        y += 22f
        canvas.drawText("المدفوع: ${String.format(Locale.US, "%.2f", invoice.paidAmount)} ج.م", 180f, y, paintText)
        y += 22f

        if (invoice.remainingAmount > 0) {
            val paintRed = Paint(paintBold).apply { color = Color.RED }
            canvas.drawText("المتبقي (آجل): ${String.format(Locale.US, "%.2f", invoice.remainingAmount)} ج.م", 160f, y, paintRed)
            y += 22f
        }

        y += 15f
        canvas.drawLine(16f, y, pageWidth - 16f, y, paintLine)
        y += 25f

        canvas.drawText("شكراً لزيارتكم! يسعدنا خدمتكم دائماً", pageWidth / 2f, y, paintCenterSub)

        document.finishPage(page)

        val outputDir = File(context.cacheDir, "invoices").apply { mkdirs() }
        val file = File(outputDir, "${invoice.invoiceNumber}.pdf")
        FileOutputStream(file).use { out ->
            document.writeTo(out)
        }
        document.close()

        return file
    }

    /**
     * Generates a comprehensive Report PDF (Sales, Profit, Purchases, Debts)
     */
    fun exportReportPdf(
        context: Context,
        reportTitle: String,
        dateRange: String,
        metrics: List<Pair<String, String>>,
        tableHeaders: List<String>,
        tableRows: List<List<String>>
    ): File {
        val pageWidth = 595 // Standard A4 width
        val pageHeight = 842 // Standard A4 height

        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = document.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val paintTitle = Paint().apply {
            color = Color.BLACK
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }

        val paintSub = Paint().apply {
            color = Color.DKGRAY
            textSize = 13f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }

        val paintBold = Paint().apply {
            color = Color.BLACK
            textSize = 13f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val paintText = Paint().apply {
            color = Color.BLACK
            textSize = 12f
            isAntiAlias = true
        }

        val paintLine = Paint().apply {
            color = Color.GRAY
            strokeWidth = 1f
        }

        var y = 50f
        canvas.drawText("ماركتي - تقرير $reportTitle", pageWidth / 2f, y, paintTitle)
        y += 24f
        canvas.drawText("الفترة: $dateRange | تاريخ الاستخراج: ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())}", pageWidth / 2f, y, paintSub)
        y += 25f

        canvas.drawLine(30f, y, pageWidth - 30f, y, paintLine)
        y += 25f

        // Key Metrics Cards
        var metricX = 35f
        for ((label, value) in metrics) {
            canvas.drawText(label, metricX, y, paintBold)
            canvas.drawText(value, metricX, y + 18f, paintText)
            metricX += 135f
            if (metricX > pageWidth - 140f) {
                metricX = 35f
                y += 45f
            }
        }
        y += 45f
        canvas.drawLine(30f, y, pageWidth - 30f, y, paintLine)
        y += 25f

        // Table Header
        if (tableHeaders.isNotEmpty()) {
            val colWidth = (pageWidth - 60f) / tableHeaders.size
            for ((index, header) in tableHeaders.withIndex()) {
                canvas.drawText(header, 35f + (index * colWidth), y, paintBold)
            }
            y += 12f
            canvas.drawLine(30f, y, pageWidth - 30f, y, paintLine)
            y += 20f

            for (row in tableRows) {
                for ((index, cell) in row.withIndex()) {
                    val safeCell = if (cell.length > 20) cell.take(20) + ".." else cell
                    canvas.drawText(safeCell, 35f + (index * colWidth), y, paintText)
                }
                y += 22f
                if (y > pageHeight - 50f) break
            }
        }

        document.finishPage(page)

        val outputDir = File(context.cacheDir, "reports").apply { mkdirs() }
        val filename = "Report_${System.currentTimeMillis()}.pdf"
        val file = File(outputDir, filename)
        FileOutputStream(file).use { out ->
            document.writeTo(out)
        }
        document.close()

        return file
    }
}
