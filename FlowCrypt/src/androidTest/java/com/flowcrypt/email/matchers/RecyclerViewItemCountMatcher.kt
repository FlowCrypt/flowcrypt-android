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
 * @author Denys Bondarenko
 */
class RecyclerViewItemCountMatcher<T : View>(private val itemCount: Int) : BaseMatcher<T>() {
  override fun matches(item: Any): Boolean {
    return (item as? RecyclerView)?.adapter?.itemCount == itemCount
  }

  override fun describeTo(description: Description) {
    description.appendText("The count of the RecyclerView is not equal = $itemCount")
  }
}
