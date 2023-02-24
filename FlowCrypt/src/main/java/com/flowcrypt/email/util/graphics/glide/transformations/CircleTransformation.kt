/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.util.graphics.glide.transformations

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Shader

import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation

import java.security.MessageDigest

/**
 * The circle transformation for the Glide.
 * For details see https://stackoverflow
 * .com/questions/25278821/how-to-round-an-image-with-glide-library/25806229#25806229
 *
 * @author Denys Bondarenko
 */
class CircleTransformation : BitmapTransformation() {

  val id: String
    get() = javaClass.name

  override fun updateDiskCacheKey(messageDigest: MessageDigest) {

  }

  override fun transform(bitmapPool: BitmapPool, bitmap: Bitmap, i: Int, i1: Int): Bitmap? {
    return circleCrop(bitmapPool, bitmap)
  }

  private fun circleCrop(pool: BitmapPool, source: Bitmap?): Bitmap? {
    if (source == null) {
      return null
    }

    val size = Math.min(source.width, source.height)
    val x = (source.width - size) / 2
    val y = (source.height - size) / 2

    val squared = Bitmap.createBitmap(source, x, y, size, size)
    val result = pool.get(size, size, Bitmap.Config.ARGB_8888)

    val canvas = Canvas(result)
    val paint = Paint()
    paint.shader = BitmapShader(squared, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
    paint.isAntiAlias = true
    val r = size / 2f
    canvas.drawCircle(r, r, r, paint)
    return result
  }
}
