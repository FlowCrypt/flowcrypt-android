/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.adapter.selection

import androidx.recyclerview.selection.ItemKeyProvider
import com.flowcrypt.email.security.model.PgpKeyRingDetails

/**
 * @author Denys Bondarenko
 */
class PgpKeyDetailsKeyProvider(private val items: List<PgpKeyRingDetails>) :
  ItemKeyProvider<PgpKeyRingDetails>(SCOPE_CACHED) {
  override fun getKey(position: Int) = items.getOrNull(position)
  override fun getPosition(key: PgpKeyRingDetails) = items.indexOf(key)
}
