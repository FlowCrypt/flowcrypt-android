/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.adapter.recyclerview.itemdecoration

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

/**
 * @author Denys Bondarenko
 */
class MarginItemDecoration(
  private val marginTop: Int = 0,
  private val marginRight: Int = 0,
  private val marginBottom: Int = 0,
  private val marginLeft: Int = 0
) : RecyclerView.ItemDecoration() {
  override fun getItemOffsets(
    outRect: Rect,
    view: View,
    parent: RecyclerView,
    state: RecyclerView.State
  ) {
    with(outRect) {
      top = marginTop
      right = marginRight
      bottom = marginBottom
      left = marginLeft
    }
  }
}
