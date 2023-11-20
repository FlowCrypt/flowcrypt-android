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
class SelectionPgpKeyDetails(
  private val position: Int,
  private val pgpKeyRingDetails: KeyRingInfo?
) : ItemDetailsLookup.ItemDetails<KeyRingInfo>() {
  override fun getSelectionKey() = pgpKeyRingDetails
  override fun getPosition() = position
}
