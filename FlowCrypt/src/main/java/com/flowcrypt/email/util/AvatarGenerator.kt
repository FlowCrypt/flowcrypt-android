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

  fun generate(text: String, bitmapWidth: Int, bitmapHeight: Int, fontSize: Float): Bitmap {
    val bitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    //draw a circle
    canvas.drawCircle(
      bitmapWidth / 2F,
      bitmapHeight / 2F,
      minOf(bitmapWidth, bitmapHeight) / 2F,
      Paint().apply { color = ColorGenerator.DEFAULT.getColor(text) }
    )

    //draw a text
    textPaint.textSize = fontSize
    canvas.drawText(
      text.substring(0, 2).uppercase(), (bitmapWidth / 2).toFloat(),
      bitmapHeight / 2 - (textPaint.descent() + textPaint.ascent()) / 2, textPaint
    )

    return bitmap
  }

  private class ColorGenerator private constructor(private val colors: List<Int>) {
    fun getColor(key: Any): Int {
      return colors[abs(key.hashCode()) % colors.size]
    }

    companion object {
      var DEFAULT = create(
        listOf(
          -0x1a8c8d,
          -0xf9d6e,
          -0x459738,
          -0x6a8a33,
          -0x867935,
          -0x9b4a0a,
          -0xb03c09,
          -0xb22f1f,
          -0xb24954,
          -0x7e387c,
          -0x512a7f,
          -0x759b,
          -0x2b1ea9,
          -0x2ab1,
          -0x48b3,
          -0x5e7781,
          -0x6f5b52
        )
      )

      fun create(colorList: List<Int>): ColorGenerator {
        return ColorGenerator(colorList)
      }
    }
  }
}