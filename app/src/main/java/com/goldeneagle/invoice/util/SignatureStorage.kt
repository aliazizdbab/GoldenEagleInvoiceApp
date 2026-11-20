package com.goldeneagle.invoice.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import java.io.File
import java.io.FileOutputStream

data class StrokePoint(val x: Float, val y: Float)

object SignatureStorage {

    fun saveStrokesAsPng(
        context: Context,
        strokes: List<List<StrokePoint>>,
        canvasWidth: Float,
        canvasHeight: Float,
        fileName: String = "sig_${System.currentTimeMillis()}.png"
    ): String? {
        if (strokes.isEmpty() || canvasWidth <= 0f || canvasHeight <= 0f) return null
        val targetW = 600
        val targetH = ((canvasHeight / canvasWidth) * targetW).toInt().coerceIn(180, 420)
        val scaleX = targetW / canvasWidth
        val scaleY = targetH / canvasHeight

        val bitmap = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.TRANSPARENT)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0D2558") // Dark blue pen ink
            style = Paint.Style.STROKE
            strokeWidth = 5.5f
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

        for (stroke in strokes) {
            if (stroke.isEmpty()) continue
            val path = Path()
            path.moveTo(stroke.first().x * scaleX, stroke.first().y * scaleY)
            if (stroke.size == 1) {
                canvas.drawCircle(stroke.first().x * scaleX, stroke.first().y * scaleY, 2.5f, paint)
            } else {
                for (i in 1 until stroke.size) {
                    val prev = stroke[i - 1]
                    val curr = stroke[i]
                    val midX = ((prev.x + curr.x) / 2f) * scaleX
                    val midY = ((prev.y + curr.y) / 2f) * scaleY
                    path.quadTo(prev.x * scaleX, prev.y * scaleY, midX, midY)
                }
                val last = stroke.last()
                path.lineTo(last.x * scaleX, last.y * scaleY)
                canvas.drawPath(path, paint)
            }
        }

        val dir = File(context.filesDir, "signatures").apply { mkdirs() }
        val file = File(dir, fileName)
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        bitmap.recycle()
        return file.absolutePath
    }
}
