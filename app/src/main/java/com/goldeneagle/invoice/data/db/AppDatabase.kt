package com.goldeneagle.invoice.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.goldeneagle.invoice.data.dao.CatalogDao
import com.goldeneagle.invoice.data.dao.CustomerDao
import com.goldeneagle.invoice.data.dao.InvoiceDao
import com.goldeneagle.invoice.data.dao.SettingsDao
import com.goldeneagle.invoice.data.entity.CatalogItemEntity
import com.goldeneagle.invoice.data.entity.CompanySettingsEntity
import com.goldeneagle.invoice.data.entity.CustomerEntity
import com.goldeneagle.invoice.data.entity.InvoiceEntity
import com.goldeneagle.invoice.data.entity.InvoiceItemEntity
import com.goldeneagle.invoice.data.model.CurrencyCode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        InvoiceEntity::class,
        InvoiceItemEntity::class,
        CatalogItemEntity::class,
        CustomerEntity::class,
        CompanySettingsEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun invoiceDao(): InvoiceDao
    abstract fun catalogDao(): CatalogDao
    abstract fun customerDao(): CustomerDao
    abstract fun settingsDao(): SettingsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "golden_eagle_invoices.db"
                )
                    .addCallback(SeedCallback(context.applicationContext))
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class SeedCallback(private val appContext: Context) : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            CoroutineScope(Dispatchers.IO).launch {
                val database = getInstance(appContext)
                database.settingsDao().upsert(CompanySettingsEntity())
                database.catalogDao().insertAll(
                    listOf(
                        CatalogItemEntity(
                            name = "كاميرا ملونة بصوت Craft GA-YUFW-M5S IP 5MP",
                            defaultPrice = 400.0,
                            category = "كاميرات مراقبة",
                            currencyCode = CurrencyCode.LYD.name
                        ),
                        CatalogItemEntity(
                            name = "GU-N8016H1S-P8-NVR 16 CH 8 POE 4K CRAFT",
                            defaultPrice = 720.0,
                            category = "كاميرات مراقبة",
                            currencyCode = CurrencyCode.LYD.name
                        ),
                        CatalogItemEntity(
                            name = "هارد ديسك HDD 1T",
                            defaultPrice = 300.0,
                            category = "تخزين وشبكات",
                            currencyCode = CurrencyCode.LYD.name
                        ),
                        CatalogItemEntity(
                            name = "مستلزمات تركيب",
                            defaultPrice = 100.0,
                            category = "مستلزمات",
                            currencyCode = CurrencyCode.LYD.name
                        ),
                        CatalogItemEntity(
                            name = "عمل يد وبرمجة وتشغيل",
                            defaultPrice = 900.0,
                            category = "خدمات وتركيب",
                            currencyCode = CurrencyCode.LYD.name
                        )
                    )
                )
            }
        }
    }
}
