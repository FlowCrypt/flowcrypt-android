/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.ui.adapter.recyclerview.itemdecoration

import android.content.Context
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.divider.MaterialDividerItemDecoration

/**
 * @author Denys Bondarenko
 */
class SkipFirstAndLastDividerItemDecoration(context: Context, orientation: Int) :
  MaterialDividerItemDecoration(context, orientation) {
  init {
    isLastItemDecorated = false
  }

  override fun shouldDrawDivider(position: Int, adapter: RecyclerView.Adapter<*>?): Boolean {
    return when {
      position == 0 -> false
      else -> super.shouldDrawDivider(position, adapter)
    }
  }
}