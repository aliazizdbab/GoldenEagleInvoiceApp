package com.goldeneagle.invoice.util

import com.goldeneagle.invoice.data.model.CurrencyCode
import kotlin.math.abs
import kotlin.math.roundToLong

object ArabicTafqeet {

    private val ones = arrayOf(
        "",
        "واحد",
        "اثنان",
        "ثلاثة",
        "أربعة",
        "خمسة",
        "ستة",
        "سبعة",
        "ثمانية",
        "تسعة"
    )

    private val teens = arrayOf(
        "عشرة",
        "أحد عشر",
        "اثنا عشر",
        "ثلاثة عشر",
        "أربعة عشر",
        "خمسة عشر",
        "ستة عشر",
        "سبعة عشر",
        "ثمانية عشر",
        "تسعة عشر"
    )

    private val tens = arrayOf(
        "",
        "عشرة",
        "عشرون",
        "ثلاثون",
        "أربعون",
        "خمسون",
        "ستون",
        "سبعون",
        "ثمانون",
        "تسعون"
    )

    private val hundreds = arrayOf(
        "",
        "مئة",
        "مئتان",
        "ثلاث مئة",
        "أربع مئة",
        "خمس مئة",
        "ست مئة",
        "سبع مئة",
        "ثماني مئة",
        "تسع مئة"
    )

    fun toArabicWords(amount: Double, currency: CurrencyCode): String {
        val safeAmount = if (amount.isNaN() || amount.isInfinite()) 0.0 else abs(amount)
        val factor = currency.minorFactor.toLong()
        val totalMinor = (safeAmount * factor).roundToLong()
        val majorPart = totalMinor / factor
        val minorPart = totalMinor % factor

        if (majorPart == 0L && minorPart == 0L) {
            return "صفر ${currency.majorSingular}"
        }

        val parts = mutableListOf<String>()

        if (majorPart > 0L) {
            parts += formatUnit(
                number = majorPart,
                singular = currency.majorSingular,
                dual = currency.majorDual,
                plural = currency.majorPlural,
                accusative = currency.majorAccusative
            )
        }

        if (minorPart > 0L) {
            parts += formatUnit(
                number = minorPart,
                singular = currency.minorSingular,
                dual = currency.minorDual,
                plural = currency.minorPlural,
                accusative = currency.minorAccusative
            )
        }

        return parts.joinToString(" و")
    }

    private fun formatUnit(
        number: Long,
        singular: String,
        dual: String,
        plural: String,
        accusative: String
    ): String {
        val lastTwo = (number % 100L).toInt()
        return when {
            number == 1L -> singular
            number == 2L -> dual
            number in 3L..10L -> "${convertInteger(number)} $plural"
            lastTwo in 3..10 -> "${convertInteger(number)} $plural"
            lastTwo in 11..99 -> "${convertInteger(number)} $accusative"
            else -> "${convertInteger(number)} $singular"
        }
    }

    fun convertInteger(number: Long): String {
        if (number == 0L) return "صفر"

        val groups = mutableListOf<String>()

        val billions = (number / 1_000_000_000L).toInt()
        val millions = ((number % 1_000_000_000L) / 1_000_000L).toInt()
        val thousands = ((number % 1_000_000L) / 1_000L).toInt()
        val remainder = (number % 1_000L).toInt()

        if (billions > 0) {
            groups += formatScale(billions, "مليار", "ملياران", "مليارات", "ملياراً")
        }
        if (millions > 0) {
            groups += formatScale(millions, "مليون", "مليونان", "ملايين", "مليوناً")
        }
        if (thousands > 0) {
            groups += formatScale(thousands, "ألف", "ألفان", "آلاف", "ألفاً")
        }
        if (remainder > 0) {
            groups += convertUnder1000(remainder)
        }

        return groups.joinToString(" و")
    }

    private fun formatScale(
        value: Int,
        singular: String,
        dual: String,
        plural: String,
        accusative: String
    ): String {
        val lastTwo = value % 100
        return when {
            value == 1 -> singular
            value == 2 -> dual
            value in 3..10 -> "${convertUnder1000(value)} $plural"
            lastTwo in 3..10 -> "${convertUnder1000(value)} $plural"
            lastTwo in 11..99 -> "${convertUnder1000(value)} $accusative"
            else -> "${convertUnder1000(value)} $singular"
        }
    }

    private fun convertUnder1000(n: Int): String {
        val h = n / 100
        val rem = n % 100
        val parts = mutableListOf<String>()
        if (h > 0) {
            parts += hundreds[h]
        }
        if (rem > 0) {
            parts += when {
                rem < 10 -> ones[rem]
                rem in 10..19 -> teens[rem - 10]
                else -> {
                    val u = rem % 10
                    val t = rem / 10
                    if (u == 0) tens[t] else "${ones[u]} و${tens[t]}"
                }
            }
        }
        return parts.joinToString(" و")
    }
}
