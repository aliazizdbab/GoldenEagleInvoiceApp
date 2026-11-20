package com.goldeneagle.invoice.ui.screens

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.goldeneagle.invoice.R
import com.goldeneagle.invoice.data.model.CurrencyCode
import com.goldeneagle.invoice.data.model.DecimalMode
import com.goldeneagle.invoice.data.model.LogoStyle
import com.goldeneagle.invoice.ui.components.SectionCard
import com.goldeneagle.invoice.ui.components.SignaturePadDialog
import com.goldeneagle.invoice.ui.theme.AccentDanger
import com.goldeneagle.invoice.ui.theme.BorderSoft
import com.goldeneagle.invoice.ui.theme.InkMuted
import com.goldeneagle.invoice.ui.theme.OliveGold
import com.goldeneagle.invoice.ui.theme.OliveGoldLight
import com.goldeneagle.invoice.util.DateHelper
import com.goldeneagle.invoice.viewmodel.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: MainViewModel) {
    val savedSettings by viewModel.settings.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var cityLine by remember { mutableStateOf(savedSettings.cityLine) }
    var phone1 by remember { mutableStateOf(savedSettings.phone1) }
    var phone2 by remember { mutableStateOf(savedSettings.phone2) }
    var website by remember { mutableStateOf(savedSettings.website) }
    var payableTitle by remember { mutableStateOf(savedSettings.payableToTitle) }
    var payableCompany by remember { mutableStateOf(savedSettings.payableToCompany) }
    var approvalText by remember { mutableStateOf(savedSettings.approvalText) }
    var thankYouText by remember { mutableStateOf(savedSettings.thankYouText) }
    var logoStyle by remember { mutableStateOf(LogoStyle.fromString(savedSettings.logoStyle)) }
    var defaultCurrency by remember { mutableStateOf(CurrencyCode.fromString(savedSettings.defaultCurrency)) }
    var decimalMode by remember { mutableStateOf(DecimalMode.fromString(savedSettings.decimalMode)) }
    var defaultTaxText by remember { mutableStateOf(savedSettings.defaultTaxPercent.toString()) }
    var defaultInvoiceNotes by remember { mutableStateOf(savedSettings.defaultInvoiceNotes) }
    var defaultQuoteNotes by remember { mutableStateOf(savedSettings.defaultQuoteNotes) }
    var defaultSignaturePath by remember { mutableStateOf(savedSettings.defaultSignaturePath) }

    var showSignaturePad by remember { mutableStateOf(false) }

    LaunchedEffect(savedSettings) {
        cityLine = savedSettings.cityLine
        phone1 = savedSettings.phone1
        phone2 = savedSettings.phone2
        website = savedSettings.website
        payableTitle = savedSettings.payableToTitle
        payableCompany = savedSettings.payableToCompany
        approvalText = savedSettings.approvalText
        thankYouText = savedSettings.thankYouText
        logoStyle = LogoStyle.fromString(savedSettings.logoStyle)
        defaultCurrency = CurrencyCode.fromString(savedSettings.defaultCurrency)
        decimalMode = DecimalMode.fromString(savedSettings.decimalMode)
        defaultTaxText = if (savedSettings.defaultTaxPercent == 0.0) "0" else savedSettings.defaultTaxPercent.toString()
        defaultInvoiceNotes = savedSettings.defaultInvoiceNotes
        defaultQuoteNotes = savedSettings.defaultQuoteNotes
        defaultSignaturePath = savedSettings.defaultSignaturePath
    }

    // Export Backup SAF launcher
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val res = viewModel.exportBackupJson()
                res.onSuccess { jsonStr ->
                    runCatching {
                        withContext(Dispatchers.IO) {
                            context.contentResolver.openOutputStream(uri)?.use { out ->
                                out.write(jsonStr.toByteArray(Charsets.UTF_8))
                            } ?: error("تعذر الكتابة إلى الملف المختار")
                        }
                    }.onSuccess {
                        snackbarHostState.showSnackbar("تم تصدير النسخة الاحتياطية بنجاح")
                    }.onFailure { err ->
                        snackbarHostState.showSnackbar("فشل التصدير: ${err.message}")
                    }
                }.onFailure { err ->
                    snackbarHostState.showSnackbar("فشل تجهيز النسخة: ${err.message}")
                }
            }
        }
    }

    // Import Backup SAF launcher
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val readRes = runCatching {
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use {
                            it.readText()
                        } ?: error("تعذر قراءة الملف")
                    }
                }
                readRes.onSuccess { rawJson ->
                    val importRes = viewModel.importBackupJson(rawJson)
                    importRes.onSuccess { count ->
                        snackbarHostState.showSnackbar("تمت الاستعادة بنجاح ($count فاتورة)")
                    }.onFailure { err ->
                        snackbarHostState.showSnackbar("ملف غير صالح: ${err.message}")
                    }
                }.onFailure { err ->
                    snackbarHostState.showSnackbar("فشل فتح الملف: ${err.message}")
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("الإعدادات والنسخ الاحتياطي", color = Color.White, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "بيانات الشركة، الشعار، العملة، وحفظ البيانات",
                            color = Color.White.copy(alpha = 0.85f),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = OliveGold)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Logo Style Card
            SectionCard(title = "شعار الشركة في الفاتورة PDF", icon = Icons.Default.Image) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    LogoChoiceCard(
                        title = "أحمر وذهبي",
                        subtitle = "مطابق لـ 26-0101",
                        resId = R.drawable.logo_red_gold,
                        selected = logoStyle == LogoStyle.RED_GOLD,
                        onClick = { logoStyle = LogoStyle.RED_GOLD },
                        modifier = Modifier.weight(1f)
                    )
                    LogoChoiceCard(
                        title = "ذهبي كامل",
                        subtitle = "النسر الذهبي شعار",
                        resId = R.drawable.logo_full_gold,
                        selected = logoStyle == LogoStyle.FULL_GOLD,
                        onClick = { logoStyle = LogoStyle.FULL_GOLD },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 2. Currency & Decimals Card
            SectionCard(title = "العملة الافتراضية والخانات العشرية", icon = Icons.Default.CurrencyExchange) {
                Text("العملة الافتراضية للفواتير الجديدة:", style = MaterialTheme.typography.bodySmall, color = InkMuted)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CurrencyCode.entries.forEach { code ->
                        FilterChip(
                            selected = defaultCurrency == code,
                            onClick = { defaultCurrency = code },
                            label = { Text("${code.arabicName} (${code.symbol})") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text("عرض الخانات العشرية:", style = MaterialTheme.typography.bodySmall, color = InkMuted)
                DecimalMode.entries.forEach { mode ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { decimalMode = mode }
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = decimalMode == mode, onClick = { decimalMode = mode })
                        Text(mode.arabicLabel, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = defaultTaxText,
                    onValueChange = { defaultTaxText = it },
                    label = { Text("نسبة الضريبة الافتراضية (%)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 3. Company Contact Info Card
            SectionCard(title = "بيانات الترويسة والتذييل", icon = Icons.Default.Business) {
                OutlinedTextField(
                    value = cityLine,
                    onValueChange = { cityLine = it },
                    label = { Text("المدينة / العنوان") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = phone1,
                        onValueChange = { phone1 = it },
                        label = { Text("الهاتف 1") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = phone2,
                        onValueChange = { phone2 = it },
                        label = { Text("الهاتف 2") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = website,
                    onValueChange = { website = it },
                    label = { Text("الموقع الإلكتروني") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = payableTitle,
                        onValueChange = { payableTitle = it },
                        label = { Text("عنوان الاستحقاق") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = payableCompany,
                        onValueChange = { payableCompany = it },
                        label = { Text("اسم الشركة المستفيدة") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = approvalText,
                    onValueChange = { approvalText = it },
                    label = { Text("عبارة الإقرار أسفل الفاتورة") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = thankYouText,
                    onValueChange = { thankYouText = it },
                    label = { Text("عبارة الشكر السفلية") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 4. Default Notes Templates
            SectionCard(title = "قوالب الملاحظات الافتراضية", icon = Icons.AutoMirrored.Filled.Notes) {
                OutlinedTextField(
                    value = defaultInvoiceNotes,
                    onValueChange = { defaultInvoiceNotes = it },
                    label = { Text("ملاحظات الفاتورة الافتراضية") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = defaultQuoteNotes,
                    onValueChange = { defaultQuoteNotes = it },
                    label = { Text("ملاحظات عرض السعر الافتراضية") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 5. Default Hand-drawn Signature
            SectionCard(title = "التوقيع اليدوي الافتراضي", icon = Icons.Default.Draw) {
                val sigBmp = remember(defaultSignaturePath) {
                    defaultSignaturePath?.let { File(it).takeIf { f -> f.exists() }?.let { f -> BitmapFactory.decodeFile(f.absolutePath) } }
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
                                contentDescription = "التوقيع المحفوظ",
                                modifier = Modifier
                                    .width(120.dp)
                                    .height(50.dp)
                                    .padding(4.dp),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }
                    OutlinedButton(
                        onClick = { showSignaturePad = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (sigBmp == null) "رسم توقيع افتراضي" else "تغيير التوقيع")
                    }
                    if (defaultSignaturePath != null) {
                        IconButton(onClick = { defaultSignaturePath = null }) {
                            Icon(Icons.Default.Delete, contentDescription = "حذف التوقيع", tint = AccentDanger)
                        }
                    }
                }
            }

            // Save Settings Button
            Button(
                onClick = {
                    val tax = defaultTaxText.replace(",", ".").toDoubleOrNull() ?: 0.0
                    viewModel.saveSettings(
                        savedSettings.copy(
                            cityLine = cityLine.trim(),
                            phone1 = phone1.trim(),
                            phone2 = phone2.trim(),
                            website = website.trim(),
                            payableToTitle = payableTitle.trim(),
                            payableToCompany = payableCompany.trim(),
                            approvalText = approvalText.trim(),
                            thankYouText = thankYouText.trim(),
                            logoStyle = logoStyle.name,
                            defaultCurrency = defaultCurrency.name,
                            decimalMode = decimalMode.name,
                            defaultTaxPercent = tax,
                            defaultInvoiceNotes = defaultInvoiceNotes,
                            defaultQuoteNotes = defaultQuoteNotes,
                            defaultSignaturePath = defaultSignaturePath
                        )
                    )
                    scope.launch { snackbarHostState.showSnackbar("تم حفظ الإعدادات بنجاح") }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = OliveGold),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("حفظ الإعدادات")
            }

            // 6. Backup & Restore Card
            SectionCard(title = "النسخ الاحتياطي والاستعادة", icon = Icons.Default.Backup) {
                Text(
                    "احفظ نسخة كاملة من جميع فواتيرك وأصنافك وزبائنك وإعداداتك في ملف واحد لنقلها لهاتف آخر أو استعادتها لاحقاً.",
                    style = MaterialTheme.typography.bodySmall,
                    color = InkMuted
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            exportLauncher.launch("GoldenEagle_Backup_${DateHelper.todayFormatted()}.json")
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = OliveGold),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Backup, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("تصدير نسخة")
                    }
                    OutlinedButton(
                        onClick = { importLauncher.launch(arrayOf("application/json", "*/*")) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("استعادة نسخة")
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showSignaturePad) {
        SignaturePadDialog(
            title = "رسم التوقيع الافتراضي للشركة",
            onDismiss = { showSignaturePad = false },
            onSaved = { path ->
                defaultSignaturePath = path
                showSignaturePad = false
            }
        )
    }
}

@Composable
private fun LogoChoiceCard(
    title: String,
    subtitle: String,
    resId: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (selected) OliveGoldLight else Color.White,
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) OliveGold else BorderSoft)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(resId),
                contentDescription = title,
                modifier = Modifier.size(64.dp),
                contentScale = ContentScale.Fit
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = InkMuted)
        }
    }
}
