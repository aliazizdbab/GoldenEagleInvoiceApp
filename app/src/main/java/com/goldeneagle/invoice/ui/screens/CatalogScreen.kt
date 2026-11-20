package com.goldeneagle.invoice.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.goldeneagle.invoice.data.entity.CatalogItemEntity
import com.goldeneagle.invoice.data.model.CurrencyCode
import com.goldeneagle.invoice.data.model.DecimalMode
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
fun CatalogScreen(viewModel: MainViewModel) {
    val catalog by viewModel.catalog.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val decimalMode = DecimalMode.fromString(settings.decimalMode)

    var searchQuery by remember { mutableStateOf("") }
    var editingItem by remember { mutableStateOf<CatalogItemEntity?>(null) }
    var showEditorDialog by remember { mutableStateOf(false) }
    var deletingItem by remember { mutableStateOf<CatalogItemEntity?>(null) }

    val filtered = remember(catalog, searchQuery) {
        val q = searchQuery.trim()
        if (q.isEmpty()) catalog
        else catalog.filter {
            it.name.contains(q, ignoreCase = true) ||
                it.category.contains(q, ignoreCase = true)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("كتالوج الأصناف والخدمات", color = Color.White, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "أصناف جاهزة للإضافة السريعة في الفواتير",
                            color = Color.White.copy(alpha = 0.85f),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = OliveGold)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    editingItem = null
                    showEditorDialog = true
                },
                containerColor = OliveGold,
                contentColor = Color.White,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("صنف جديد") }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CardWhite)
                    .padding(14.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("ابحث باسم الصنف أو القسم...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = OliveGold) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }

            if (filtered.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyStateView(
                        icon = Icons.Default.Inventory2,
                        title = "لا توجد أصناف",
                        subtitle = "أضف أجهزتك وخدماتك المتكررة مع أسعارها لتسريع إصدار الفواتير"
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 92.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filtered, key = { it.id }) { item ->
                        val curr = CurrencyCode.fromString(item.currencyCode)
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = CardWhite),
                            border = BorderStroke(1.dp, BorderSoft)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.name,
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = OliveGoldLight
                                        ) {
                                            Text(
                                                text = item.category.ifBlank { "عام" },
                                                style = MaterialTheme.typography.bodySmall,
                                                color = OliveGoldDark,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                            )
                                        }
                                        Text(
                                            text = CurrencyFormatter.formatMoney(item.defaultPrice, curr, decimalMode),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = OliveGoldDark
                                        )
                                    }
                                }
                                IconButton(onClick = {
                                    editingItem = item
                                    showEditorDialog = true
                                }) {
                                    Icon(Icons.Default.Edit, contentDescription = "تعديل", tint = OliveGoldDark)
                                }
                                IconButton(onClick = { deletingItem = item }) {
                                    Icon(Icons.Default.Delete, contentDescription = "حذف", tint = AccentDanger)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showEditorDialog) {
        CatalogItemDialog(
            initial = editingItem,
            defaultCurrency = CurrencyCode.fromString(settings.defaultCurrency),
            onDismiss = { showEditorDialog = false },
            onSave = { item ->
                viewModel.saveCatalogItem(item)
                showEditorDialog = false
            }
        )
    }

    deletingItem?.let { item ->
        ConfirmDeleteDialog(
            title = "حذف الصنف",
            message = "هل تريد حذف «${item.name}» من كتالوج الأصناف؟",
            onConfirm = {
                viewModel.deleteCatalogItem(item)
                deletingItem = null
            },
            onDismiss = { deletingItem = null }
        )
    }
}

@Composable
private fun CatalogItemDialog(
    initial: CatalogItemEntity?,
    defaultCurrency: CurrencyCode,
    onDismiss: () -> Unit,
    onSave: (CatalogItemEntity) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var priceText by remember { mutableStateOf(if (initial != null) initial.defaultPrice.toString() else "") }
    var category by remember { mutableStateOf(initial?.category ?: "كاميرات مراقبة") }
    var currency by remember { mutableStateOf(CurrencyCode.fromString(initial?.currencyCode ?: defaultCurrency.name)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "إضافة صنف للكتالوج" else "تعديل الصنف") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("وصف / اسم الصنف") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it },
                    label = { Text("السعر الافتراضي") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("القسم (كاميرات، شبكات، كهرباء، حريق...)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("العملة الافتراضية للصنف:", style = MaterialTheme.typography.bodySmall, color = InkMuted)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    CurrencyCode.entries.forEach { code ->
                        FilterChip(
                            selected = currency == code,
                            onClick = { currency = code },
                            label = { Text(code.symbol) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = name.isNotBlank(),
                onClick = {
                    val parsedPrice = priceText.replace(",", ".").toDoubleOrNull() ?: 0.0
                    onSave(
                        CatalogItemEntity(
                            id = initial?.id ?: 0L,
                            name = name.trim(),
                            defaultPrice = parsedPrice,
                            category = category.trim().ifBlank { "عام" },
                            currencyCode = currency.name
                        )
                    )
                }
            ) {
                Text("حفظ")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}
