/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.adapter.selection

import androidx.recyclerview.selection.ItemKeyProvider
import com.flowcrypt.email.security.model.PgpKeyDetails

/**
 * @author Denys Bondarenko
 */
class PgpKeyDetailsKeyProvider(private val items: List<PgpKeyDetails>) :
  ItemKeyProvider<PgpKeyDetails>(SCOPE_CACHED) {
  override fun getKey(position: Int) = items.getOrNull(position)
  override fun getPosition(key: PgpKeyDetails) = items.indexOf(key)
}
