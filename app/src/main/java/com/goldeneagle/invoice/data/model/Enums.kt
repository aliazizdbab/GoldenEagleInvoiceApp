package com.goldeneagle.invoice.data.model

enum class CurrencyCode(
    val symbol: String,
    val arabicName: String,
    val defaultDecimals: Int,
    val majorSingular: String,
    val majorDual: String,
    val majorPlural: String,
    val majorAccusative: String,
    val minorSingular: String,
    val minorDual: String,
    val minorPlural: String,
    val minorAccusative: String,
    val minorFactor: Int
) {
    LYD(
        symbol = "د.ل",
        arabicName = "دينار ليبي",
        defaultDecimals = 3,
        majorSingular = "دينار ليبي",
        majorDual = "ديناران ليبيان",
        majorPlural = "دنانير ليبية",
        majorAccusative = "ديناراً ليبياً",
        minorSingular = "درهم",
        minorDual = "درهمان",
        minorPlural = "دراهم",
        minorAccusative = "درهماً",
        minorFactor = 1000
    ),
    USD(
        symbol = "$",
        arabicName = "دولار أمريكي",
        defaultDecimals = 2,
        majorSingular = "دولار أمريكي",
        majorDual = "دولاران أمريكيان",
        majorPlural = "دولارات أمريكية",
        majorAccusative = "دولاراً أمريكياً",
        minorSingular = "سنت",
        minorDual = "سنتان",
        minorPlural = "سنتات",
        minorAccusative = "سنتاً",
        minorFactor = 100
    ),
    EUR(
        symbol = "€",
        arabicName = "يورو",
        defaultDecimals = 2,
        majorSingular = "يورو",
        majorDual = "اثنان يورو",
        majorPlural = "يورو",
        majorAccusative = "يورو",
        minorSingular = "سنت",
        minorDual = "سنتان",
        minorPlural = "سنتات",
        minorAccusative = "سنتاً",
        minorFactor = 100
    );

    companion object {
        fun fromString(value: String?): CurrencyCode =
            entries.firstOrNull { it.name == value } ?: LYD
    }
}

enum class DecimalMode(val arabicLabel: String) {
    THREE("3 خانات عشرية (5,440.000)"),
    TWO("خانتان عشريتان (5,440.00)"),
    AUTO_BY_CURRENCY("تلقائي حسب العملة (3 للدينار، 2 للدولار/اليورو)"),
    ZERO("بدون كسور عشرية (5,440)");

    fun resolveDecimals(currency: CurrencyCode): Int = when (this) {
        THREE -> 3
        TWO -> 2
        AUTO_BY_CURRENCY -> currency.defaultDecimals
        ZERO -> 0
    }

    companion object {
        fun fromString(value: String?): DecimalMode =
            entries.firstOrNull { it.name == value } ?: THREE
    }
}

enum class DiscountType(val arabicLabel: String) {
    FIXED("مبلغ ثابت"),
    PERCENT("نسبة مئوية (%)");

    companion object {
        fun fromString(value: String?): DiscountType =
            entries.firstOrNull { it.name == value } ?: FIXED
    }
}

enum class LogoStyle(val arabicLabel: String) {
    RED_GOLD("الشعار الأحمر والذهبي (مطابق لـ 26-0101)"),
    FULL_GOLD("الشعار الذهبي الكامل");

    companion object {
        fun fromString(value: String?): LogoStyle =
            entries.firstOrNull { it.name == value } ?: RED_GOLD
    }
}

object SectorPresets {
    val list = listOf("عرض", "فاتورة", "أمر عمل", "صيانة", "خاص")
}

object DefaultNotes {
    val INVOICE_NOTES = listOf(
        "إجمالي الدفع المستحق بعد 30 يوما من الانتهاء من العمل",
        "عند استلامك للفاتورة لا يقبل التعديل عليها بعد يوم",
        "يرجى مراسلتنا عبر الواتس اب لاي تعديلات او تغيرات او تاخر للدفع"
    ).joinToString("\n")

    val QUOTE_NOTES = listOf(
        "صلاحية هذا العرض 7 أيام من تاريخ إصداره",
        "الأسعار قابلة للتغيير حسب توفر الأصناف وسعر الصرف عند الاعتماد",
        "يرجى مراسلتنا عبر الواتس اب للاعتماد أو لأي استفسار أو تعديل"
    ).joinToString("\n")
}
