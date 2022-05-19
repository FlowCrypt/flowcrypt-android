/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.adapter.selection

import androidx.recyclerview.selection.ItemKeyProvider
import androidx.recyclerview.widget.RecyclerView
import com.flowcrypt.email.ui.adapter.MsgsPagedListAdapter

/**
 * @author Denis Bondarenko
 *         Date: 1/8/20
 *         Time: 4:44 PM
 *         E-mail: DenBond7@gmail.com
 */
class CustomStableIdKeyProvider(private val adapter: MsgsPagedListAdapter) :
  ItemKeyProvider<Long>(SCOPE_CACHED) {

  override fun getKey(position: Int): Long? {
    return adapter.getMessageEntity(position)?.id
  }

  override fun getPosition(key: Long): Int {
    return adapter.getPositionByIds(key) ?: RecyclerView.NO_POSITION
  }
}
