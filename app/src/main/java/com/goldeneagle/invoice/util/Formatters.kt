package com.goldeneagle.invoice.util

import com.goldeneagle.invoice.data.model.CurrencyCode
import com.goldeneagle.invoice.data.model.DecimalMode
import com.goldeneagle.invoice.data.model.DiscountType
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.max

data class CalculationResult(
    val subtotal: Double,
    val taxAmount: Double,
    val discountAmount: Double,
    val grandTotal: Double
)

object InvoiceCalculator {
    fun calculate(
        lineTotals: List<Double>,
        taxRatePercent: Double,
        discountType: DiscountType,
        discountValue: Double
    ): CalculationResult {
        val subtotal = lineTotals.sumOf { if (it.isFinite() && it > 0.0) it else 0.0 }
        val safeTaxRate = if (taxRatePercent.isFinite() && taxRatePercent > 0.0) taxRatePercent else 0.0
        val taxAmount = subtotal * (safeTaxRate / 100.0)
        val safeDiscountVal = if (discountValue.isFinite() && discountValue > 0.0) discountValue else 0.0
        val discountAmount = when (discountType) {
            DiscountType.FIXED -> safeDiscountVal
            DiscountType.PERCENT -> (subtotal + taxAmount) * (safeDiscountVal.coerceAtMost(100.0) / 100.0)
        }
        val grandTotal = max(0.0, subtotal + taxAmount - discountAmount)
        return CalculationResult(
            subtotal = subtotal,
            taxAmount = taxAmount,
            discountAmount = discountAmount,
            grandTotal = grandTotal
        )
    }
}

object CurrencyFormatter {
    private val usSymbols = DecimalFormatSymbols(Locale.US)

    fun formatNumber(value: Double, decimals: Int): String {
        val pattern = when (decimals) {
            0 -> "#,##0"
            1 -> "#,##0.0"
            2 -> "#,##0.00"
            else -> "#,##0.000"
        }
        return DecimalFormat(pattern, usSymbols).format(value)
    }

    fun formatMoney(
        value: Double,
        currency: CurrencyCode,
        decimalMode: DecimalMode
    ): String {
        val decimals = decimalMode.resolveDecimals(currency)
        return "${formatNumber(value, decimals)} ${currency.symbol}"
    }

    fun formatNumberOrDash(
        value: Double,
        currency: CurrencyCode,
        decimalMode: DecimalMode
    ): String {
        if (kotlin.math.abs(value) < 0.0001) return "-"
        return formatNumber(value, decimalMode.resolveDecimals(currency))
    }

    fun formatPercent(value: Double): String {
        val safe = if (value.isFinite() && value >= 0.0) value else 0.0
        return "${DecimalFormat("0.000", usSymbols).format(safe)}%"
    }

    fun formatQuantity(value: Double): String {
        val longVal = value.toLong()
        return if (kotlin.math.abs(value - longVal.toDouble()) < 0.0001) {
            longVal.toString()
        } else {
            DecimalFormat("0.##", usSymbols).format(value)
        }
    }
}

object WorkOrderGenerator {
    fun nextNumber(
        existingNumbers: List<String>,
        calendar: Calendar = Calendar.getInstance()
    ): String {
        val yy = (calendar.get(Calendar.YEAR) % 100).toString().padStart(2, '0')
        val mm = (calendar.get(Calendar.MONTH) + 1).toString().padStart(2, '0')
        val prefix = "$yy-$mm"
        val regex = Regex("^${Regex.escape(prefix)}(\\d+)$")
        val maxSeq = existingNumbers.mapNotNull { number ->
            regex.matchEntire(number.trim())?.groupValues?.getOrNull(1)?.toIntOrNull()
        }.maxOrNull() ?: 0
        val nextSeq = (maxSeq + 1).toString().padStart(2, '0')
        return "$prefix$nextSeq"
    }
}

object DateHelper {
    private val formatter = SimpleDateFormat("dd-MM-yyyy", Locale.US)

    fun todayFormatted(): String = formatter.format(Date())

    fun formatMillis(millis: Long): String = formatter.format(Date(millis))
}
