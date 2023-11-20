/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.adapter.selection

import androidx.recyclerview.selection.ItemDetailsLookup
import org.pgpainless.key.info.KeyRingInfo

/**
 * @author Denys Bondarenko
 */
class KeyRingInfoItemDetails(private val position: Int, private val keyRingInfo: KeyRingInfo?) :
  ItemDetailsLookup.ItemDetails<KeyRingInfo>() {
  override fun getSelectionKey() = keyRingInfo
  override fun getPosition() = position
}
