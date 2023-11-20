/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.adapter.selection

import androidx.recyclerview.selection.ItemKeyProvider
import org.pgpainless.key.info.KeyRingInfo

/**
 * @author Denys Bondarenko
 */
class KeyRingInfoItemKeyProvider(private val items: List<KeyRingInfo>) :
  ItemKeyProvider<KeyRingInfo>(SCOPE_CACHED) {
  override fun getKey(position: Int) = items.getOrNull(position)
  override fun getPosition(key: KeyRingInfo) = items.indexOf(key)
}
