package com.goldeneagle.invoice.ui.screens

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.unit.dp
import com.goldeneagle.invoice.pdf.PdfActions
import com.goldeneagle.invoice.ui.theme.AccentDanger
import com.goldeneagle.invoice.ui.theme.AccentSuccess
import com.goldeneagle.invoice.ui.theme.BorderSoft
import com.goldeneagle.invoice.ui.theme.CardWhite
import com.goldeneagle.invoice.ui.theme.InkMuted
import com.goldeneagle.invoice.ui.theme.OliveGold
import com.goldeneagle.invoice.ui.theme.OliveGoldDark
import com.goldeneagle.invoice.ui.theme.OliveGoldLight
import com.goldeneagle.invoice.viewmodel.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfPreviewScreen(
    invoiceId: Long,
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onEdit: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var pdfFile by remember { mutableStateOf<File?>(null) }
    var titleText by remember { mutableStateOf("معاينة الفاتورة PDF") }
    var renderedPages by remember { mutableStateOf<List<Bitmap>>(emptyList()) }

    LaunchedEffect(invoiceId) {
        isLoading = true
        errorMessage = null
        val result = viewModel.generatePdfForInvoice(invoiceId)
        result.onSuccess { (invoiceWithItems, file) ->
            pdfFile = file
            titleText = "فاتورة ${invoiceWithItems.invoice.workOrderNumber}"
            val bitmaps = withContext(Dispatchers.IO) { renderPdfPages(file) }
            renderedPages.forEach { it.recycle() }
            renderedPages = bitmaps
        }.onFailure { err ->
            errorMessage = err.message ?: "تعذر إنشاء ملف PDF"
        }
        isLoading = false
    }

    DisposableEffect(Unit) {
        onDispose {
            renderedPages.forEach { it.recycle() }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(titleText, color = Color.White, style = MaterialTheme.typography.titleMedium)
                        pdfFile?.let { f ->
                            val kb = (f.length() / 1024L).coerceAtLeast(1L)
                            Text(
                                text = "${renderedPages.size} صفحة • $kb KB • A4",
                                color = Color.White.copy(alpha = 0.85f),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "رجوع",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "تعديل الفاتورة", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = OliveGold)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            pdfFile?.let { file ->
                Surface(
                    shadowElevation = 8.dp,
                    color = CardWhite,
                    border = BorderStroke(1.dp, BorderSoft)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { PdfActions.sharePdf(context, file, targetPackage = "com.whatsapp") },
                                modifier = Modifier.weight(1.2f),
                                colors = ButtonDefaults.buttonColors(containerColor = AccentSuccess),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("واتساب")
                            }
                            Button(
                                onClick = { PdfActions.sharePdf(context, file) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = OliveGold),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("مشاركة")
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        val saved = withContext(Dispatchers.IO) {
                                            PdfActions.saveToDownloads(context, file)
                                        }
                                        saved.onSuccess { loc ->
                                            snackbarHostState.showSnackbar("تم الحفظ في: $loc")
                                        }.onFailure { err ->
                                            snackbarHostState.showSnackbar("فشل الحفظ: ${err.message}")
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("حفظ بالتنزيلات")
                            }
                            OutlinedButton(
                                onClick = { PdfActions.printPdf(context, file, titleText) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("طباعة")
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFE7E2D5))
                .padding(innerPadding)
        ) {
            when {
                isLoading -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = OliveGold)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("جاري تجهيز ملف الـ PDF...", color = OliveGoldDark)
                    }
                }

                errorMessage != null -> {
                    Text(
                        text = errorMessage!!,
                        color = AccentDanger,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp)
                    )
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(14.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        itemsIndexed(renderedPages) { index, bitmap ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                            ) {
                                Column {
                                    Image(
                                        bitmap = bitmap.asImageBitmap(),
                                        contentDescription = "صفحة ${index + 1}",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .aspectRatio(595f / 842f),
                                        contentScale = ContentScale.Fit
                                    )
                                    if (renderedPages.size > 1) {
                                        Surface(
                                            color = OliveGoldLight,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = "صفحة ${index + 1} من ${renderedPages.size}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = InkMuted,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun renderPdfPages(file: File): List<Bitmap> {
    val result = mutableListOf<Bitmap>()
    ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
        PdfRenderer(pfd).use { renderer ->
            val scale = 2.2f
            for (i in 0 until renderer.pageCount) {
                renderer.openPage(i).use { page ->
                    val width = (page.width * scale).toInt()
                    val height = (page.height * scale).toInt()
                    val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(bmp)
                    canvas.drawColor(AndroidColor.WHITE)
                    page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    result.add(bmp)
                }
            }
        }
    }
    return result
}
