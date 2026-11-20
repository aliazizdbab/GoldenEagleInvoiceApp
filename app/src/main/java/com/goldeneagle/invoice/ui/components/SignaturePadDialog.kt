package com.goldeneagle.invoice.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.goldeneagle.invoice.ui.theme.BorderSoft
import com.goldeneagle.invoice.ui.theme.InkMuted
import com.goldeneagle.invoice.util.SignatureStorage
import com.goldeneagle.invoice.util.StrokePoint

@Composable
fun SignaturePadDialog(
    title: String = "رسم التوقيع اليدوي",
    onDismiss: () -> Unit,
    onSaved: (String) -> Unit
) {
    val context = LocalContext.current
    val strokes = remember { mutableStateListOf<MutableList<StrokePoint>>() }
    var canvasW by remember { mutableFloatStateOf(0f) }
    var canvasH by remember { mutableFloatStateOf(0f) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "وقّع بإصبعك داخل الإطار الأبيض أدناه",
                    style = MaterialTheme.typography.bodySmall,
                    color = InkMuted
                )
                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .border(1.5.dp, BorderSoft, RoundedCornerShape(12.dp))
                        .onSizeChanged {
                            canvasW = it.width.toFloat()
                            canvasH = it.height.toFloat()
                        }
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    strokes.add(mutableStateListOf(StrokePoint(offset.x, offset.y)))
                                },
                                onDrag = { change, _ ->
                                    change.consume()
                                    val pos = change.position
                                    if (pos.x in 0f..size.width.toFloat() && pos.y in 0f..size.height.toFloat()) {
                                        strokes.lastOrNull()?.add(StrokePoint(pos.x, pos.y))
                                    }
                                }
                            )
                        }
                ) {
                    if (strokes.isEmpty()) {
                        Text(
                            text = "منطقة التوقيع",
                            color = Color(0xFFCCCCCC),
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }

                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val inkColor = Color(0xFF0D2558)
                        strokes.forEach { pts ->
                            if (pts.size == 1) {
                                drawCircle(
                                    color = inkColor,
                                    radius = 3f,
                                    center = androidx.compose.ui.geometry.Offset(pts[0].x, pts[0].y)
                                )
                            } else if (pts.size > 1) {
                                val path = Path().apply {
                                    moveTo(pts.first().x, pts.first().y)
                                    for (i in 1 until pts.size) {
                                        val prev = pts[i - 1]
                                        val curr = pts[i]
                                        quadraticTo(
                                            prev.x,
                                            prev.y,
                                            (prev.x + curr.x) / 2f,
                                            (prev.y + curr.y) / 2f
                                        )
                                    }
                                    lineTo(pts.last().x, pts.last().y)
                                }
                                drawPath(
                                    path = path,
                                    color = inkColor,
                                    style = Stroke(
                                        width = 5f,
                                        cap = StrokeCap.Round,
                                        join = StrokeJoin.Round
                                    )
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("إلغاء")
                    }
                    OutlinedButton(
                        onClick = { strokes.clear() },
                        enabled = strokes.isNotEmpty()
                    ) {
                        Text("مسح")
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Button(
                        enabled = strokes.isNotEmpty(),
                        onClick = {
                            val savedPath = SignatureStorage.saveStrokesAsPng(
                                context = context,
                                strokes = strokes.map { it.toList() },
                                canvasWidth = canvasW,
                                canvasHeight = canvasH
                            )
                            if (savedPath != null) {
                                onSaved(savedPath)
                            }
                        }
                    ) {
                        Text("حفظ التوقيع")
                    }
                }
            }
        }
    }
}
