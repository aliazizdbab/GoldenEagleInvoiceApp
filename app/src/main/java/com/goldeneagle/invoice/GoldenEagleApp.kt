package com.goldeneagle.invoice

import android.app.Application
import com.goldeneagle.invoice.data.db.AppDatabase
import com.goldeneagle.invoice.data.repository.AppRepository
import com.goldeneagle.invoice.pdf.InvoicePdfGenerator

class GoldenEagleApp : Application() {
    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }
    val repository: AppRepository by lazy { AppRepository(database) }
    val pdfGenerator: InvoicePdfGenerator by lazy { InvoicePdfGenerator(this) }
}
