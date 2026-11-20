package com.goldeneagle.invoice.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.goldeneagle.invoice.data.entity.CatalogItemEntity
import com.goldeneagle.invoice.data.entity.CompanySettingsEntity
import com.goldeneagle.invoice.data.entity.CustomerEntity
import com.goldeneagle.invoice.data.entity.InvoiceEntity
import com.goldeneagle.invoice.data.entity.InvoiceItemEntity
import com.goldeneagle.invoice.data.entity.InvoiceWithItems
import kotlinx.coroutines.flow.Flow

@Dao
interface InvoiceDao {
    @Transaction
    @Query("SELECT * FROM invoices ORDER BY updatedAtMillis DESC, id DESC")
    fun observeAllInvoicesWithItems(): Flow<List<InvoiceWithItems>>

    @Transaction
    @Query("SELECT * FROM invoices ORDER BY updatedAtMillis DESC, id DESC")
    suspend fun getAllInvoicesWithItemsOnce(): List<InvoiceWithItems>

    @Transaction
    @Query("SELECT * FROM invoices WHERE id = :id")
    suspend fun getInvoiceWithItems(id: Long): InvoiceWithItems?

    @Query("SELECT workOrderNumber FROM invoices")
    suspend fun getAllWorkOrderNumbers(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoice(invoice: InvoiceEntity): Long

    @Update
    suspend fun updateInvoice(invoice: InvoiceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<InvoiceItemEntity>)

    @Query("DELETE FROM invoice_items WHERE invoiceId = :invoiceId")
    suspend fun deleteItemsForInvoice(invoiceId: Long)

    @Delete
    suspend fun deleteInvoice(invoice: InvoiceEntity)

    @Query("DELETE FROM invoices")
    suspend fun deleteAllInvoices()

    @Transaction
    suspend fun saveInvoiceWithItems(
        invoice: InvoiceEntity,
        items: List<InvoiceItemEntity>
    ): Long {
        val invoiceId = if (invoice.id == 0L) {
            insertInvoice(invoice)
        } else {
            updateInvoice(invoice)
            deleteItemsForInvoice(invoice.id)
            invoice.id
        }
        val mapped = items.mapIndexed { index, item ->
            item.copy(id = 0, invoiceId = invoiceId, position = index)
        }
        if (mapped.isNotEmpty()) {
            insertItems(mapped)
        }
        return invoiceId
    }
}

@Dao
interface CatalogDao {
    @Query("SELECT * FROM catalog_items ORDER BY category ASC, name ASC")
    fun observeAll(): Flow<List<CatalogItemEntity>>

    @Query("SELECT * FROM catalog_items ORDER BY category ASC, name ASC")
    suspend fun getAllOnce(): List<CatalogItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: CatalogItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<CatalogItemEntity>)

    @Delete
    suspend fun delete(item: CatalogItemEntity)

    @Query("DELETE FROM catalog_items")
    suspend fun deleteAll()
}

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers ORDER BY name ASC")
    fun observeAll(): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers ORDER BY name ASC")
    suspend fun getAllOnce(): List<CustomerEntity>

    @Query("SELECT * FROM customers WHERE LOWER(name) = LOWER(:name) LIMIT 1")
    suspend fun findByName(name: String): CustomerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(customer: CustomerEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(customers: List<CustomerEntity>)

    @Delete
    suspend fun delete(customer: CustomerEntity)

    @Query("DELETE FROM customers")
    suspend fun deleteAll()
}

@Dao
interface SettingsDao {
    @Query("SELECT * FROM company_settings WHERE id = 1")
    fun observeSettings(): Flow<CompanySettingsEntity?>

    @Query("SELECT * FROM company_settings WHERE id = 1")
    suspend fun getSettingsOnce(): CompanySettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(settings: CompanySettingsEntity)
}
