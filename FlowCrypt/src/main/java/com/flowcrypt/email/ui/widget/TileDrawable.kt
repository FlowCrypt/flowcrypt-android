/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.ui.widget

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Shader
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.core.graphics.drawable.toBitmap

/*
 * Taken from https://gist.github.com/nickbutcher/4179642450db266f0a33837f2622ace3. Modified.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
class TileDrawable(drawable: Drawable, tileMode: Shader.TileMode) : Drawable() {

  private val paint: Paint

  init {
    paint = Paint().apply {
      shader = BitmapShader(getBitmap(drawable), tileMode, tileMode)
    }
  }

  override fun draw(canvas: Canvas) {
    canvas.drawPaint(paint)
  }

  override fun setAlpha(alpha: Int) {
    paint.alpha = alpha
  }

  @Deprecated(
    "Deprecated in Java",
    ReplaceWith("PixelFormat.TRANSLUCENT", "android.graphics.PixelFormat")
  )
  override fun getOpacity() = PixelFormat.TRANSLUCENT

  override fun setColorFilter(colorFilter: ColorFilter?) {
    paint.colorFilter = colorFilter
  }

  private fun getBitmap(drawable: Drawable): Bitmap {
    return if (drawable is BitmapDrawable) {
      drawable.bitmap
    } else {
      drawable.toBitmap()
    }
  }
}