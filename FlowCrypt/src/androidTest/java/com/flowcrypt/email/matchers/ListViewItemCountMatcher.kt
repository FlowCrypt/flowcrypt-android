/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.matchers

import android.view.View
import android.widget.HeaderViewListAdapter
import android.widget.ListView
import org.hamcrest.BaseMatcher
import org.hamcrest.Description

/**
 * @author Denys Bondarenko
 */
class ListViewItemCountMatcher<T : View>(val itemCount: Int) : BaseMatcher<T>() {
  override fun matches(item: Any): Boolean {
    return if (item is ListView) {
      val adapter = item.adapter

      if (adapter is HeaderViewListAdapter) {
        adapter.count == itemCount + adapter.headersCount + adapter.footersCount
      } else {
        adapter.count == itemCount
      }
    } else {
      false
    }
  }

  override fun describeTo(description: Description) {
    description.appendText("The size of the list is not equal = $itemCount")
  }
}
