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
class EmptyRecyclerViewMatcher<T : View> : BaseMatcher<T>() {
  override fun matches(item: Any): Boolean {
    return (item as? RecyclerView)?.adapter?.itemCount == 0
  }

  override fun describeTo(description: Description) {
    description.appendText("RecyclerView is not empty")
  }
}
