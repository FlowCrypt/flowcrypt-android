/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.matchers

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import org.hamcrest.BaseMatcher
import org.hamcrest.Description

/**
 * @author Denis Bondarenko
 *         Date: 5/3/19
 *         Time: 3:47 PM
 *         E-mail: DenBond7@gmail.com
 */
class RecyclerViewItemCountMatcher<T : View>(private val itemCount: Int) : BaseMatcher<T>() {
  override fun matches(item: Any): Boolean {
    return if (item is RecyclerView) {
      item.adapter?.itemCount == itemCount
    } else {
      false
    }
  }

  override fun describeTo(description: Description) {
    description.appendText("The count of the RecyclerView is not equal = $itemCount")
  }
}
