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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.goldeneagle.invoice.data.entity.CustomerEntity
import com.goldeneagle.invoice.ui.components.ConfirmDeleteDialog
import com.goldeneagle.invoice.ui.components.EmptyStateView
import com.goldeneagle.invoice.ui.theme.AccentDanger
import com.goldeneagle.invoice.ui.theme.BorderSoft
import com.goldeneagle.invoice.ui.theme.CardWhite
import com.goldeneagle.invoice.ui.theme.InkMuted
import com.goldeneagle.invoice.ui.theme.OliveGold
import com.goldeneagle.invoice.ui.theme.OliveGoldDark
import com.goldeneagle.invoice.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomersScreen(
    viewModel: MainViewModel,
    onCreateInvoiceForCustomer: (CustomerEntity) -> Unit
) {
    val customers by viewModel.customers.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var editingCustomer by remember { mutableStateOf<CustomerEntity?>(null) }
    var showEditorDialog by remember { mutableStateOf(false) }
    var deletingCustomer by remember { mutableStateOf<CustomerEntity?>(null) }

    val filtered = remember(customers, searchQuery) {
        val q = searchQuery.trim()
        if (q.isEmpty()) customers
        else customers.filter {
            it.name.contains(q, ignoreCase = true) ||
                it.companyName.contains(q, ignoreCase = true) ||
                it.phone.contains(q, ignoreCase = true)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("دليل الزبائن", color = Color.White, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "تعبئة تلقائية سريعة لبيانات العملاء في الفواتير",
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
                    editingCustomer = null
                    showEditorDialog = true
                },
                containerColor = OliveGold,
                contentColor = Color.White,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("زبون جديد") }
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
                    placeholder = { Text("ابحث بالاسم، اسم الشركة، أو رقم الهاتف...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = OliveGold) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }

            if (filtered.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyStateView(
                        icon = Icons.Default.People,
                        title = "لا يوجد زبائن محفوظون بعد",
                        subtitle = "يُحفظ الزبائن تلقائياً عند إصدار الفواتير، أو يمكنك إضافتهم يدوياً من هنا"
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 92.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filtered, key = { it.id }) { customer ->
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
                                        text = customer.name,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    if (customer.companyName.isNotBlank()) {
                                        Text(
                                            text = customer.companyName,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = InkMuted
                                        )
                                    }
                                    if (customer.phone.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Phone,
                                                contentDescription = null,
                                                tint = OliveGold,
                                                modifier = Modifier.height(14.dp)
                                            )
                                            Text(
                                                text = customer.phone,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = OliveGoldDark
                                            )
                                        }
                                    }
                                }
                                IconButton(onClick = { onCreateInvoiceForCustomer(customer) }) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.ReceiptLong,
                                        contentDescription = "إنشاء فاتورة للزبون",
                                        tint = OliveGold
                                    )
                                }
                                IconButton(onClick = {
                                    editingCustomer = customer
                                    showEditorDialog = true
                                }) {
                                    Icon(Icons.Default.Edit, contentDescription = "تعديل", tint = OliveGoldDark)
                                }
                                IconButton(onClick = { deletingCustomer = customer }) {
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
        CustomerEditorDialog(
            initial = editingCustomer,
            onDismiss = { showEditorDialog = false },
            onSave = { customer ->
                viewModel.saveCustomer(customer)
                showEditorDialog = false
            }
        )
    }

    deletingCustomer?.let { c ->
        ConfirmDeleteDialog(
            title = "حذف الزبون",
            message = "هل تريد حذف «${c.name}» من دليل الزبائن؟ (لن تتأثر فواتيره السابقة)",
            onConfirm = {
                viewModel.deleteCustomer(c)
                deletingCustomer = null
            },
            onDismiss = { deletingCustomer = null }
        )
    }
}

@Composable
private fun CustomerEditorDialog(
    initial: CustomerEntity?,
    onDismiss: () -> Unit,
    onSave: (CustomerEntity) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var companyName by remember { mutableStateOf(initial?.companyName ?: "") }
    var phone by remember { mutableStateOf(initial?.phone ?: "") }
    var notes by remember { mutableStateOf(initial?.notes ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "إضافة زبون جديد" else "تعديل بيانات الزبون") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("اسم الزبون (إلى السيد)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = companyName,
                    onValueChange = { companyName = it },
                    label = { Text("اسم الشركة (اختياري)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("رقم الهاتف") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("ملاحظات (اختياري)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                enabled = name.isNotBlank(),
                onClick = {
                    onSave(
                        CustomerEntity(
                            id = initial?.id ?: 0L,
                            name = name.trim(),
                            companyName = companyName.trim(),
                            phone = phone.trim(),
                            notes = notes.trim()
                        )
                    )
                }
            ) { Text("حفظ") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}
