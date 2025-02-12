/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.matchers

import android.view.View
import android.widget.ListView
import org.hamcrest.BaseMatcher
import org.hamcrest.Description

/**
 * @author Denys Bondarenko
 */
class EmptyListViewMather<T : View> : BaseMatcher<T>() {
  override fun matches(item: Any): Boolean {
    return (item as? ListView)?.adapter?.count == 0
  }

  override fun describeTo(description: Description) {
    description.appendText("List is not empty")
  }
}
