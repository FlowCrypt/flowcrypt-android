/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.matchers

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.view.View
import android.widget.ImageView
import org.hamcrest.Description
import org.hamcrest.TypeSafeMatcher

/**
 * @author Denys Bondarenko
 */
class BitmapMatcher(private val expectedBitmap: Bitmap) :
  TypeSafeMatcher<View>(View::class.java) {

  override fun describeTo(description: Description) {
    description.appendText("with expected bitmap size: ")
    description.appendValue(expectedBitmap.byteCount)
  }

  override fun matchesSafely(target: View): Boolean {
    if (target !is ImageView) {
      return false
    }

    val targetBitmap = (target.drawable as? BitmapDrawable)?.bitmap
    return targetBitmap?.sameAs(expectedBitmap) == true
  }
}
