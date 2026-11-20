package com.goldeneagle.invoice.data.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.goldeneagle.invoice.data.model.CurrencyCode
import com.goldeneagle.invoice.data.model.DecimalMode
import com.goldeneagle.invoice.data.model.DefaultNotes
import com.goldeneagle.invoice.data.model.DiscountType
import com.goldeneagle.invoice.data.model.LogoStyle
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "invoices")
data class InvoiceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val workOrderNumber: String = "",
    val dateText: String = "",
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis(),
    val customerName: String = "",
    val sector: String = "عرض",
    val companyName: String = "",
    val customerPhone: String = "",
    val currencyCode: String = CurrencyCode.LYD.name,
    val taxRatePercent: Double = 0.0,
    val discountType: String = DiscountType.FIXED.name,
    val discountValue: Double = 0.0,
    val notes: String = DefaultNotes.INVOICE_NOTES,
    val showStamp: Boolean = true,
    val showSignature: Boolean = false,
    val signaturePath: String? = null,
    val subtotal: Double = 0.0,
    val taxAmount: Double = 0.0,
    val discountAmount: Double = 0.0,
    val grandTotal: Double = 0.0
)

@Serializable
@Entity(
    tableName = "invoice_items",
    foreignKeys = [
        ForeignKey(
            entity = InvoiceEntity::class,
            parentColumns = ["id"],
            childColumns = ["invoiceId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["invoiceId"])]
)
data class InvoiceItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val invoiceId: Long = 0,
    val position: Int = 0,
    val description: String = "",
    val quantity: Double = 1.0,
    val unitPrice: Double = 0.0
) {
    val lineTotal: Double
        get() = quantity * unitPrice
}

data class InvoiceWithItems(
    @Embedded val invoice: InvoiceEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "invoiceId"
    )
    val items: List<InvoiceItemEntity>
) {
    val sortedItems: List<InvoiceItemEntity>
        get() = items.sortedBy { it.position }
}

@Serializable
@Entity(tableName = "catalog_items")
data class CatalogItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val defaultPrice: Double = 0.0,
    val category: String = "عام",
    val currencyCode: String = CurrencyCode.LYD.name
)

@Serializable
@Entity(tableName = "customers")
data class CustomerEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val companyName: String = "",
    val phone: String = "",
    val notes: String = ""
)

@Serializable
@Entity(tableName = "company_settings")
data class CompanySettingsEntity(
    @PrimaryKey
    val id: Int = 1,
    val cityLine: String = "ليبيا - بنغازي",
    val phone1: String = "(218)926258464",
    val phone2: String = "(218)925940881",
    val website: String = "www.Alnesirelthahabiy.com",
    val payableToTitle: String = "استحقاق الدفع لشركة",
    val payableToCompany: String = "النسر الذهبي",
    val approvalText: String = "أوافق على أن جميع الأعمال قد تم تنفيذها بما يرضيني.",
    val thankYouText: String = "Thank You For Your Business!",
    val logoStyle: String = LogoStyle.RED_GOLD.name,
    val defaultCurrency: String = CurrencyCode.LYD.name,
    val decimalMode: String = DecimalMode.THREE.name,
    val defaultTaxPercent: Double = 0.0,
    val defaultInvoiceNotes: String = DefaultNotes.INVOICE_NOTES,
    val defaultQuoteNotes: String = DefaultNotes.QUOTE_NOTES,
    val defaultSignaturePath: String? = null
)
