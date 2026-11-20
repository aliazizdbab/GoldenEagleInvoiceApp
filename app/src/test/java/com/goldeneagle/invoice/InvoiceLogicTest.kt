package com.goldeneagle.invoice

import com.goldeneagle.invoice.data.model.CurrencyCode
import com.goldeneagle.invoice.data.model.DecimalMode
import com.goldeneagle.invoice.data.model.DiscountType
import com.goldeneagle.invoice.util.ArabicTafqeet
import com.goldeneagle.invoice.util.CurrencyFormatter
import com.goldeneagle.invoice.util.InvoiceCalculator
import com.goldeneagle.invoice.util.WorkOrderGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class InvoiceLogicTest {

    @Test
    fun tafqeet_5440_LYD() {
        val text = ArabicTafqeet.toArabicWords(5440.0, CurrencyCode.LYD)
        assertEquals("خمسة آلاف وأربع مئة وأربعون ديناراً ليبياً", text)
    }

    @Test
    fun tafqeet_with_dirhams_LYD() {
        val text = ArabicTafqeet.toArabicWords(125.5, CurrencyCode.LYD)
        assertEquals("مئة وخمسة وعشرون ديناراً ليبياً وخمس مئة درهم", text)
    }

    @Test
    fun tafqeet_USD_and_EUR() {
        val usd = ArabicTafqeet.toArabicWords(1200.50, CurrencyCode.USD)
        assertEquals("ألف ومئتان دولار أمريكي وخمسون سنتاً", usd)

        val eur = ArabicTafqeet.toArabicWords(2.05, CurrencyCode.EUR)
        assertEquals("اثنان يورو وخمسة سنتات", eur)
    }

    @Test
    fun tafqeet_zero() {
        assertEquals("صفر دينار ليبي", ArabicTafqeet.toArabicWords(0.0, CurrencyCode.LYD))
    }

    @Test
    fun calculator_exact_26_0101_values() {
        val result = InvoiceCalculator.calculate(
            lineTotals = listOf(6 * 400.0, 2 * 720.0, 2 * 300.0, 100.0, 900.0),
            taxRatePercent = 0.0,
            discountType = DiscountType.FIXED,
            discountValue = 0.0
        )
        assertEquals(5440.0, result.subtotal, 0.001)
        assertEquals(0.0, result.taxAmount, 0.001)
        assertEquals(0.0, result.discountAmount, 0.001)
        assertEquals(5440.0, result.grandTotal, 0.001)
    }

    @Test
    fun calculator_tax_and_percent_discount() {
        val result = InvoiceCalculator.calculate(
            lineTotals = listOf(1000.0),
            taxRatePercent = 10.0,
            discountType = DiscountType.PERCENT,
            discountValue = 10.0
        )
        assertEquals(1000.0, result.subtotal, 0.001)
        assertEquals(100.0, result.taxAmount, 0.001)
        assertEquals(110.0, result.discountAmount, 0.001)
        assertEquals(990.0, result.grandTotal, 0.001)
    }

    @Test
    fun currency_formatting_modes() {
        assertEquals(
            "5,440.000 د.ل",
            CurrencyFormatter.formatMoney(5440.0, CurrencyCode.LYD, DecimalMode.THREE)
        )
        assertEquals(
            "5,440.00 $",
            CurrencyFormatter.formatMoney(5440.0, CurrencyCode.USD, DecimalMode.AUTO_BY_CURRENCY)
        )
        assertEquals(
            "5,440.00 €",
            CurrencyFormatter.formatMoney(5440.0, CurrencyCode.EUR, DecimalMode.AUTO_BY_CURRENCY)
        )
        assertEquals(
            "5,440 د.ل",
            CurrencyFormatter.formatMoney(5440.0, CurrencyCode.LYD, DecimalMode.ZERO)
        )
    }

    @Test
    fun workOrder_auto_increment() {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, 2026)
            set(Calendar.MONTH, Calendar.JANUARY)
            set(Calendar.DAY_OF_MONTH, 17)
        }
        assertEquals("26-0101", WorkOrderGenerator.nextNumber(emptyList(), cal))
        assertEquals(
            "26-0104",
            WorkOrderGenerator.nextNumber(listOf("26-0101", "26-0103", "25-1209"), cal)
        )
        assertTrue(WorkOrderGenerator.nextNumber(listOf("26-0199"), cal) == "26-01100")
    }
}
