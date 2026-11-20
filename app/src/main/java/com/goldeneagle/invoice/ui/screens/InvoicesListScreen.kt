package com.goldeneagle.invoice.ui.screens

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.goldeneagle.invoice.R
import com.goldeneagle.invoice.data.entity.InvoiceEntity
import com.goldeneagle.invoice.data.entity.InvoiceWithItems
import com.goldeneagle.invoice.data.model.CurrencyCode
import com.goldeneagle.invoice.data.model.DecimalMode
import com.goldeneagle.invoice.data.model.LogoStyle
import com.goldeneagle.invoice.ui.components.ConfirmDeleteDialog
import com.goldeneagle.invoice.ui.components.EmptyStateView
import com.goldeneagle.invoice.ui.theme.AccentDanger
import com.goldeneagle.invoice.ui.theme.BorderSoft
import com.goldeneagle.invoice.ui.theme.CardWhite
import com.goldeneagle.invoice.ui.theme.InkMuted
import com.goldeneagle.invoice.ui.theme.OliveGold
import com.goldeneagle.invoice.ui.theme.OliveGoldDark
import com.goldeneagle.invoice.ui.theme.OliveGoldLight
import com.goldeneagle.invoice.util.CurrencyFormatter
import com.goldeneagle.invoice.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoicesListScreen(
    viewModel: MainViewModel,
    onCreateNew: () -> Unit,
    onEditInvoice: (Long) -> Unit,
    onPreviewPdf: (Long) -> Unit
) {
    val invoices by viewModel.invoices.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val decimalMode = DecimalMode.fromString(settings.decimalMode)
    val logoRes = if (LogoStyle.fromString(settings.logoStyle) == LogoStyle.RED_GOLD) {
        R.drawable.logo_red_gold
    } else {
        R.drawable.logo_full_gold
    }

    var query by remember { mutableStateOf("") }
    var selectedSector by remember { mutableStateOf("الكل") }
    var invoiceToDelete by remember { mutableStateOf<InvoiceEntity?>(null) }

    val filterOptions = listOf("الكل", "عرض", "فاتورة", "أمر عمل", "صيانة")

    val filtered = remember(invoices, query, selectedSector) {
        invoices.filter { item ->
            val inv = item.invoice
            val matchesSector = selectedSector == "الكل" || inv.sector.contains(selectedSector, ignoreCase = true)
            val q = query.trim()
            val matchesQuery = q.isEmpty() ||
                inv.workOrderNumber.contains(q, ignoreCase = true) ||
                inv.customerName.contains(q, ignoreCase = true) ||
                inv.companyName.contains(q, ignoreCase = true) ||
                inv.customerPhone.contains(q, ignoreCase = true) ||
                inv.dateText.contains(q, ignoreCase = true)
            matchesSector && matchesQuery
        }
    }

    // Group totals per currency for quick dashboard overview
    val totalsByCurrency = remember(filtered) {
        filtered.groupBy { CurrencyCode.fromString(it.invoice.currencyCode) }
            .mapValues { entry -> entry.value.sumOf { it.invoice.grandTotal } }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(logoRes),
                            contentDescription = "شعار النسر الذهبي",
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.92f))
                                .padding(3.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "شركة النسر الذهبي",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White
                            )
                            Text(
                                text = "سجل الفواتير وعروض الأسعار",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = OliveGold)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCreateNew,
                containerColor = OliveGold,
                contentColor = Color.White,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("فاتورة جديدة") }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Search & Quick Filter Bar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CardWhite)
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("بحث برقم الفاتورة، اسم الزبون، الشركة، أو التاريخ...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = OliveGold) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    filterOptions.forEach { option ->
                        FilterChip(
                            selected = selectedSector == option,
                            onClick = { selectedSector = option },
                            label = { Text(option) }
                        )
                    }
                }
            }

            // Summary strip
            if (filtered.isNotEmpty()) {
                Surface(
                    color = OliveGoldLight,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "العدد: ${filtered.size}",
                            style = MaterialTheme.typography.labelLarge,
                            color = OliveGoldDark
                        )
                        totalsByCurrency.forEach { (currency, sum) ->
                            Text(
                                text = "الإجمالي (${currency.symbol}): ${CurrencyFormatter.formatMoney(sum, currency, decimalMode)}",
                                style = MaterialTheme.typography.labelLarge,
                                color = OliveGoldDark
                            )
                        }
                    }
                }
            }

            if (filtered.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyStateView(
                        icon = Icons.Default.Description,
                        title = if (invoices.isEmpty()) "لا توجد فواتير بعد" else "لا توجد نتائج مطابقة",
                        subtitle = if (invoices.isEmpty()) {
                            "اضغط على زر «فاتورة جديدة» لإنشاء أول فاتورة PDF جاهزة"
                        } else {
                            "جرّب تغيير كلمة البحث أو التصفية"
                        }
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 92.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filtered, key = { it.invoice.id }) { record ->
                        InvoiceListCard(
                            record = record,
                            decimalMode = decimalMode,
                            onEdit = { onEditInvoice(record.invoice.id) },
                            onPreviewPdf = { onPreviewPdf(record.invoice.id) },
                            onDuplicate = { newSector ->
                                viewModel.duplicateInvoice(record.invoice.id, newSector) { newId ->
                                    onEditInvoice(newId)
                                }
                            },
                            onDelete = { invoiceToDelete = record.invoice }
                        )
                    }
                }
            }
        }
    }

    invoiceToDelete?.let { inv ->
        ConfirmDeleteDialog(
            title = "حذف الفاتورة",
            message = "هل تريد حذف الفاتورة رقم ${inv.workOrderNumber} الخاصة بـ «${inv.customerName.ifBlank { "بدون اسم" }}» نهائياً؟",
            onConfirm = {
                viewModel.deleteInvoice(inv)
                invoiceToDelete = null
            },
            onDismiss = { invoiceToDelete = null }
        )
    }
}

@Composable
private fun InvoiceListCard(
    record: InvoiceWithItems,
    decimalMode: DecimalMode,
    onEdit: () -> Unit,
    onPreviewPdf: () -> Unit,
    onDuplicate: (String?) -> Unit,
    onDelete: () -> Unit
) {
    val inv = record.invoice
    val currency = CurrencyCode.fromString(inv.currencyCode)
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        border = BorderStroke(1.dp, BorderSoft),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = OliveGoldLight
                ) {
                    Text(
                        text = inv.workOrderNumber.ifBlank { "#${inv.id}" },
                        style = MaterialTheme.typography.labelLarge,
                        color = OliveGoldDark,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                if (inv.sector.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFEEECE6)
                    ) {
                        Text(
                            text = inv.sector,
                            style = MaterialTheme.typography.bodySmall,
                            color = InkMuted,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = inv.dateText,
                    style = MaterialTheme.typography.bodySmall,
                    color = InkMuted
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = inv.customerName.ifBlank { "عميل غير محدد" },
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (inv.companyName.isNotBlank()) {
                        Text(
                            text = inv.companyName,
                            style = MaterialTheme.typography.bodySmall,
                            color = InkMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = "${record.items.size} أصناف${if (inv.showStamp) " • مختومة" else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = InkMuted
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = CurrencyFormatter.formatMoney(inv.grandTotal, currency, decimalMode),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = OliveGoldDark
                    )
                    Text(
                        text = currency.arabicName,
                        style = MaterialTheme.typography.bodySmall,
                        color = InkMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = OliveGold,
                    modifier = Modifier.clickable(onClick = onPreviewPdf)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Default.PictureAsPdf,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "معاينة PDF",
                            color = Color.White,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }

                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "تعديل", tint = OliveGoldDark)
                }

                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "نسخ أو تحويل", tint = OliveGoldDark)
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("نسخ كفاتورة جديدة") },
                            leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onDuplicate(null)
                            }
                        )
                        if (inv.sector != "فاتورة") {
                            DropdownMenuItem(
                                text = { Text("تحويل إلى «فاتورة» نهائية") },
                                leadingIcon = { Icon(Icons.Default.SwapHoriz, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    onDuplicate("فاتورة")
                                }
                            )
                        }
                        if (inv.sector != "عرض") {
                            DropdownMenuItem(
                                text = { Text("نسخ كـ «عرض» سعر") },
                                leadingIcon = { Icon(Icons.Default.SwapHoriz, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    onDuplicate("عرض")
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "حذف", tint = AccentDanger)
                }
            }
        }
    }
}
