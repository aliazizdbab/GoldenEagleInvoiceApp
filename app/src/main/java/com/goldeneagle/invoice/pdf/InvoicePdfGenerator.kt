package com.goldeneagle.invoice.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import com.goldeneagle.invoice.R
import com.goldeneagle.invoice.data.entity.CompanySettingsEntity
import com.goldeneagle.invoice.data.entity.InvoiceEntity
import com.goldeneagle.invoice.data.entity.InvoiceItemEntity
import com.goldeneagle.invoice.data.model.CurrencyCode
import com.goldeneagle.invoice.data.model.DecimalMode
import com.goldeneagle.invoice.data.model.DiscountType
import com.goldeneagle.invoice.data.model.LogoStyle
import com.goldeneagle.invoice.util.ArabicTafqeet
import com.goldeneagle.invoice.util.CurrencyFormatter
import com.goldeneagle.invoice.util.InvoiceCalculator
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max

class InvoicePdfGenerator(private val context: Context) {

    companion object {
        private const val PAGE_WIDTH = 595
        private const val PAGE_HEIGHT = 842

        private const val MARGIN_LEFT = 38f
        private const val MARGIN_RIGHT = 557f
        private const val CONTENT_WIDTH = MARGIN_RIGHT - MARGIN_LEFT

        // Table columns (Left to Right matching RTL 26-0101.pdf)
        private const val COL_TOTAL_L = MARGIN_LEFT
        private const val COL_TOTAL_R = 130f
        private const val COL_PRICE_L = 130f
        private const val COL_PRICE_R = 222f
        private const val COL_QTY_L = 222f
        private const val COL_QTY_R = 268f
        private const val COL_DESC_L = 268f
        private const val COL_DESC_R = 517f
        private const val COL_NUM_L = 517f
        private const val COL_NUM_R = MARGIN_RIGHT

        private const val MIN_ROWS_FIRST_PAGE = 17
        private const val MIN_ROW_HEIGHT = 16.5f
        private const val TABLE_HEADER_HEIGHT = 19f
        private const val PAGE_BREAK_Y = 785f
        private const val SUMMARY_MAX_START_Y = 532f

        private val COLOR_OLIVE_GOLD = Color.parseColor("#87743B")
        private val COLOR_OLIVE_DARK = Color.parseColor("#6E5D2D")
        private val COLOR_GRAY_HEADER = Color.parseColor("#D9D9D9")
        private val COLOR_GRAY_ROW = Color.parseColor("#EFEFEF")
        private val COLOR_GRAY_LIGHT = Color.parseColor("#F7F7F7")
        private val COLOR_BORDER = Color.parseColor("#9A9A9A")
        private val COLOR_DARK_TEXT = Color.parseColor("#111111")
    }

    private data class PreparedRow(
        val rowIndex: Int,
        val item: InvoiceItemEntity?,
        val descLayout: StaticLayout?,
        val rowHeight: Float
    )

    fun generatePdfFile(
        invoice: InvoiceEntity,
        items: List<InvoiceItemEntity>,
        settings: CompanySettingsEntity
    ): File {
        val pdfDir = File(context.cacheDir, "pdfs").apply { mkdirs() }
        val safeOrder = invoice.workOrderNumber
            .ifBlank { "invoice_${invoice.id}" }
            .replace(Regex("[^a-zA-Z0-9\\u0600-\\u06FF_-]"), "_")
        val safeCustomer = invoice.customerName
            .trim()
            .take(24)
            .replace(Regex("[^a-zA-Z0-9\\u0600-\\u06FF_-]"), "_")
        val fileName = if (safeCustomer.isNotBlank()) {
            "${safeOrder}_${safeCustomer}.pdf"
        } else {
            "${safeOrder}.pdf"
        }
        val outputFile = File(pdfDir, fileName)

        val currency = CurrencyCode.fromString(invoice.currencyCode)
        val decimalMode = DecimalMode.fromString(settings.decimalMode)
        val discountType = DiscountType.fromString(invoice.discountType)
        val calc = InvoiceCalculator.calculate(
            lineTotals = items.map { it.lineTotal },
            taxRatePercent = invoice.taxRatePercent,
            discountType = discountType,
            discountValue = invoice.discountValue
        )

        val logoResId = when (LogoStyle.fromString(settings.logoStyle)) {
            LogoStyle.RED_GOLD -> R.drawable.logo_red_gold
            LogoStyle.FULL_GOLD -> R.drawable.logo_full_gold
        }
        val logoBitmap = decodeSampledBitmap(logoResId, 480)
        val headerBitmap = decodeSampledBitmap(R.drawable.header_calligraphy, 1000)
        val stampBitmap = if (invoice.showStamp) decodeSampledBitmap(R.drawable.company_stamp, 600) else null
        val signaturePath = invoice.signaturePath?.takeIf { it.isNotBlank() } ?: settings.defaultSignaturePath
        val signatureBitmap = if (invoice.showSignature && !signaturePath.isNullOrBlank()) {
            File(signaturePath).takeIf { it.exists() }?.let { BitmapFactory.decodeFile(it.absolutePath) }
        } else {
            null
        }

        val descPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_DARK_TEXT
            textSize = 9.2f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }

        val sortedItems = items.sortedBy { it.position }
        val totalRowsCount = max(MIN_ROWS_FIRST_PAGE, sortedItems.size)
        val preparedRows = (0 until totalRowsCount).map { idx ->
            val item = sortedItems.getOrNull(idx)
            val descText = item?.description?.trim().orEmpty()
            val layout = if (descText.isNotEmpty()) {
                buildRtlLayout(
                    text = descText,
                    paint = descPaint,
                    width = (COL_DESC_R - COL_DESC_L - 10f).toInt(),
                    alignment = Layout.Alignment.ALIGN_CENTER
                )
            } else {
                null
            }
            val height = if (layout != null) {
                max(MIN_ROW_HEIGHT, layout.height + 5f)
            } else {
                MIN_ROW_HEIGHT
            }
            PreparedRow(
                rowIndex = idx + 1,
                item = item,
                descLayout = layout,
                rowHeight = height
            )
        }

        val document = PdfDocument()
        try {
            var pageNumber = 1
            var currentPage = document.startPage(
                PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
            )
            var canvas = currentPage.canvas

            var cursorY = drawHeaderAndCustomer(
                canvas = canvas,
                invoice = invoice,
                settings = settings,
                logoBitmap = logoBitmap,
                headerBitmap = headerBitmap,
                includeCustomerBar = true,
                pageNumber = pageNumber
            )
            drawWatermark(canvas, logoBitmap)
            cursorY = drawTableHeader(canvas, cursorY)

            for (row in preparedRows) {
                if (cursorY + row.rowHeight > PAGE_BREAK_Y) {
                    document.finishPage(currentPage)
                    pageNumber++
                    currentPage = document.startPage(
                        PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                    )
                    canvas = currentPage.canvas
                    cursorY = drawHeaderAndCustomer(
                        canvas = canvas,
                        invoice = invoice,
                        settings = settings,
                        logoBitmap = logoBitmap,
                        headerBitmap = headerBitmap,
                        includeCustomerBar = false,
                        pageNumber = pageNumber
                    )
                    drawWatermark(canvas, logoBitmap)
                    cursorY = drawTableHeader(canvas, cursorY)
                }
                drawTableRow(
                    canvas = canvas,
                    topY = cursorY,
                    row = row,
                    currency = currency,
                    decimalMode = decimalMode
                )
                cursorY += row.rowHeight
            }

            if (cursorY > SUMMARY_MAX_START_Y) {
                document.finishPage(currentPage)
                pageNumber++
                currentPage = document.startPage(
                    PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                )
                canvas = currentPage.canvas
                cursorY = drawHeaderAndCustomer(
                    canvas = canvas,
                    invoice = invoice,
                    settings = settings,
                    logoBitmap = logoBitmap,
                    headerBitmap = headerBitmap,
                    includeCustomerBar = false,
                    pageNumber = pageNumber
                )
                drawWatermark(canvas, logoBitmap)
                cursorY += 8f
            }

            val summaryStartY = if (pageNumber == 1) max(cursorY + 8f, 526f) else cursorY + 8f
            drawTotalsAndNotes(
                canvas = canvas,
                startY = summaryStartY,
                invoice = invoice,
                settings = settings,
                currency = currency,
                decimalMode = decimalMode,
                subtotal = calc.subtotal,
                taxAmount = calc.taxAmount,
                discountAmount = calc.discountAmount,
                grandTotal = calc.grandTotal,
                discountType = discountType
            )

            drawFooterAndStamp(
                canvas = canvas,
                settings = settings,
                stampBitmap = stampBitmap,
                signatureBitmap = signatureBitmap
            )

            document.finishPage(currentPage)

            FileOutputStream(outputFile).use { fos ->
                document.writeTo(fos)
            }
        } finally {
            document.close()
            logoBitmap?.recycle()
            headerBitmap?.recycle()
            stampBitmap?.recycle()
            signatureBitmap?.recycle()
        }

        return outputFile
    }

    private fun drawHeaderAndCustomer(
        canvas: Canvas,
        invoice: InvoiceEntity,
        settings: CompanySettingsEntity,
        logoBitmap: Bitmap?,
        headerBitmap: Bitmap?,
        includeCustomerBar: Boolean,
        pageNumber: Int
    ): Float {
        // 1. Top-Right Eagle Logo + Contact Details (Matching 26-0101.pdf)
        if (logoBitmap != null) {
            val logoTargetW = 76f
            val aspect = logoBitmap.height.toFloat() / logoBitmap.width.toFloat()
            val logoTargetH = logoTargetW * aspect
            val logoLeft = 422f
            val logoTop = 26f
            val dst = RectF(logoLeft, logoTop, logoLeft + logoTargetW, logoTop + logoTargetH)
            canvas.drawBitmap(
                logoBitmap,
                Rect(0, 0, logoBitmap.width, logoBitmap.height),
                dst,
                Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
            )
        }

        val contactPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_OLIVE_GOLD
            textSize = 9.2f
            textAlign = Paint.Align.RIGHT
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
        val cityPaint = Paint(contactPaint).apply {
            textSize = 8.8f
        }
        canvas.drawText(settings.cityLine, MARGIN_RIGHT, 97f, cityPaint)

        var contactY = 112f
        if (settings.phone1.isNotBlank()) {
            canvas.drawText("Phone: ${settings.phone1}", MARGIN_RIGHT, contactY, contactPaint)
            contactY += 13.5f
        }
        if (settings.phone2.isNotBlank()) {
            canvas.drawText("Phone: ${settings.phone2}", MARGIN_RIGHT, contactY, contactPaint)
            contactY += 13.5f
        }
        if (settings.website.isNotBlank()) {
            val webPaint = Paint(contactPaint).apply { isUnderlineText = true }
            canvas.drawText(settings.website, MARGIN_RIGHT, contactY, webPaint)
        }

        // 2. Center-Left Calligraphy Header Image
        val barLeft = MARGIN_LEFT
        val barRight = 408f
        if (headerBitmap != null) {
            val targetW = 248f
            val targetH = targetW * (headerBitmap.height.toFloat() / headerBitmap.width.toFloat())
            val centerX = (barLeft + barRight) / 2f
            val dst = RectF(
                centerX - targetW / 2f,
                28f,
                centerX + targetW / 2f,
                28f + targetH
            )
            canvas.drawBitmap(
                headerBitmap,
                Rect(0, 0, headerBitmap.width, headerBitmap.height),
                dst,
                Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
            )
        }

        // 3. DATE / WORK ORDER # Bar
        val metaTop = 126f
        val metaHeaderH = 16.5f
        val metaValH = 18.5f
        val midX = (barLeft + barRight) / 2f

        val fillOlive = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_OLIVE_GOLD
            style = Paint.Style.FILL
        }
        canvas.drawRect(barLeft, metaTop, barRight, metaTop + metaHeaderH, fillOlive)

        val whiteHeaderText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 9.5f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val metaHeaderBaseline = centeredBaseline(metaTop, metaHeaderH, whiteHeaderText)
        canvas.drawText("DATE", (barLeft + midX) / 2f, metaHeaderBaseline, whiteHeaderText)
        canvas.drawText("WORK ORDER #", (midX + barRight) / 2f, metaHeaderBaseline, whiteHeaderText)

        val metaValPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_DARK_TEXT
            textSize = 10f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val metaValBaseline = centeredBaseline(metaTop + metaHeaderH, metaValH, metaValPaint)
        canvas.drawText(invoice.dateText, (barLeft + midX) / 2f, metaValBaseline, metaValPaint)
        canvas.drawText(invoice.workOrderNumber, (midX + barRight) / 2f, metaValBaseline, metaValPaint)

        val subtleLine = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#CCCCCC")
            strokeWidth = 0.6f
        }
        canvas.drawLine(barLeft, metaTop + metaHeaderH + metaValH, barRight, metaTop + metaHeaderH + metaValH, subtleLine)

        if (pageNumber > 1) {
            val pageIndicatorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = COLOR_OLIVE_DARK
                textSize = 8.5f
                textAlign = Paint.Align.LEFT
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            }
            canvas.drawText("صفحة $pageNumber (تابع)", MARGIN_LEFT, 22f, pageIndicatorPaint)
        }

        if (!includeCustomerBar) {
            return metaTop + metaHeaderH + metaValH + 14f
        }

        // 4. Customer Info Bar (اسم الشركة | قطاع | الى السيد)
        val custTop = 175f
        val custHeaderH = 16.5f
        val custValH = 18.5f
        val col1R = 208f
        val col2R = 378f

        val grayHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_GRAY_HEADER
            style = Paint.Style.FILL
        }
        val grayRowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_GRAY_ROW
            style = Paint.Style.FILL
        }
        canvas.drawRect(MARGIN_LEFT, custTop, MARGIN_RIGHT, custTop + custHeaderH, grayHeaderPaint)
        canvas.drawRect(MARGIN_LEFT, custTop + custHeaderH, MARGIN_RIGHT, custTop + custHeaderH + custValH, grayRowPaint)

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_BORDER
            style = Paint.Style.STROKE
            strokeWidth = 0.6f
        }
        // Vertical dividers between the 3 fields
        canvas.drawLine(col1R, custTop, col1R, custTop + custHeaderH + custValH, borderPaint)
        canvas.drawLine(col2R, custTop, col2R, custTop + custHeaderH + custValH, borderPaint)
        // Subtle horizontal divider & bottom line
        canvas.drawLine(MARGIN_LEFT, custTop + custHeaderH, MARGIN_RIGHT, custTop + custHeaderH, subtleLine)

        val custHeaderText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_DARK_TEXT
            textSize = 9.2f
            textAlign = Paint.Align.RIGHT
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val custValText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_DARK_TEXT
            textSize = 9.8f
            textAlign = Paint.Align.RIGHT
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }

        val chBase = centeredBaseline(custTop, custHeaderH, custHeaderText)
        canvas.drawText("اسم الشركة", col1R - 6f, chBase, custHeaderText)
        canvas.drawText("قطاع", col2R - 6f, chBase, custHeaderText)
        canvas.drawText("الى السيد", MARGIN_RIGHT - 6f, chBase, custHeaderText)

        val cvBase = centeredBaseline(custTop + custHeaderH, custValH, custValText)
        if (invoice.companyName.isNotBlank()) {
            canvas.drawText(
                ellipsizeToWidth(invoice.companyName.trim(), custValText, (col1R - MARGIN_LEFT) - 12f),
                col1R - 6f,
                cvBase,
                custValText
            )
        }
        if (invoice.sector.isNotBlank()) {
            canvas.drawText(
                ellipsizeToWidth(invoice.sector.trim(), custValText, (col2R - col1R) - 12f),
                col2R - 6f,
                cvBase,
                custValText
            )
        }
        if (invoice.customerName.isNotBlank()) {
            canvas.drawText(
                ellipsizeToWidth(invoice.customerName.trim(), custValText, (MARGIN_RIGHT - col2R) - 12f),
                MARGIN_RIGHT - 6f,
                cvBase,
                custValText
            )
        }

        return custTop + custHeaderH + custValH + 14f
    }

    private fun drawWatermark(canvas: Canvas, logoBitmap: Bitmap?) {
        if (logoBitmap == null) return
        val wmWidth = 435f
        val wmHeight = wmWidth * (logoBitmap.height.toFloat() / logoBitmap.width.toFloat())
        val left = (PAGE_WIDTH - wmWidth) / 2f
        val top = 310f
        val wmPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            alpha = 26 // ~10% opacity matching 26-0101.xlsx alphaModFix 10000
        }
        canvas.drawBitmap(
            logoBitmap,
            Rect(0, 0, logoBitmap.width, logoBitmap.height),
            RectF(left, top, left + wmWidth, top + wmHeight),
            wmPaint
        )
    }

    private fun drawTableHeader(canvas: Canvas, topY: Float): Float {
        val fillOlive = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_OLIVE_GOLD
            style = Paint.Style.FILL
        }
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_OLIVE_DARK
            style = Paint.Style.STROKE
            strokeWidth = 0.7f
        }
        val rect = RectF(MARGIN_LEFT, topY, MARGIN_RIGHT, topY + TABLE_HEADER_HEIGHT)
        canvas.drawRect(rect, fillOlive)
        canvas.drawRect(rect, borderPaint)

        val headerTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 9.5f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val base = centeredBaseline(topY, TABLE_HEADER_HEIGHT, headerTextPaint)
        canvas.drawText("الاجمالي", (COL_TOTAL_L + COL_TOTAL_R) / 2f, base, headerTextPaint)
        canvas.drawText("السعر", (COL_PRICE_L + COL_PRICE_R) / 2f, base, headerTextPaint)
        canvas.drawText("العدد", (COL_QTY_L + COL_QTY_R) / 2f, base, headerTextPaint)
        canvas.drawText("الوصف", (COL_DESC_L + COL_DESC_R) / 2f, base, headerTextPaint)
        canvas.drawText("ر.م", (COL_NUM_L + COL_NUM_R) / 2f, base, headerTextPaint)

        return topY + TABLE_HEADER_HEIGHT
    }

    private fun drawTableRow(
        canvas: Canvas,
        topY: Float,
        row: PreparedRow,
        currency: CurrencyCode,
        decimalMode: DecimalMode
    ) {
        val bottomY = topY + row.rowHeight
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_BORDER
            style = Paint.Style.STROKE
            strokeWidth = 0.55f
        }

        // Row outer and vertical cell grid lines
        canvas.drawLine(MARGIN_LEFT, bottomY, MARGIN_RIGHT, bottomY, borderPaint)
        val colXs = floatArrayOf(COL_TOTAL_L, COL_PRICE_L, COL_QTY_L, COL_DESC_L, COL_NUM_L, COL_NUM_R)
        for (x in colXs) {
            canvas.drawLine(x, topY, x, bottomY, borderPaint)
        }

        val cellCenterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_DARK_TEXT
            textSize = 9.2f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
        val base = centeredBaseline(topY, row.rowHeight, cellCenterPaint)

        // Row number
        canvas.drawText(row.rowIndex.toString(), (COL_NUM_L + COL_NUM_R) / 2f, base, cellCenterPaint)

        val item = row.item ?: return

        // Description (RTL multi-line centered layout)
        row.descLayout?.let { layout ->
            canvas.save()
            val verticalOffset = (row.rowHeight - layout.height) / 2f
            canvas.translate(COL_DESC_L + 5f, topY + verticalOffset)
            layout.draw(canvas)
            canvas.restore()
        }

        // Quantity
        canvas.drawText(
            CurrencyFormatter.formatQuantity(item.quantity),
            (COL_QTY_L + COL_QTY_R) / 2f,
            base,
            cellCenterPaint
        )

        val decimals = decimalMode.resolveDecimals(currency)
        val currencyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_DARK_TEXT
            textSize = 8.6f
            textAlign = Paint.Align.LEFT
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
        val amountPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_DARK_TEXT
            textSize = 9.2f
            textAlign = Paint.Align.RIGHT
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }

        // Unit price cell (symbol left, number right — exact match to 26-0101.pdf)
        canvas.drawText(currency.symbol, COL_PRICE_L + 5f, base, currencyPaint)
        canvas.drawText(
            CurrencyFormatter.formatNumber(item.unitPrice, decimals),
            COL_PRICE_R - 5f,
            base,
            amountPaint
        )

        // Line total cell
        canvas.drawText(currency.symbol, COL_TOTAL_L + 5f, base, currencyPaint)
        canvas.drawText(
            CurrencyFormatter.formatNumber(item.lineTotal, decimals),
            COL_TOTAL_R - 5f,
            base,
            amountPaint
        )
    }

    private fun drawTotalsAndNotes(
        canvas: Canvas,
        startY: Float,
        invoice: InvoiceEntity,
        settings: CompanySettingsEntity,
        currency: CurrencyCode,
        decimalMode: DecimalMode,
        subtotal: Double,
        taxAmount: Double,
        discountAmount: Double,
        grandTotal: Double,
        discountType: DiscountType
    ) {
        val leftX = COL_TOTAL_L
        val midX = COL_PRICE_L
        val rightX = COL_PRICE_R
        val rowH = 16.5f
        val decimals = decimalMode.resolveDecimals(currency)

        val fillDarkGray = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E6E6E6")
            style = Paint.Style.FILL
        }
        val fillLightGray = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_GRAY_LIGHT
            style = Paint.Style.FILL
        }
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_DARK_TEXT
            textSize = 9.2f
            textAlign = Paint.Align.RIGHT
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val valNumPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_DARK_TEXT
            textSize = 9.2f
            textAlign = Paint.Align.RIGHT
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
        val valCurPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_DARK_TEXT
            textSize = 8.6f
            textAlign = Paint.Align.LEFT
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }

        var y = startY

        fun drawSummaryRow(
            label: String,
            valueText: String,
            showCurrency: Boolean,
            bgPaint: Paint?,
            boldValue: Boolean = false
        ) {
            if (bgPaint != null) {
                canvas.drawRect(leftX, y, rightX, y + rowH, bgPaint)
            }
            val base = centeredBaseline(y, rowH, labelPaint)
            canvas.drawText(label, rightX - 6f, base, labelPaint)
            if (showCurrency) {
                canvas.drawText(currency.symbol, leftX + 5f, base, valCurPaint)
            }
            val p = if (boldValue) {
                Paint(valNumPaint).apply { typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD) }
            } else {
                valNumPaint
            }
            canvas.drawText(valueText, midX - 5f, base, p)
            y += rowH
        }

        // Row 1: Subtotal (الاجمالي)
        drawSummaryRow(
            label = "الاجمالي",
            valueText = CurrencyFormatter.formatNumber(subtotal, decimals),
            showCurrency = true,
            bgPaint = fillDarkGray,
            boldValue = true
        )

        // Row 2: Taxable / Tax base (ضرائب)
        drawSummaryRow(
            label = "ضرائب",
            valueText = CurrencyFormatter.formatNumberOrDash(taxAmount, currency, decimalMode),
            showCurrency = true,
            bgPaint = fillLightGray
        )

        // Row 3: Tax Rate (نسبة الضرائب)
        val borderLight = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_BORDER
            style = Paint.Style.STROKE
            strokeWidth = 0.5f
        }
        canvas.drawRect(leftX, y, midX, y + rowH, borderLight)
        drawSummaryRow(
            label = "نسبة الضرائب",
            valueText = CurrencyFormatter.formatPercent(invoice.taxRatePercent),
            showCurrency = false,
            bgPaint = null
        )

        // Row 4: Total Tax (مجموع الضرائب)
        drawSummaryRow(
            label = "مجموع الضرائب",
            valueText = CurrencyFormatter.formatNumberOrDash(taxAmount, currency, decimalMode),
            showCurrency = true,
            bgPaint = fillDarkGray
        )

        // Row 5: Discounts Section Title (الخصومات)
        val discountTitleLabel = if (discountType == DiscountType.PERCENT && invoice.discountValue > 0.0) {
            "الخصومات (${CurrencyFormatter.formatQuantity(invoice.discountValue)}%)"
        } else {
            "الخصومات"
        }
        val titleBase = centeredBaseline(y, rowH, labelPaint)
        canvas.drawText(discountTitleLabel, rightX - 6f, titleBase, labelPaint)
        y += rowH

        // Row 6: Discounts Amount (خصومات)
        drawSummaryRow(
            label = "خصومات",
            valueText = CurrencyFormatter.formatNumberOrDash(discountAmount, currency, decimalMode),
            showCurrency = true,
            bgPaint = fillDarkGray
        )

        // Row 7: TOTAL olive-gold bar
        val totalBarH = 20f
        val fillOlive = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_OLIVE_GOLD
            style = Paint.Style.FILL
        }
        canvas.drawRect(leftX, y, rightX, y + totalBarH, fillOlive)

        val totalLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 10.5f
            textAlign = Paint.Align.RIGHT
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val totalCurPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 9.5f
            textAlign = Paint.Align.LEFT
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val totalNumPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 10.5f
            textAlign = Paint.Align.RIGHT
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val totalBase = centeredBaseline(y, totalBarH, totalLabelPaint)
        canvas.drawText("TOTAL", rightX - 6f, totalBase, totalLabelPaint)
        canvas.drawText(currency.symbol, leftX + 5f, totalBase, totalCurPaint)
        canvas.drawText(CurrencyFormatter.formatNumber(grandTotal, decimals), midX + 18f, totalBase, totalNumPaint)
        y += totalBarH

        // Row 8: Arabic Tafqeet Bar (olive-gold directly below TOTAL)
        val tafqeetText = ArabicTafqeet.toArabicWords(grandTotal, currency)
        val tafqeetPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 8.4f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val tafqeetLayout = buildRtlLayout(
            text = tafqeetText,
            paint = tafqeetPaint,
            width = (rightX - leftX - 10f).toInt(),
            alignment = Layout.Alignment.ALIGN_CENTER
        )
        val tafqeetH = max(17f, tafqeetLayout.height + 6f)
        canvas.drawRect(leftX, y, rightX, y + tafqeetH, fillOlive)
        val thinWhiteLine = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(120, 255, 255, 255)
            strokeWidth = 0.6f
        }
        canvas.drawLine(leftX, y, rightX, y, thinWhiteLine)
        canvas.save()
        canvas.translate(leftX + 5f, y + (tafqeetH - tafqeetLayout.height) / 2f)
        tafqeetLayout.draw(canvas)
        canvas.restore()
        y += tafqeetH + 6f

        // Payable To text below Tafqeet (corrected spelling: استحقاق)
        val payablePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_DARK_TEXT
            textSize = 9.0f
            textAlign = Paint.Align.RIGHT
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        if (settings.payableToTitle.isNotBlank()) {
            canvas.drawText(settings.payableToTitle, rightX - 2f, y + 8f, payablePaint)
            y += 13f
        }
        if (settings.payableToCompany.isNotBlank()) {
            canvas.drawText(settings.payableToCompany, rightX - 2f, y + 8f, payablePaint)
        }

        // --- Right Notes Block (x = 240f..557f) ---
        val notesLeft = 240f
        val notesRight = MARGIN_RIGHT
        val notesTop = startY + 12f
        val notesHeaderH = 16.5f

        val grayHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_GRAY_HEADER
            style = Paint.Style.FILL
        }
        canvas.drawRect(notesLeft, notesTop, notesRight, notesTop + notesHeaderH, grayHeaderPaint)
        val notesHeaderBase = centeredBaseline(notesTop, notesHeaderH, labelPaint)
        canvas.drawText("ملاحظات", notesRight - 6f, notesHeaderBase, labelPaint)

        val notesTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_DARK_TEXT
            textSize = 8.9f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
        val rawNoteLines = invoice.notes
            .lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        var notesY = notesTop + notesHeaderH + 6f
        val notesWidth = (notesRight - notesLeft - 8f).toInt()
        rawNoteLines.forEachIndexed { idx, line ->
            val formattedLine = if (Regex("^\\d+[.\\-)]\\s*").containsMatchIn(line)) {
                line
            } else {
                "${idx + 1}. $line"
            }
            val layout = buildRtlLayout(
                text = formattedLine,
                paint = notesTextPaint,
                width = notesWidth,
                alignment = Layout.Alignment.ALIGN_NORMAL
            )
            if (notesY + layout.height < 695f) {
                canvas.save()
                canvas.translate(notesLeft + 4f, notesY)
                layout.draw(canvas)
                canvas.restore()
                notesY += layout.height + 3.5f
            }
        }

        // Customer phone line
        val phonePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_DARK_TEXT
            textSize = 9.1f
            textAlign = Paint.Align.RIGHT
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val phoneDisplay = if (invoice.customerPhone.isNotBlank()) {
            "رقم هاتف الزبون: ${invoice.customerPhone.trim()}"
        } else {
            "رقم هاتف الزبون"
        }
        if (notesY + 14f < 710f) {
            canvas.drawText(phoneDisplay, notesRight - 4f, notesY + 11f, phonePaint)
        }
    }

    private fun drawFooterAndStamp(
        canvas: Canvas,
        settings: CompanySettingsEntity,
        stampBitmap: Bitmap?,
        signatureBitmap: Bitmap?
    ) {
        val dividerY = 720f

        // Dashed horizontal separator matching 26-0101.pdf
        val dashedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_BORDER
            style = Paint.Style.STROKE
            strokeWidth = 0.7f
            pathEffect = DashPathEffect(floatArrayOf(3.5f, 2.5f), 0f)
        }
        canvas.drawLine(MARGIN_LEFT, dividerY, MARGIN_RIGHT, dividerY, dashedPaint)

        // Approval statement
        val approvalPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_DARK_TEXT
            textSize = 9.2f
            textAlign = Paint.Align.RIGHT
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
        canvas.drawText(settings.approvalText, MARGIN_RIGHT - 2f, dividerY + 16f, approvalPaint)

        // Signature lines
        val sigLineY = 782f
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_DARK_TEXT
            strokeWidth = 1.1f
        }
        canvas.drawLine(MARGIN_LEFT, sigLineY, COL_PRICE_R, sigLineY, linePaint)
        canvas.drawLine(255f, sigLineY, MARGIN_RIGHT, sigLineY, linePaint)

        // Optional Hand-drawn Signature above right signature line
        if (signatureBitmap != null) {
            val maxW = 140f
            val maxH = 44f
            val scale = minOf(maxW / signatureBitmap.width, maxH / signatureBitmap.height)
            val drawW = signatureBitmap.width * scale
            val drawH = signatureBitmap.height * scale
            val sigLeft = 406f - drawW / 2f
            val sigTop = sigLineY - drawH - 2f
            canvas.drawBitmap(
                signatureBitmap,
                Rect(0, 0, signatureBitmap.width, signatureBitmap.height),
                RectF(sigLeft, sigTop, sigLeft + drawW, sigTop + drawH),
                Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
            )
        }

        // Company Stamp (Project 20251204015824) tilted realistically over the stamp zone
        if (stampBitmap != null) {
            val stampW = 142f
            val stampH = stampW * (stampBitmap.height.toFloat() / stampBitmap.width.toFloat())
            val centerX = 130f
            val centerY = 746f
            val stampPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                alpha = 225
            }
            canvas.save()
            canvas.rotate(-11f, centerX, centerY)
            canvas.drawBitmap(
                stampBitmap,
                Rect(0, 0, stampBitmap.width, stampBitmap.height),
                RectF(
                    centerX - stampW / 2f,
                    centerY - stampH / 2f,
                    centerX + stampW / 2f,
                    centerY + stampH / 2f
                ),
                stampPaint
            )
            canvas.restore()
        }

        // Signature label & Thank You text
        val sigLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_DARK_TEXT
            textSize = 9.0f
            textAlign = Paint.Align.RIGHT
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.ITALIC)
        }
        canvas.drawText("Signature", MARGIN_RIGHT - 2f, sigLineY + 13f, sigLabelPaint)

        val thankYouPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_OLIVE_GOLD
            textSize = 9.6f
            textAlign = Paint.Align.RIGHT
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD_ITALIC)
        }
        canvas.drawText(settings.thankYouText, MARGIN_RIGHT - 2f, sigLineY + 27f, thankYouPaint)
    }

    private fun buildRtlLayout(
        text: String,
        paint: TextPaint,
        width: Int,
        alignment: Layout.Alignment
    ): StaticLayout {
        return StaticLayout.Builder
            .obtain(text, 0, text.length, paint, max(10, width))
            .setAlignment(alignment)
            .setTextDirection(TextDirectionHeuristics.RTL)
            .setLineSpacing(1.5f, 1.0f)
            .setIncludePad(false)
            .build()
    }

    private fun centeredBaseline(topY: Float, height: Float, paint: Paint): Float {
        val fm = paint.fontMetrics
        return topY + (height - (fm.descent - fm.ascent)) / 2f - fm.ascent
    }

    private fun ellipsizeToWidth(text: String, paint: Paint, maxWidth: Float): String {
        if (paint.measureText(text) <= maxWidth) return text
        var end = text.length
        while (end > 1 && paint.measureText(text.substring(0, end) + "…") > maxWidth) {
            end--
        }
        return text.substring(0, end) + "…"
    }

    private fun decodeSampledBitmap(resId: Int, maxDim: Int): Bitmap? {
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeResource(context.resources, resId, opts)
        var inSampleSize = 1
        val maxOrig = max(opts.outWidth, opts.outHeight)
        while (maxOrig / inSampleSize > maxDim * 2) {
            inSampleSize *= 2
        }
        val decodeOpts = BitmapFactory.Options().apply {
            this.inSampleSize = inSampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        return BitmapFactory.decodeResource(context.resources, resId, decodeOpts)
    }
}
