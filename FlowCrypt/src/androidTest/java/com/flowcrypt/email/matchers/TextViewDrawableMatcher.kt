/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.matchers

import android.graphics.drawable.Drawable
import android.view.View
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.IntDef
import androidx.core.graphics.drawable.toBitmap
import org.hamcrest.Description
import org.hamcrest.TypeSafeMatcher

/**
 * @author Denys Bondarenko
 */
class TextViewDrawableMatcher(
  private val drawable: Drawable? = null,
  @DrawableRes private val resourceId: Int = 0,
  @DrawablePosition private val drawablePosition: Int
) : TypeSafeMatcher<View>() {
  override fun describeTo(description: Description) {
    description.appendText(
      "TextView with compound $drawable at position $drawablePosition" +
          " same as drawable with id $resourceId"
    )
  }

  override fun matchesSafely(view: View): Boolean {
    if (view !is TextView) {
      return false
    }

    val expectedBitmap = drawable?.toBitmap() ?: view.context.getDrawable(resourceId)?.toBitmap()
    return view.compoundDrawables[drawablePosition].toBitmap().sameAs(expectedBitmap)
  }

  @Retention(AnnotationRetention.SOURCE)
  @IntDef(
    DrawablePosition.LEFT,
    DrawablePosition.TOP,
    DrawablePosition.RIGHT,
    DrawablePosition.BOTTOM
  )
  annotation class DrawablePosition {
    companion object {
      const val LEFT = 0
      const val TOP = 1
      const val RIGHT = 2
      const val BOTTOM = 3
    }
  }
}
