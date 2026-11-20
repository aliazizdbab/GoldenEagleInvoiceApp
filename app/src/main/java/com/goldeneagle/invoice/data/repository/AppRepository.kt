package com.goldeneagle.invoice.data.repository

import androidx.room.withTransaction
import com.goldeneagle.invoice.data.db.AppDatabase
import com.goldeneagle.invoice.data.entity.CatalogItemEntity
import com.goldeneagle.invoice.data.entity.CompanySettingsEntity
import com.goldeneagle.invoice.data.entity.CustomerEntity
import com.goldeneagle.invoice.data.entity.InvoiceEntity
import com.goldeneagle.invoice.data.entity.InvoiceItemEntity
import com.goldeneagle.invoice.data.entity.InvoiceWithItems
import com.goldeneagle.invoice.util.WorkOrderGenerator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class BackupInvoiceRecord(
    val invoice: InvoiceEntity,
    val items: List<InvoiceItemEntity>
)

@Serializable
data class BackupPayload(
    val version: Int = 1,
    val exportedAtMillis: Long = System.currentTimeMillis(),
    val settings: CompanySettingsEntity = CompanySettingsEntity(),
    val catalog: List<CatalogItemEntity> = emptyList(),
    val customers: List<CustomerEntity> = emptyList(),
    val invoices: List<BackupInvoiceRecord> = emptyList()
)

class AppRepository(private val db: AppDatabase) {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun observeInvoices(): Flow<List<InvoiceWithItems>> =
        db.invoiceDao().observeAllInvoicesWithItems()

    suspend fun getInvoiceWithItems(id: Long): InvoiceWithItems? =
        db.invoiceDao().getInvoiceWithItems(id)

    suspend fun generateNextWorkOrderNumber(): String {
        val existing = db.invoiceDao().getAllWorkOrderNumbers()
        return WorkOrderGenerator.nextNumber(existing)
    }

    suspend fun saveInvoice(
        invoice: InvoiceEntity,
        items: List<InvoiceItemEntity>,
        autoSaveCustomer: Boolean = true
    ): Long {
        val now = System.currentTimeMillis()
        val toSave = invoice.copy(
            createdAtMillis = if (invoice.id == 0L) now else invoice.createdAtMillis,
            updatedAtMillis = now
        )
        val savedId = db.invoiceDao().saveInvoiceWithItems(toSave, items)

        if (autoSaveCustomer && invoice.customerName.isNotBlank()) {
            val existing = db.customerDao().findByName(invoice.customerName.trim())
            if (existing == null) {
                db.customerDao().upsert(
                    CustomerEntity(
                        name = invoice.customerName.trim(),
                        companyName = invoice.companyName.trim(),
                        phone = invoice.customerPhone.trim()
                    )
                )
            } else {
                val updatedCompany = invoice.companyName.trim().ifBlank { existing.companyName }
                val updatedPhone = invoice.customerPhone.trim().ifBlank { existing.phone }
                if (updatedCompany != existing.companyName || updatedPhone != existing.phone) {
                    db.customerDao().upsert(
                        existing.copy(companyName = updatedCompany, phone = updatedPhone)
                    )
                }
            }
        }
        return savedId
    }

    suspend fun duplicateInvoice(sourceId: Long, newSector: String? = null): Long? {
        val source = db.invoiceDao().getInvoiceWithItems(sourceId) ?: return null
        val nextNum = generateNextWorkOrderNumber()
        val now = System.currentTimeMillis()
        val copyInvoice = source.invoice.copy(
            id = 0L,
            workOrderNumber = nextNum,
            sector = newSector ?: source.invoice.sector,
            createdAtMillis = now,
            updatedAtMillis = now
        )
        val copyItems = source.sortedItems.map { it.copy(id = 0L, invoiceId = 0L) }
        return db.invoiceDao().saveInvoiceWithItems(copyInvoice, copyItems)
    }

    suspend fun deleteInvoice(invoice: InvoiceEntity) {
        db.invoiceDao().deleteInvoice(invoice)
    }

    fun observeCatalog(): Flow<List<CatalogItemEntity>> = db.catalogDao().observeAll()

    suspend fun saveCatalogItem(item: CatalogItemEntity): Long = db.catalogDao().upsert(item)

    suspend fun deleteCatalogItem(item: CatalogItemEntity) = db.catalogDao().delete(item)

    fun observeCustomers(): Flow<List<CustomerEntity>> = db.customerDao().observeAll()

    suspend fun saveCustomer(customer: CustomerEntity): Long = db.customerDao().upsert(customer)

    suspend fun deleteCustomer(customer: CustomerEntity) = db.customerDao().delete(customer)

    fun observeSettings(): Flow<CompanySettingsEntity> =
        db.settingsDao().observeSettings().map { it ?: CompanySettingsEntity() }

    suspend fun getSettings(): CompanySettingsEntity =
        db.settingsDao().getSettingsOnce() ?: CompanySettingsEntity()

    suspend fun saveSettings(settings: CompanySettingsEntity) {
        db.settingsDao().upsert(settings.copy(id = 1))
    }

    suspend fun exportBackupJson(): String {
        val settings = getSettings()
        val catalog = db.catalogDao().getAllOnce()
        val customers = db.customerDao().getAllOnce()
        val invoices = db.invoiceDao().getAllInvoicesWithItemsOnce().map {
            BackupInvoiceRecord(invoice = it.invoice, items = it.sortedItems)
        }
        val payload = BackupPayload(
            settings = settings,
            catalog = catalog,
            customers = customers,
            invoices = invoices
        )
        return json.encodeToString(payload)
    }

    suspend fun importBackupJson(rawJson: String): Int {
        val payload = json.decodeFromString<BackupPayload>(rawJson)
        db.withTransaction {
            db.settingsDao().upsert(payload.settings.copy(id = 1))
            db.catalogDao().deleteAll()
            db.catalogDao().insertAll(payload.catalog.map { it.copy(id = 0) })
            db.customerDao().deleteAll()
            db.customerDao().insertAll(payload.customers.map { it.copy(id = 0) })
            db.invoiceDao().deleteAllInvoices()
            for (record in payload.invoices) {
                db.invoiceDao().saveInvoiceWithItems(
                    invoice = record.invoice.copy(id = 0),
                    items = record.items.map { it.copy(id = 0, invoiceId = 0) }
                )
            }
        }
        return payload.invoices.size
    }
}
