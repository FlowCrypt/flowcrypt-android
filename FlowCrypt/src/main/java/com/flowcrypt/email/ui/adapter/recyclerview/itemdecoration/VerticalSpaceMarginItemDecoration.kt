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
class VerticalSpaceMarginItemDecoration(
  private val marginTop: Int,
  private val marginBottom: Int,
  private val marginInternal: Int
) : RecyclerView.ItemDecoration() {
  override fun getItemOffsets(
    outRect: Rect, view: View,
    parent: RecyclerView, state: RecyclerView.State
  ) {
    val position = parent.getChildLayoutPosition(view)
    val adapter = parent.adapter

    if (adapter != null && position == adapter.itemCount - 1) {
      outRect.bottom = marginBottom
    }

    if (position == 0) {
      outRect.top = marginTop
    } else {
      outRect.top = marginInternal
    }
  }
}
