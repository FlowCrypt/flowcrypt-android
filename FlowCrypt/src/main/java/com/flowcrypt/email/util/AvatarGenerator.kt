/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import kotlin.math.abs

/**
 * @author Denys Bondarenko
 */
object AvatarGenerator {

  private val textPaint = Paint().apply {
    color = Color.WHITE
    isAntiAlias = true
    style = Paint.Style.FILL
    textAlign = Paint.Align.CENTER
  }

  private val COLORS = listOf(
    0xFF1ABC9C,
    0xFF2ECC71,
    0xFF3498DB,
    0xFF9B59B6,
    0xFF34495E,
    0xFF16A085,
    0xFF27AE60,
    0xFF2980B9,
    0xFF8E44AD,
    0xFF2C3E50,
    0xFFF1C40F,
    0xFFE67E22,
    0xFFE74C3C,
    0xFF95A5A6,
    0xFFF39C12,
    0xFFD35400,
    0xFFC0392B,
    0xFFBDC3C7,
    0xFF7F8C8D,
  )

  fun generate(text: String, bitmapWidth: Int, bitmapHeight: Int, fontSize: Float): Bitmap {
    val bitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    //draw a circle
    canvas.drawCircle(
      bitmapWidth / 2F,
      bitmapHeight / 2F,
      minOf(bitmapWidth, bitmapHeight) / 2F,
      Paint().apply { color = getColor(text) }
    )

    //draw a text
    textPaint.textSize = fontSize
    canvas.drawText(
      text.substring(0, 2).uppercase(), (bitmapWidth / 2).toFloat(),
      bitmapHeight / 2 - (textPaint.descent() + textPaint.ascent()) / 2, textPaint
    )

    return bitmap
  }

  private fun getColor(key: Any): Int {
    return COLORS[abs(key.hashCode()) % COLORS.size].toInt()
  }
}