package com.example.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import androidx.core.content.res.ResourcesCompat
import com.example.R
import com.example.data.local.AppLanguage
import com.example.data.local.CustomerEntity
import com.example.data.local.LedgerTransactionEntity
import com.example.data.local.ShopEntity
import java.io.File
import java.io.FileOutputStream

data class CustomerStatementInput(
    val customer: CustomerEntity,
    val transactionsAsc: List<LedgerTransactionEntity>
)

data class CustomerStatementPageModel(
    val customer: CustomerEntity,
    val pageNumberForCustomer: Int,
    val totalPagesForCustomer: Int,
    val isFirstPage: Boolean,
    val transactionsSlice: List<LedgerTransactionEntity>
)

object PrintPdfHelper {

    private const val A4_WIDTH = 595
    private const val A4_HEIGHT = 842
    private const val MARGIN = 36f

    private const val FIRST_PAGE_MAX_ROWS_WITH_NOTE = 15
    private const val FIRST_PAGE_MAX_ROWS_NO_NOTE = 18
    private const val CONTINUATION_PAGE_MAX_ROWS = 26

    fun cleanTempPdfCache(context: Context) {
        runCatching {
            val dir = File(context.cacheDir, "shared_pdfs")
            if (dir.exists()) {
                dir.listFiles()?.forEach { it.delete() }
            }
        }
    }

    /**
     * Builds deterministic A4 pagination per customer so both the Compose A4 preview
     * and the printed PdfDocument match page-for-page without ever mixing customers.
     */
    fun buildMultiPrintPages(
        inputs: List<CustomerStatementInput>,
        commonNote: String,
        includeHistory: Boolean
    ): List<CustomerStatementPageModel> {
        val result = mutableListOf<CustomerStatementPageModel>()
        for (input in inputs) {
            val txns = if (includeHistory) input.transactionsAsc else emptyList()
            if (txns.isEmpty()) {
                result.add(
                    CustomerStatementPageModel(
                        customer = input.customer,
                        pageNumberForCustomer = 1,
                        totalPagesForCustomer = 1,
                        isFirstPage = true,
                        transactionsSlice = emptyList()
                    )
                )
            } else {
                val firstPageCap = if (commonNote.isNotBlank()) {
                    FIRST_PAGE_MAX_ROWS_WITH_NOTE
                } else {
                    FIRST_PAGE_MAX_ROWS_NO_NOTE
                }
                val slices = mutableListOf<List<LedgerTransactionEntity>>()
                val firstSlice = txns.take(firstPageCap)
                slices.add(firstSlice)
                var index = firstSlice.size
                while (index < txns.size) {
                    val nextSlice = txns.subList(index, minOf(index + CONTINUATION_PAGE_MAX_ROWS, txns.size))
                    slices.add(nextSlice)
                    index += nextSlice.size
                }
                slices.forEachIndexed { pageIdx, slice ->
                    result.add(
                        CustomerStatementPageModel(
                            customer = input.customer,
                            pageNumberForCustomer = pageIdx + 1,
                            totalPagesForCustomer = slices.size,
                            isFirstPage = pageIdx == 0,
                            transactionsSlice = slice
                        )
                    )
                }
            }
        }
        return result
    }

    fun printMultiCustomerStatements(
        context: Context,
        shop: ShopEntity,
        inputs: List<CustomerStatementInput>,
        commonNote: String,
        includeHistory: Boolean,
        showAddress: Boolean,
        showPhone: Boolean,
        currencySymbol: String,
        language: AppLanguage
    ) {
        if (inputs.isEmpty()) return
        cleanTempPdfCache(context)
        val pages = buildMultiPrintPages(inputs, commonNote, includeHistory)
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
        val jobName = "${shop.name}_MultiPrint_${inputs.size}_Customers"
        val bengaliTypeface = runCatching {
            ResourcesCompat.getFont(context, R.font.noto_sans_bengali)
        }.getOrNull() ?: Typeface.DEFAULT

        val adapter = object : PrintDocumentAdapter() {
            override fun onLayout(
                oldAttributes: PrintAttributes?,
                newAttributes: PrintAttributes,
                cancellationSignal: CancellationSignal?,
                callback: LayoutResultCallback,
                extras: Bundle?
            ) {
                if (cancellationSignal?.isCanceled == true) {
                    callback.onLayoutCancelled()
                    return
                }
                val info = PrintDocumentInfo.Builder("$jobName.pdf")
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .setPageCount(pages.size)
                    .build()
                callback.onLayoutFinished(info, true)
            }

            override fun onWrite(
                pageRanges: Array<out PageRange>?,
                destination: ParcelFileDescriptor,
                cancellationSignal: CancellationSignal?,
                callback: WriteResultCallback
            ) {
                val pdf = PdfDocument()
                try {
                    pages.forEachIndexed { idx, pageModel ->
                        if (cancellationSignal?.isCanceled == true) {
                            pdf.close()
                            callback.onWriteCancelled()
                            return
                        }
                        val pageInfo = PdfDocument.PageInfo.Builder(A4_WIDTH, A4_HEIGHT, idx + 1).create()
                        val page = pdf.startPage(pageInfo)
                        drawCustomerStatementPage(
                            canvas = page.canvas,
                            typeface = bengaliTypeface,
                            shop = shop,
                            model = pageModel,
                            commonNote = commonNote,
                            includeHistory = includeHistory,
                            showAddress = showAddress,
                            showPhone = showPhone,
                            currencySymbol = currencySymbol,
                            language = language
                        )
                        pdf.finishPage(page)
                    }
                    FileOutputStream(destination.fileDescriptor).use { out ->
                        pdf.writeTo(out)
                    }
                    callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                } catch (e: Exception) {
                    callback.onWriteFailed(e.message)
                } finally {
                    pdf.close()
                }
            }
        }

        val attributes = PrintAttributes.Builder()
            .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
            .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
            .build()
        printManager.print(jobName, adapter, attributes)
    }

    private fun drawCustomerStatementPage(
        canvas: Canvas,
        typeface: Typeface,
        shop: ShopEntity,
        model: CustomerStatementPageModel,
        commonNote: String,
        includeHistory: Boolean,
        showAddress: Boolean,
        showPhone: Boolean,
        currencySymbol: String,
        language: AppLanguage
    ) {
        val navyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#172554")
        }
        val bluePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#2563EB")
        }
        val softBlueBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#EFF6FF")
        }
        val softOrangeBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FFF7ED")
        }
        val orangeBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#EA580C")
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
        }
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#CBD5E1")
            style = Paint.Style.STROKE
            strokeWidth = 0.8f
        }
        val rowAltBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#F8FAFC")
        }

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = Typeface.create(typeface, Typeface.BOLD)
            color = Color.WHITE
            textSize = 18f
        }
        val whiteSubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = typeface
            color = Color.parseColor("#DBEAFE")
            textSize = 9.5f
        }
        val boldDarkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = Typeface.create(typeface, Typeface.BOLD)
            color = Color.parseColor("#0F172A")
            textSize = 11f
        }
        val bodyDarkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = typeface
            color = Color.parseColor("#334155")
            textSize = 10f
        }
        val smallMutedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = typeface
            color = Color.parseColor("#64748B")
            textSize = 8.5f
        }
        val dueValuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = Typeface.create(typeface, Typeface.BOLD)
            color = Color.parseColor("#EA580C")
            textSize = 16f
            textAlign = Paint.Align.RIGHT
        }

        val contentWidth = A4_WIDTH - MARGIN * 2
        var y = MARGIN

        // Top Branded Header
        val headerHeight = if (model.isFirstPage) 72f else 48f
        canvas.drawRoundRect(
            RectF(MARGIN, y, A4_WIDTH - MARGIN, y + headerHeight),
            8f,
            8f,
            navyPaint
        )
        canvas.drawText(shop.name, MARGIN + 14f, y + 24f, titlePaint)
        if (model.isFirstPage) {
            val contactParts = buildList {
                if (showAddress && shop.address.isNotBlank()) add(shop.address)
                if (showPhone && shop.phone.isNotBlank()) add("মোবাইল: ${shop.phone}")
            }.joinToString("  |  ")
            if (contactParts.isNotBlank()) {
                canvas.drawText(contactParts.take(70), MARGIN + 14f, y + 41f, whiteSubPaint)
            }
            canvas.drawText(
                "বিবরণী তারিখ: ${Formatters.formatDate(System.currentTimeMillis(), language)}",
                MARGIN + 14f,
                y + 58f,
                whiteSubPaint
            )
        } else {
            canvas.drawText(
                "${model.customer.name} (${model.customer.customerCode}) - চলমান লেনদেন",
                MARGIN + 14f,
                y + 39f,
                whiteSubPaint
            )
        }

        val pageLabelPaint = Paint(whiteSubPaint).apply { textAlign = Paint.Align.RIGHT }
        canvas.drawText(
            "পৃষ্ঠা ${model.pageNumberForCustomer}/${model.totalPagesForCustomer}",
            A4_WIDTH - MARGIN - 14f,
            y + 24f,
            pageLabelPaint
        )

        y += headerHeight + 14f

        if (model.isFirstPage) {
            // Customer Info Card
            val infoBoxHeight = 68f
            canvas.drawRoundRect(
                RectF(MARGIN, y, A4_WIDTH - MARGIN, y + infoBoxHeight),
                6f,
                6f,
                softBlueBg
            )
            canvas.drawRoundRect(
                RectF(MARGIN, y, A4_WIDTH - MARGIN, y + infoBoxHeight),
                6f,
                6f,
                borderPaint
            )
            canvas.drawText("কাস্টমার: ${model.customer.name}", MARGIN + 12f, y + 20f, boldDarkPaint)
            canvas.drawText("আইডি: ${model.customer.customerCode}", A4_WIDTH - MARGIN - 120f, y + 20f, bodyDarkPaint)
            canvas.drawText("মোবাইল: ${model.customer.phone.ifBlank { "সংরক্ষিত নেই" }}", MARGIN + 12f, y + 38f, bodyDarkPaint)
            val addr = model.customer.address.ifBlank { "ঠিকানা সংরক্ষিত নেই" }
            canvas.drawText("ঠিকানা: ${addr.take(65)}", MARGIN + 12f, y + 55f, bodyDarkPaint)

            y += infoBoxHeight + 12f

            // Common Note (if provided)
            if (commonNote.isNotBlank()) {
                val noteLines = wrapText(commonNote, bodyDarkPaint, contentWidth - 24f).take(3)
                val noteBoxHeight = 20f + noteLines.size * 14f
                canvas.drawRoundRect(
                    RectF(MARGIN, y, A4_WIDTH - MARGIN, y + noteBoxHeight),
                    6f,
                    6f,
                    rowAltBg
                )
                canvas.drawRoundRect(
                    RectF(MARGIN, y, A4_WIDTH - MARGIN, y + noteBoxHeight),
                    6f,
                    6f,
                    borderPaint
                )
                noteLines.forEachIndexed { i, line ->
                    canvas.drawText(line, MARGIN + 12f, y + 18f + i * 14f, bodyDarkPaint)
                }
                y += noteBoxHeight + 12f
            }

            // Total Due Highlight Box (NO separate accounting summary table!)
            val dueBoxHeight = 46f
            canvas.drawRoundRect(
                RectF(MARGIN, y, A4_WIDTH - MARGIN, y + dueBoxHeight),
                6f,
                6f,
                softOrangeBg
            )
            canvas.drawRoundRect(
                RectF(MARGIN, y, A4_WIDTH - MARGIN, y + dueBoxHeight),
                6f,
                6f,
                orangeBorder
            )
            val dueTitlePaint = Paint(boldDarkPaint).apply { textSize = 13f }
            canvas.drawText("সর্বমোট পাওনা:", MARGIN + 14f, y + 28f, dueTitlePaint)
            canvas.drawText(
                Formatters.formatMoney(model.customer.currentDue, currencySymbol),
                A4_WIDTH - MARGIN - 14f,
                y + 29f,
                dueValuePaint
            )
            y += dueBoxHeight + 16f
        }

        // Optional Complete Transaction History (ONLY when includeHistory == true)
        if (includeHistory && model.transactionsSlice.isNotEmpty()) {
            canvas.drawText("লেনদেনের বিবরণ (প্রথম থেকে সর্বশেষ)", MARGIN, y, boldDarkPaint)
            y += 10f

            // Table Header
            val tableHeaderHeight = 24f
            canvas.drawRect(MARGIN, y, A4_WIDTH - MARGIN, y + tableHeaderHeight, bluePaint)
            val thPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.typeface = Typeface.create(typeface, Typeface.BOLD)
                color = Color.WHITE
                textSize = 9.5f
            }
            val colDateX = MARGIN + 8f
            val colTypeX = MARGIN + 105f
            val colNoteX = MARGIN + 215f
            val colAmtX = A4_WIDTH - MARGIN - 8f

            canvas.drawText("তারিখ", colDateX, y + 16f, thPaint)
            canvas.drawText("ধরন", colTypeX, y + 16f, thPaint)
            canvas.drawText("বিবরণ / নোট", colNoteX, y + 16f, thPaint)
            val thRight = Paint(thPaint).apply { textAlign = Paint.Align.RIGHT }
            canvas.drawText("পরিমাণ", colAmtX, y + 16f, thRight)
            y += tableHeaderHeight

            val rowHeight = 22f
            val amtPaint = Paint(bodyDarkPaint).apply { textAlign = Paint.Align.RIGHT }
            model.transactionsSlice.forEachIndexed { idx, txn ->
                if (idx % 2 == 1) {
                    canvas.drawRect(MARGIN, y, A4_WIDTH - MARGIN, y + rowHeight, rowAltBg)
                }
                canvas.drawLine(MARGIN, y + rowHeight, A4_WIDTH - MARGIN, y + rowHeight, borderPaint)
                val textY = y + 15f
                canvas.drawText(Formatters.formatDate(txn.transactionDate, language), colDateX, textY, bodyDarkPaint)
                canvas.drawText(Formatters.transactionTypeLabel(txn.type, language), colTypeX, textY, bodyDarkPaint)
                val notePreview = txn.note.ifBlank { "-" }.take(28)
                canvas.drawText(notePreview, colNoteX, textY, bodyDarkPaint)
                canvas.drawText(Formatters.formatMoney(txn.amount, currencySymbol), colAmtX, textY, amtPaint)
                y += rowHeight
            }
        }

        // Footer
        canvas.drawText(
            "Talikhata • ${shop.name}",
            MARGIN,
            A4_HEIGHT - 20f,
            smallMutedPaint
        )
    }

    fun printSingleReceipt(
        context: Context,
        shop: ShopEntity,
        txn: LedgerTransactionEntity,
        currencySymbol: String,
        language: AppLanguage
    ) {
        cleanTempPdfCache(context)
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
        val jobName = "Receipt_${txn.receiptNumber}"
        val typeface = runCatching {
            ResourcesCompat.getFont(context, R.font.noto_sans_bengali)
        }.getOrNull() ?: Typeface.DEFAULT

        val adapter = object : PrintDocumentAdapter() {
            override fun onLayout(
                oldAttributes: PrintAttributes?,
                newAttributes: PrintAttributes,
                cancellationSignal: CancellationSignal?,
                callback: LayoutResultCallback,
                extras: Bundle?
            ) {
                if (cancellationSignal?.isCanceled == true) {
                    callback.onLayoutCancelled()
                    return
                }
                val info = PrintDocumentInfo.Builder("$jobName.pdf")
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .setPageCount(1)
                    .build()
                callback.onLayoutFinished(info, true)
            }

            override fun onWrite(
                pageRanges: Array<out PageRange>?,
                destination: ParcelFileDescriptor,
                cancellationSignal: CancellationSignal?,
                callback: WriteResultCallback
            ) {
                val pdf = PdfDocument()
                try {
                    val page = pdf.startPage(PdfDocument.PageInfo.Builder(A4_WIDTH, A4_HEIGHT, 1).create())
                    drawReceiptPage(page.canvas, typeface, shop, txn, currencySymbol, language)
                    pdf.finishPage(page)
                    FileOutputStream(destination.fileDescriptor).use { pdf.writeTo(it) }
                    callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                } catch (e: Exception) {
                    callback.onWriteFailed(e.message)
                } finally {
                    pdf.close()
                }
            }
        }

        printManager.print(
            jobName,
            adapter,
            PrintAttributes.Builder().setMediaSize(PrintAttributes.MediaSize.ISO_A4).build()
        )
    }

    fun createTempReceiptPdfForShare(
        context: Context,
        shop: ShopEntity,
        txn: LedgerTransactionEntity,
        currencySymbol: String,
        language: AppLanguage
    ): File? {
        cleanTempPdfCache(context)
        val dir = File(context.cacheDir, "shared_pdfs")
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, "receipt_${txn.receiptNumber}.pdf")
        val typeface = runCatching {
            ResourcesCompat.getFont(context, R.font.noto_sans_bengali)
        }.getOrNull() ?: Typeface.DEFAULT
        val pdf = PdfDocument()
        return try {
            val page = pdf.startPage(PdfDocument.PageInfo.Builder(A4_WIDTH, A4_HEIGHT, 1).create())
            drawReceiptPage(page.canvas, typeface, shop, txn, currencySymbol, language)
            pdf.finishPage(page)
            FileOutputStream(file).use { pdf.writeTo(it) }
            file
        } catch (_: Exception) {
            null
        } finally {
            pdf.close()
        }
    }

    fun exportReceiptPdfToUri(
        context: Context,
        uri: Uri,
        shop: ShopEntity,
        txn: LedgerTransactionEntity,
        currencySymbol: String,
        language: AppLanguage
    ): Boolean {
        val typeface = runCatching {
            ResourcesCompat.getFont(context, R.font.noto_sans_bengali)
        }.getOrNull() ?: Typeface.DEFAULT
        val pdf = PdfDocument()
        return try {
            val page = pdf.startPage(PdfDocument.PageInfo.Builder(A4_WIDTH, A4_HEIGHT, 1).create())
            drawReceiptPage(page.canvas, typeface, shop, txn, currencySymbol, language)
            pdf.finishPage(page)
            context.contentResolver.openOutputStream(uri)?.use { out ->
                pdf.writeTo(out)
            } ?: return false
            true
        } catch (_: Exception) {
            false
        } finally {
            pdf.close()
        }
    }

    private fun drawReceiptPage(
        canvas: Canvas,
        typeface: Typeface,
        shop: ShopEntity,
        txn: LedgerTransactionEntity,
        currencySymbol: String,
        language: AppLanguage
    ) {
        val navyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#172554") }
        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#F8FAFC") }
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#CBD5E1")
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = Typeface.create(typeface, Typeface.BOLD)
            color = Color.WHITE
            textSize = 20f
        }
        val subWhite = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = typeface
            color = Color.parseColor("#DBEAFE")
            textSize = 10f
        }
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = typeface
            color = Color.parseColor("#475569")
            textSize = 11f
        }
        val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = Typeface.create(typeface, Typeface.BOLD)
            color = Color.parseColor("#0F172A")
            textSize = 12f
            textAlign = Paint.Align.RIGHT
        }

        var y = MARGIN
        canvas.drawRoundRect(RectF(MARGIN, y, A4_WIDTH - MARGIN, y + 78f), 10f, 10f, navyPaint)
        canvas.drawText(shop.name, MARGIN + 18f, y + 28f, titlePaint)
        canvas.drawText("লেনদেন রসিদ  •  ${txn.receiptNumber}", MARGIN + 18f, y + 48f, subWhite)
        if (shop.phone.isNotBlank()) {
            canvas.drawText("ফোন: ${shop.phone}", MARGIN + 18f, y + 64f, subWhite)
        }

        y += 96f
        canvas.drawRoundRect(RectF(MARGIN, y, A4_WIDTH - MARGIN, y + 260f), 10f, 10f, cardPaint)
        canvas.drawRoundRect(RectF(MARGIN, y, A4_WIDTH - MARGIN, y + 260f), 10f, 10f, borderPaint)

        val rows = listOf(
            "রসিদ নম্বর" to txn.receiptNumber,
            "আইডি" to txn.partyCode,
            "নাম" to txn.partyName,
            "মোবাইল" to txn.partyPhone.ifBlank { "-" },
            "তারিখ" to Formatters.formatDateTime(txn.transactionDate, language),
            "লেনদেনের ধরন" to Formatters.transactionTypeLabel(txn.type, language),
            "পূর্বের ব্যালেন্স" to Formatters.formatMoney(txn.previousBalance, currencySymbol),
            "পরিমাণ" to Formatters.formatMoney(txn.amount, currencySymbol),
            "নতুন ব্যালেন্স" to Formatters.formatMoney(txn.newBalance, currencySymbol),
            "নোট" to txn.note.ifBlank { "-" }
        )
        var ry = y + 24f
        rows.forEach { (k, v) ->
            canvas.drawText(k, MARGIN + 16f, ry, labelPaint)
            canvas.drawText(v.take(36), A4_WIDTH - MARGIN - 16f, ry, valuePaint)
            ry += 24f
        }
    }

    fun printReportSummary(
        context: Context,
        shop: ShopEntity,
        reportTitle: String,
        rows: List<Pair<String, String>>
    ) {
        cleanTempPdfCache(context)
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
        val typeface = runCatching {
            ResourcesCompat.getFont(context, R.font.noto_sans_bengali)
        }.getOrNull() ?: Typeface.DEFAULT

        val adapter = object : PrintDocumentAdapter() {
            override fun onLayout(
                oldAttributes: PrintAttributes?,
                newAttributes: PrintAttributes,
                cancellationSignal: CancellationSignal?,
                callback: LayoutResultCallback,
                extras: Bundle?
            ) {
                if (cancellationSignal?.isCanceled == true) {
                    callback.onLayoutCancelled()
                    return
                }
                val chunked = rows.chunked(26).ifEmpty { listOf(emptyList()) }
                val info = PrintDocumentInfo.Builder("Report.pdf")
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .setPageCount(chunked.size)
                    .build()
                callback.onLayoutFinished(info, true)
            }

            override fun onWrite(
                pageRanges: Array<out PageRange>?,
                destination: ParcelFileDescriptor,
                cancellationSignal: CancellationSignal?,
                callback: WriteResultCallback
            ) {
                val pdf = PdfDocument()
                try {
                    val chunks = rows.chunked(26).ifEmpty { listOf(emptyList()) }
                    val teal = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#0F766E") }
                    val titleP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        this.typeface = Typeface.create(typeface, Typeface.BOLD)
                        color = Color.WHITE
                        textSize = 16f
                    }
                    val leftP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        this.typeface = typeface
                        color = Color.parseColor("#0F172A")
                        textSize = 10.5f
                    }
                    val rightP = Paint(leftP).apply {
                        this.typeface = Typeface.create(typeface, Typeface.BOLD)
                        textAlign = Paint.Align.RIGHT
                    }
                    val lineP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.parseColor("#CBD5E1")
                        strokeWidth = 0.8f
                    }

                    chunks.forEachIndexed { idx, slice ->
                        val page = pdf.startPage(PdfDocument.PageInfo.Builder(A4_WIDTH, A4_HEIGHT, idx + 1).create())
                        val c = page.canvas
                        c.drawRoundRect(RectF(MARGIN, MARGIN, A4_WIDTH - MARGIN, MARGIN + 56f), 8f, 8f, teal)
                        c.drawText("${shop.name} — $reportTitle", MARGIN + 14f, MARGIN + 34f, titleP)
                        var ry = MARGIN + 82f
                        slice.forEach { (label, valText) ->
                            c.drawText(label.take(45), MARGIN + 6f, ry, leftP)
                            c.drawText(valText.take(30), A4_WIDTH - MARGIN - 6f, ry, rightP)
                            c.drawLine(MARGIN, ry + 7f, A4_WIDTH - MARGIN, ry + 7f, lineP)
                            ry += 24f
                        }
                        pdf.finishPage(page)
                    }
                    FileOutputStream(destination.fileDescriptor).use { pdf.writeTo(it) }
                    callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                } catch (e: Exception) {
                    callback.onWriteFailed(e.message)
                } finally {
                    pdf.close()
                }
            }
        }
        printManager.print(
            reportTitle,
            adapter,
            PrintAttributes.Builder().setMediaSize(PrintAttributes.MediaSize.ISO_A4).build()
        )
    }

    private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
        val words = text.split(Regex("\\s+"))
        val lines = mutableListOf<String>()
        var current = StringBuilder()
        for (word in words) {
            val candidate = if (current.isEmpty()) word else "$current $word"
            if (paint.measureText(candidate) <= maxWidth) {
                current = StringBuilder(candidate)
            } else {
                if (current.isNotEmpty()) lines.add(current.toString())
                current = StringBuilder(word)
            }
        }
        if (current.isNotEmpty()) lines.add(current.toString())
        return lines
    }
}
