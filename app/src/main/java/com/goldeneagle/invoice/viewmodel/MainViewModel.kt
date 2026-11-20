package com.goldeneagle.invoice.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.goldeneagle.invoice.GoldenEagleApp
import com.goldeneagle.invoice.data.entity.CatalogItemEntity
import com.goldeneagle.invoice.data.entity.CompanySettingsEntity
import com.goldeneagle.invoice.data.entity.CustomerEntity
import com.goldeneagle.invoice.data.entity.InvoiceEntity
import com.goldeneagle.invoice.data.entity.InvoiceItemEntity
import com.goldeneagle.invoice.data.entity.InvoiceWithItems
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as GoldenEagleApp
    private val repository = app.repository
    private val pdfGenerator = app.pdfGenerator

    val invoices: StateFlow<List<InvoiceWithItems>> = repository.observeInvoices()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val catalog: StateFlow<List<CatalogItemEntity>> = repository.observeCatalog()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val customers: StateFlow<List<CustomerEntity>> = repository.observeCustomers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val settings: StateFlow<CompanySettingsEntity> = repository.observeSettings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CompanySettingsEntity())

    suspend fun getInvoiceWithItems(id: Long): InvoiceWithItems? = withContext(Dispatchers.IO) {
        repository.getInvoiceWithItems(id)
    }

    suspend fun nextWorkOrderNumber(): String = withContext(Dispatchers.IO) {
        repository.generateNextWorkOrderNumber()
    }

    suspend fun getCurrentSettings(): CompanySettingsEntity = withContext(Dispatchers.IO) {
        repository.getSettings()
    }

    suspend fun saveInvoice(
        invoice: InvoiceEntity,
        items: List<InvoiceItemEntity>,
        autoSaveCustomer: Boolean = true
    ): Long = withContext(Dispatchers.IO) {
        repository.saveInvoice(invoice, items, autoSaveCustomer)
    }

    fun duplicateInvoice(sourceId: Long, newSector: String? = null, onCreated: (Long) -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            val newId = repository.duplicateInvoice(sourceId, newSector)
            if (newId != null) {
                withContext(Dispatchers.Main) { onCreated(newId) }
            }
        }
    }

    fun deleteInvoice(invoice: InvoiceEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteInvoice(invoice)
        }
    }

    fun saveCatalogItem(item: CatalogItemEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveCatalogItem(item)
        }
    }

    fun deleteCatalogItem(item: CatalogItemEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteCatalogItem(item)
        }
    }

    fun saveCustomer(customer: CustomerEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveCustomer(customer)
        }
    }

    fun deleteCustomer(customer: CustomerEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteCustomer(customer)
        }
    }

    fun saveSettings(updated: CompanySettingsEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveSettings(updated)
        }
    }

    suspend fun generatePdfForInvoice(invoiceId: Long): Result<Pair<InvoiceWithItems, File>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val data = repository.getInvoiceWithItems(invoiceId)
                    ?: error("لم يتم العثور على الفاتورة")
                val currentSettings = repository.getSettings()
                val file = pdfGenerator.generatePdfFile(
                    invoice = data.invoice,
                    items = data.sortedItems,
                    settings = currentSettings
                )
                data to file
            }
        }

    suspend fun exportBackupJson(): Result<String> = withContext(Dispatchers.IO) {
        runCatching { repository.exportBackupJson() }
    }

    suspend fun importBackupJson(rawJson: String): Result<Int> = withContext(Dispatchers.IO) {
        runCatching { repository.importBackupJson(rawJson) }
    }
}
