/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.matchers

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.view.View
import android.widget.ImageView
import org.hamcrest.Description
import org.hamcrest.TypeSafeMatcher

/**
 * See details here https://github.com/dbottillo/Blog/blob/espresso_match_imageview/app/src/androidTest/java/com
 * /danielebottillo/blog/config/DrawableMatcher.java
 *
 * @author Denis Bondarenko
 * Date: 3/15/19
 * Time: 5:17 PM
 * E-mail: DenBond7@gmail.com
 */
class DrawableMatcher(private val expectedId: Int) : TypeSafeMatcher<View>(View::class.java) {
  private var resourceName: String? = null

  override fun describeTo(description: Description) {
    description.appendText("with drawable from resource id: ")
    description.appendValue(expectedId)
    resourceName?.let { description.appendText("[").appendText(it).appendText("]") }
  }

  override fun matchesSafely(target: View): Boolean {
    if (target !is ImageView) {
      return false
    }

    when (expectedId) {
      EMPTY -> return target.drawable == null
      ANY -> return target.drawable != null
      else -> {
        val resources = target.getContext().resources
        val expectedDrawable =
          resources.getDrawable(expectedId, target.getContext().theme) ?: return false
        resourceName = resources.getResourceEntryName(expectedId)

        val bitmap = getBitmap(target.drawable)
        val otherBitmap = getBitmap(expectedDrawable)
        return bitmap.sameAs(otherBitmap)
      }
    }
  }

  private fun getBitmap(drawable: Drawable): Bitmap {
    val bitmap = Bitmap.createBitmap(
      drawable.intrinsicWidth,
      drawable.intrinsicHeight,
      Bitmap.Config.ARGB_8888
    )
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return bitmap
  }

  companion object {
    const val EMPTY = -1
    const val ANY = -2
  }
}
