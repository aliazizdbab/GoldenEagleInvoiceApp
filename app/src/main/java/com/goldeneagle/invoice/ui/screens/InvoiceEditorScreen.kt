package com.goldeneagle.invoice.ui.screens

import android.graphics.BitmapFactory
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.goldeneagle.invoice.R
import com.goldeneagle.invoice.data.entity.CatalogItemEntity
import com.goldeneagle.invoice.data.entity.CustomerEntity
import com.goldeneagle.invoice.data.entity.InvoiceEntity
import com.goldeneagle.invoice.data.entity.InvoiceItemEntity
import com.goldeneagle.invoice.data.model.CurrencyCode
import com.goldeneagle.invoice.data.model.DecimalMode
import com.goldeneagle.invoice.data.model.DiscountType
import com.goldeneagle.invoice.data.model.SectorPresets
import com.goldeneagle.invoice.ui.components.SectionCard
import com.goldeneagle.invoice.ui.components.SignaturePadDialog
import com.goldeneagle.invoice.ui.theme.AccentDanger
import com.goldeneagle.invoice.ui.theme.BorderSoft
import com.goldeneagle.invoice.ui.theme.CardWhite
import com.goldeneagle.invoice.ui.theme.InkMuted
import com.goldeneagle.invoice.ui.theme.OliveGold
import com.goldeneagle.invoice.ui.theme.OliveGoldDark
import com.goldeneagle.invoice.ui.theme.OliveGoldLight
import com.goldeneagle.invoice.util.ArabicTafqeet
import com.goldeneagle.invoice.util.CurrencyFormatter
import com.goldeneagle.invoice.util.DateHelper
import com.goldeneagle.invoice.util.InvoiceCalculator
import com.goldeneagle.invoice.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import java.io.File

private data class EditableItem(
    val uid: Long = System.nanoTime(),
    val description: String,
    val quantity: Double,
    val unitPrice: Double
) {
    val total: Double get() = quantity * unitPrice
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceEditorScreen(
    invoiceId: Long?,
    prefillCustomerName: String? = null,
    prefillCompanyName: String? = null,
    prefillPhone: String? = null,
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    onSavedAndPreview: (Long) -> Unit
) {
    val catalog by viewModel.catalog.collectAsState()
    val customers by viewModel.customers.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val decimalMode = DecimalMode.fromString(settings.decimalMode)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }

    var existingCreatedAt by remember { mutableStateOf(System.currentTimeMillis()) }
    var workOrderNumber by remember { mutableStateOf("") }
    var dateText by remember { mutableStateOf(DateHelper.todayFormatted()) }
    var customerName by remember { mutableStateOf(prefillCustomerName.orEmpty()) }
    var sector by remember { mutableStateOf("عرض") }
    var companyName by remember { mutableStateOf(prefillCompanyName.orEmpty()) }
    var customerPhone by remember { mutableStateOf(prefillPhone.orEmpty()) }
    var currency by remember { mutableStateOf(CurrencyCode.LYD) }
    var taxRateText by remember { mutableStateOf("0") }
    var discountType by remember { mutableStateOf(DiscountType.FIXED) }
    var discountText by remember { mutableStateOf("0") }
    var notes by remember { mutableStateOf("") }
    var showStamp by remember { mutableStateOf(true) }
    var showSignature by remember { mutableStateOf(false) }
    var signaturePath by remember { mutableStateOf<String?>(null) }

    val itemsList = remember { mutableStateListOf<EditableItem>() }

    // Dialog states
    var showDatePicker by remember { mutableStateOf(false) }
    var showCustomerPicker by remember { mutableStateOf(false) }
    var showCatalogPicker by remember { mutableStateOf(false) }
    var editingItemIndex by remember { mutableStateOf<Int?>(null) }
    var showManualItemDialog by remember { mutableStateOf(false) }
    var showSignatureDialog by remember { mutableStateOf(false) }

    LaunchedEffect(invoiceId) {
        isLoading = true
        val currentSettings = viewModel.getCurrentSettings()
        if (invoiceId != null && invoiceId > 0L) {
            val loaded = viewModel.getInvoiceWithItems(invoiceId)
            if (loaded != null) {
                val inv = loaded.invoice
                existingCreatedAt = inv.createdAtMillis
                workOrderNumber = inv.workOrderNumber
                dateText = inv.dateText
                customerName = inv.customerName
                sector = inv.sector
                companyName = inv.companyName
                customerPhone = inv.customerPhone
                currency = CurrencyCode.fromString(inv.currencyCode)
                taxRateText = if (inv.taxRatePercent == 0.0) "0" else inv.taxRatePercent.toString()
                discountType = DiscountType.fromString(inv.discountType)
                discountText = if (inv.discountValue == 0.0) "0" else inv.discountValue.toString()
                notes = inv.notes
                showStamp = inv.showStamp
                showSignature = inv.showSignature
                signaturePath = inv.signaturePath
                itemsList.clear()
                loaded.sortedItems.forEach {
                    itemsList.add(
                        EditableItem(
                            description = it.description,
                            quantity = it.quantity,
                            unitPrice = it.unitPrice
                        )
                    )
                }
            }
        } else {
            workOrderNumber = viewModel.nextWorkOrderNumber()
            dateText = DateHelper.todayFormatted()
            currency = CurrencyCode.fromString(currentSettings.defaultCurrency)
            taxRateText = if (currentSettings.defaultTaxPercent == 0.0) "0" else currentSettings.defaultTaxPercent.toString()
            notes = currentSettings.defaultInvoiceNotes
            signaturePath = currentSettings.defaultSignaturePath
        }
        isLoading = false
    }

    val taxRate = taxRateText.replace(",", ".").toDoubleOrNull() ?: 0.0
    val discountVal = discountText.replace(",", ".").toDoubleOrNull() ?: 0.0
    val calc = InvoiceCalculator.calculate(
        lineTotals = itemsList.map { it.total },
        taxRatePercent = taxRate,
        discountType = discountType,
        discountValue = discountVal
    )
    val liveTafqeet = ArabicTafqeet.toArabicWords(calc.grandTotal, currency)

    fun persistInvoice(openPreviewAfter: Boolean) {
        if (workOrderNumber.isBlank()) {
            scope.launch { snackbarHostState.showSnackbar("يرجى إدخال رقم الفاتورة / أمر العمل") }
            return
        }
        if (itemsList.isEmpty()) {
            scope.launch { snackbarHostState.showSnackbar("يرجى إضافة صنف واحد على الأقل") }
            return
        }
        isSaving = true
        scope.launch {
            val entity = InvoiceEntity(
                id = invoiceId ?: 0L,
                workOrderNumber = workOrderNumber.trim(),
                dateText = dateText.trim(),
                createdAtMillis = existingCreatedAt,
                customerName = customerName.trim(),
                sector = sector.trim(),
                companyName = companyName.trim(),
                customerPhone = customerPhone.trim(),
                currencyCode = currency.name,
                taxRatePercent = taxRate,
                discountType = discountType.name,
                discountValue = discountVal,
                notes = notes,
                showStamp = showStamp,
                showSignature = showSignature,
                signaturePath = signaturePath,
                subtotal = calc.subtotal,
                taxAmount = calc.taxAmount,
                discountAmount = calc.discountAmount,
                grandTotal = calc.grandTotal
            )
            val entityItems = itemsList.mapIndexed { idx, item ->
                InvoiceItemEntity(
                    position = idx,
                    description = item.description.trim(),
                    quantity = item.quantity,
                    unitPrice = item.unitPrice
                )
            }
            val savedId = viewModel.saveInvoice(entity, entityItems)
            isSaving = false
            if (openPreviewAfter) {
                onSavedAndPreview(savedId)
            } else {
                onNavigateBack()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (invoiceId != null && invoiceId > 0L) "تعديل الفاتورة" else "فاتورة جديدة",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "رجوع",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = OliveGold)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            Surface(
                shadowElevation = 8.dp,
                color = CardWhite,
                border = BorderStroke(1.dp, BorderSoft)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { persistInvoice(openPreviewAfter = false) },
                        enabled = !isSaving && !isLoading,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("حفظ فقط")
                    }
                    Button(
                        onClick = { persistInvoice(openPreviewAfter = true) },
                        enabled = !isSaving && !isLoading,
                        modifier = Modifier.weight(1.4f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = OliveGold)
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("حفظ ومعاينة PDF")
                    }
                }
            }
        }
    ) { innerPadding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = OliveGold)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 1. Metadata & Currency Card
                SectionCard(title = "بيانات المستند والعملة", icon = Icons.Default.Receipt) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = workOrderNumber,
                            onValueChange = { workOrderNumber = it },
                            label = { Text("WORK ORDER #") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = dateText,
                            onValueChange = { dateText = it },
                            label = { Text("التاريخ (DATE)") },
                            singleLine = true,
                            trailingIcon = {
                                IconButton(onClick = { showDatePicker = true }) {
                                    Icon(Icons.Default.CalendarMonth, contentDescription = "اختر التاريخ", tint = OliveGold)
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text("العملة:", style = MaterialTheme.typography.bodySmall, color = InkMuted)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CurrencyCode.entries.forEach { code ->
                            FilterChip(
                                selected = currency == code,
                                onClick = { currency = code },
                                label = { Text("${code.arabicName} (${code.symbol})") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = sector,
                        onValueChange = { sector = it },
                        label = { Text("قطاع / نوع المستند") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SectorPresets.list.forEach { preset ->
                            FilterChip(
                                selected = sector == preset,
                                onClick = { sector = preset },
                                label = { Text(preset) }
                            )
                        }
                    }
                }

                // 2. Customer Info Card
                SectionCard(
                    title = "بيانات العميل",
                    icon = Icons.Default.Person,
                    trailing = {
                        if (customers.isNotEmpty()) {
                            TextButton(onClick = { showCustomerPicker = true }) {
                                Icon(Icons.Default.PersonSearch, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("اختيار محفوظ")
                            }
                        }
                    }
                ) {
                    OutlinedTextField(
                        value = customerName,
                        onValueChange = { customerName = it },
                        label = { Text("إلى السيد") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = companyName,
                            onValueChange = { companyName = it },
                            label = { Text("اسم الشركة (اختياري)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = customerPhone,
                            onValueChange = { customerPhone = it },
                            label = { Text("رقم هاتف الزبون") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // 3. Items List Card
                SectionCard(
                    title = "الأصناف (${itemsList.size})",
                    icon = Icons.Default.ShoppingCart
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { showCatalogPicker = true },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = OliveGold),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Inventory2, contentDescription = null, modifier = Modifier.size(17.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("من الكتالوج")
                        }
                        OutlinedButton(
                            onClick = {
                                editingItemIndex = null
                                showManualItemDialog = true
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(17.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("صنف يدوي")
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (itemsList.isEmpty()) {
                        Surface(
                            color = OliveGoldLight,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "لم تتم إضافة أصناف بعد — اختر من الكتالوج أو أضف صنفاً يدوياً",
                                style = MaterialTheme.typography.bodySmall,
                                color = OliveGoldDark,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    } else {
                        itemsList.forEachIndexed { index, item ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFAF7F0)),
                                border = BorderStroke(1.dp, BorderSoft)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = OliveGold
                                        ) {
                                            Text(
                                                text = "${index + 1}",
                                                color = Color.White,
                                                style = MaterialTheme.typography.labelLarge,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = item.description,
                                            style = MaterialTheme.typography.titleSmall,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${CurrencyFormatter.formatQuantity(item.quantity)} × ${CurrencyFormatter.formatMoney(item.unitPrice, currency, decimalMode)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = InkMuted
                                        )
                                        Spacer(modifier = Modifier.weight(1f))
                                        Text(
                                            text = CurrencyFormatter.formatMoney(item.total, currency, decimalMode),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = OliveGoldDark
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        if (index > 0) {
                                            IconButton(
                                                onClick = {
                                                    val moved = itemsList.removeAt(index)
                                                    itemsList.add(index - 1, moved)
                                                },
                                                modifier = Modifier.size(34.dp)
                                            ) {
                                                Icon(Icons.Default.ArrowUpward, contentDescription = "أعلى", modifier = Modifier.size(18.dp))
                                            }
                                        }
                                        if (index < itemsList.lastIndex) {
                                            IconButton(
                                                onClick = {
                                                    val moved = itemsList.removeAt(index)
                                                    itemsList.add(index + 1, moved)
                                                },
                                                modifier = Modifier.size(34.dp)
                                            ) {
                                                Icon(Icons.Default.ArrowDownward, contentDescription = "أسفل", modifier = Modifier.size(18.dp))
                                            }
                                        }
                                        IconButton(
                                            onClick = {
                                                editingItemIndex = index
                                                showManualItemDialog = true
                                            },
                                            modifier = Modifier.size(34.dp)
                                        ) {
                                            Icon(Icons.Default.Edit, contentDescription = "تعديل", tint = OliveGoldDark, modifier = Modifier.size(18.dp))
                                        }
                                        IconButton(
                                            onClick = { itemsList.removeAt(index) },
                                            modifier = Modifier.size(34.dp)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "حذف", tint = AccentDanger, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 4. Tax, Discount & Live Totals Card
                SectionCard(title = "الضرائب، الخصم، والإجمالي النهائي") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = taxRateText,
                            onValueChange = { taxRateText = it },
                            label = { Text("نسبة الضريبة (%)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = discountText,
                            onValueChange = { discountText = it },
                            label = {
                                Text(
                                    if (discountType == DiscountType.PERCENT) "الخصم (%)"
                                    else "الخصم (${currency.symbol})"
                                )
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DiscountType.entries.forEach { dt ->
                            FilterChip(
                                selected = discountType == dt,
                                onClick = { discountType = dt },
                                label = { Text(dt.arabicLabel) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = BorderSoft)
                    Spacer(modifier = Modifier.height(10.dp))

                    SummaryLine("الإجمالي الفرعي", CurrencyFormatter.formatMoney(calc.subtotal, currency, decimalMode))
                    SummaryLine("مجموع الضرائب (${CurrencyFormatter.formatPercent(taxRate)})", CurrencyFormatter.formatMoney(calc.taxAmount, currency, decimalMode))
                    SummaryLine("الخصومات", CurrencyFormatter.formatMoney(calc.discountAmount, currency, decimalMode))

                    Spacer(modifier = Modifier.height(8.dp))

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = OliveGold,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "الإجمالي النهائي (TOTAL)",
                                    color = Color.White,
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = CurrencyFormatter.formatMoney(calc.grandTotal, currency, decimalMode),
                                    color = Color.White,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            HorizontalDivider(color = Color.White.copy(alpha = 0.35f))
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = liveTafqeet,
                                color = Color.White,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // 5. Notes Card
                SectionCard(
                    title = "الملاحظات والشروط",
                    icon = Icons.AutoMirrored.Filled.Notes,
                    trailing = {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            TextButton(onClick = { notes = settings.defaultInvoiceNotes }) {
                                Text("قالب فاتورة", style = MaterialTheme.typography.bodySmall)
                            }
                            TextButton(onClick = { notes = settings.defaultQuoteNotes }) {
                                Text("قالب عرض", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                ) {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("كل سطر سيظهر كـ بند مرقّم تلقائياً") },
                        minLines = 3,
                        maxLines = 6,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // 6. Stamp & Hand-Drawn Signature Card
                SectionCard(title = "الختم الرسمي والتوقيع اليدوي", icon = Icons.Default.Verified) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(R.drawable.company_stamp),
                            contentDescription = "ختم الشركة",
                            modifier = Modifier
                                .width(78.dp)
                                .height(42.dp),
                            contentScale = ContentScale.Fit
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("إدراج ختم الشركة", style = MaterialTheme.typography.titleSmall)
                            Text(
                                "يطبع الختم الأزرق بزاوية طبيعية فوق منطقة التوقيع",
                                style = MaterialTheme.typography.bodySmall,
                                color = InkMuted
                            )
                        }
                        Switch(checked = showStamp, onCheckedChange = { showStamp = it })
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = BorderSoft)
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("إدراج توقيع يدوي", style = MaterialTheme.typography.titleSmall)
                            Text(
                                "رسم التوقيع بإصبعك ليظهر فوق خانة Signature",
                                style = MaterialTheme.typography.bodySmall,
                                color = InkMuted
                            )
                        }
                        Switch(checked = showSignature, onCheckedChange = { showSignature = it })
                    }

                    if (showSignature) {
                        Spacer(modifier = Modifier.height(8.dp))
                        val effectiveSig = signaturePath?.takeIf { it.isNotBlank() } ?: settings.defaultSignaturePath
                        val sigBmp = remember(effectiveSig) {
                            effectiveSig?.let { File(it).takeIf { f -> f.exists() }?.let { f -> BitmapFactory.decodeFile(f.absolutePath) } }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (sigBmp != null) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color.White,
                                    border = BorderStroke(1.dp, BorderSoft)
                                ) {
                                    Image(
                                        bitmap = sigBmp.asImageBitmap(),
                                        contentDescription = "معاينة التوقيع",
                                        modifier = Modifier
                                            .width(120.dp)
                                            .height(50.dp)
                                            .padding(4.dp),
                                        contentScale = ContentScale.Fit
                                    )
                                }
                            }
                            OutlinedButton(
                                onClick = { showSignatureDialog = true },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Draw, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (sigBmp == null) "ارسم التوقيع" else "تغيير التوقيع")
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Date Picker Dialog
    if (showDatePicker) {
        val pickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        dateText = DateHelper.formatMillis(millis)
                    }
                    showDatePicker = false
                }) { Text("تأكيد") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("إلغاء") }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }

    // Customer Picker Dialog
    if (showCustomerPicker) {
        CustomerPickerDialog(
            customers = customers,
            onDismiss = { showCustomerPicker = false },
            onSelect = { c ->
                customerName = c.name
                companyName = c.companyName
                customerPhone = c.phone
                showCustomerPicker = false
            }
        )
    }

    // Catalog Picker Dialog
    if (showCatalogPicker) {
        CatalogPickerDialog(
            catalog = catalog,
            decimalMode = decimalMode,
            onDismiss = { showCatalogPicker = false },
            onSelect = { item ->
                itemsList.add(
                    EditableItem(
                        description = item.name,
                        quantity = 1.0,
                        unitPrice = item.defaultPrice
                    )
                )
                showCatalogPicker = false
            }
        )
    }

    // Manual Item Add / Edit Dialog
    if (showManualItemDialog) {
        val currentEditing = editingItemIndex?.let { itemsList.getOrNull(it) }
        ManualItemDialog(
            initial = currentEditing,
            currency = currency,
            onDismiss = {
                showManualItemDialog = false
                editingItemIndex = null
            },
            onSave = { desc, qty, price, saveToCatalog ->
                val idx = editingItemIndex
                if (idx != null && idx in itemsList.indices) {
                    itemsList[idx] = itemsList[idx].copy(
                        description = desc,
                        quantity = qty,
                        unitPrice = price
                    )
                } else {
                    itemsList.add(EditableItem(description = desc, quantity = qty, unitPrice = price))
                }
                if (saveToCatalog) {
                    viewModel.saveCatalogItem(
                        CatalogItemEntity(
                            name = desc,
                            defaultPrice = price,
                            category = "عام",
                            currencyCode = currency.name
                        )
                    )
                }
                showManualItemDialog = false
                editingItemIndex = null
            }
        )
    }

    // Finger Signature Pad Dialog
    if (showSignatureDialog) {
        SignaturePadDialog(
            onDismiss = { showSignatureDialog = false },
            onSaved = { path ->
                signaturePath = path
                showSignature = true
                showSignatureDialog = false
            }
        )
    }
}

@Composable
private fun SummaryLine(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = InkMuted)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ManualItemDialog(
    initial: EditableItem?,
    currency: CurrencyCode,
    onDismiss: () -> Unit,
    onSave: (String, Double, Double, Boolean) -> Unit
) {
    var desc by remember { mutableStateOf(initial?.description ?: "") }
    var qtyText by remember {
        mutableStateOf(
            if (initial != null) CurrencyFormatter.formatQuantity(initial.quantity) else "1"
        )
    }
    var priceText by remember { mutableStateOf(if (initial != null) initial.unitPrice.toString() else "") }
    var saveToCatalog by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "إضافة صنف" else "تعديل الصنف") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("الوصف (اسم الصنف أو الخدمة)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = qtyText,
                        onValueChange = { qtyText = it },
                        label = { Text("العدد") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = priceText,
                        onValueChange = { priceText = it },
                        label = { Text("السعر (${currency.symbol})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1.4f)
                    )
                }
                if (initial == null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { saveToCatalog = !saveToCatalog }
                    ) {
                        Checkbox(checked = saveToCatalog, onCheckedChange = { saveToCatalog = it })
                        Text("حفظ هذا الصنف في الكتالوج للاستخدام لاحقاً", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = desc.isNotBlank(),
                onClick = {
                    val qty = qtyText.replace(",", ".").toDoubleOrNull()?.takeIf { it > 0.0 } ?: 1.0
                    val price = priceText.replace(",", ".").toDoubleOrNull()?.takeIf { it >= 0.0 } ?: 0.0
                    onSave(desc.trim(), qty, price, saveToCatalog)
                }
            ) { Text("حفظ") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}

@Composable
private fun CatalogPickerDialog(
    catalog: List<CatalogItemEntity>,
    decimalMode: DecimalMode,
    onDismiss: () -> Unit,
    onSelect: (CatalogItemEntity) -> Unit
) {
    var q by remember { mutableStateOf("") }
    val filtered = remember(catalog, q) {
        val term = q.trim()
        if (term.isEmpty()) catalog
        else catalog.filter { it.name.contains(term, ignoreCase = true) || it.category.contains(term, ignoreCase = true) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("اختر صنفاً من الكتالوج") },
        text = {
            Column {
                OutlinedTextField(
                    value = q,
                    onValueChange = { q = it },
                    placeholder = { Text("بحث سريع...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                if (filtered.isEmpty()) {
                    Text("لا توجد أصناف مطابقة", color = InkMuted, modifier = Modifier.padding(12.dp))
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 320.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(filtered, key = { it.id }) { item ->
                            val itemCurr = CurrencyCode.fromString(item.currencyCode)
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = OliveGoldLight,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelect(item) }
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(item.name, style = MaterialTheme.typography.titleSmall)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(item.category, style = MaterialTheme.typography.bodySmall, color = InkMuted)
                                        Text(
                                            CurrencyFormatter.formatMoney(item.defaultPrice, itemCurr, decimalMode),
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = OliveGoldDark
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("إغلاق") }
        }
    )
}

@Composable
private fun CustomerPickerDialog(
    customers: List<CustomerEntity>,
    onDismiss: () -> Unit,
    onSelect: (CustomerEntity) -> Unit
) {
    var q by remember { mutableStateOf("") }
    val filtered = remember(customers, q) {
        val term = q.trim()
        if (term.isEmpty()) customers
        else customers.filter {
            it.name.contains(term, ignoreCase = true) ||
                it.companyName.contains(term, ignoreCase = true) ||
                it.phone.contains(term, ignoreCase = true)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("اختر زبوناً محفوظاً") },
        text = {
            Column {
                OutlinedTextField(
                    value = q,
                    onValueChange = { q = it },
                    placeholder = { Text("بحث بالاسم أو الشركة أو الهاتف...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                LazyColumn(
                    modifier = Modifier.heightIn(max = 300.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(filtered, key = { it.id }) { customer ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = OliveGoldLight,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(customer) }
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(customer.name, style = MaterialTheme.typography.titleSmall)
                                if (customer.companyName.isNotBlank()) {
                                    Text(customer.companyName, style = MaterialTheme.typography.bodySmall, color = InkMuted)
                                }
                                if (customer.phone.isNotBlank()) {
                                    Text(customer.phone, style = MaterialTheme.typography.bodySmall, color = OliveGoldDark)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("إغلاق") }
        }
    )
}
