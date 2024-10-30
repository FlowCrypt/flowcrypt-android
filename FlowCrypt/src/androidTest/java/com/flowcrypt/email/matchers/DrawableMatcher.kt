/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.matchers

import android.content.res.ColorStateList
import android.graphics.PorterDuff
import android.view.View
import android.widget.ImageView
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.google.android.material.button.MaterialButton
import org.hamcrest.Description
import org.hamcrest.TypeSafeMatcher

/**
 * See details here https://github.com/dbottillo/Blog/blob/espresso_match_imageview/app/src/androidTest/java/com
 * /danielebottillo/blog/config/DrawableMatcher.java
 *
 * and here https://medium.com/@miloszlewandowski/espresso-matcher-for-imageview-made-easy-with-android-ktx-977374ca3391
 *
 * @author Denys Bondarenko
 */
class DrawableMatcher(
  @DrawableRes private val expectedId: Int,
  @ColorRes private val tintColor: Int? = null,
  private val tintMode: PorterDuff.Mode = PorterDuff.Mode.SRC_IN
) : TypeSafeMatcher<View>(View::class.java) {
  private var resourceName: String? = null

  override fun describeTo(description: Description) {
    description.appendText("with drawable from resource id: ")
    description.appendValue(expectedId)
    resourceName?.let { description.appendText("[resourceName = $resourceName]") }
    tintColor?.let { description.appendText(",[tintColor = $tintColor, mode = $tintMode]") }
  }

  override fun matchesSafely(target: View): Boolean {
    if (!(target is ImageView || target is MaterialButton)) {
      return false
    }

    val context = target.context
    val drawable = when (target) {
      is ImageView -> target.drawable
      is MaterialButton -> target.icon
      else -> null
    }

    when (expectedId) {
      EMPTY -> return drawable == null
      ANY -> return drawable != null
      else -> {
        val resources = context.resources
        resourceName = resources.getResourceEntryName(expectedId)

        val expectedDrawable = resources.getDrawable(expectedId, context.theme)?.apply {
          tintColor?.let {
            setTintList(ColorStateList.valueOf(ContextCompat.getColor(context, tintColor)))
            setTintMode(tintMode)
          }
        } ?: return false

        val actualBitmap = drawable?.toBitmap()
        val expectedBitmap = expectedDrawable.toBitmap()

        return actualBitmap?.sameAs(expectedBitmap) == true
      }
    }
  }

  companion object {
    const val EMPTY = -1
    const val ANY = -2
  }
}
