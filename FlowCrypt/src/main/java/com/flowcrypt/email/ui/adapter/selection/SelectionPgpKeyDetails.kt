/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.adapter.selection

import androidx.recyclerview.selection.ItemDetailsLookup
import com.flowcrypt.email.security.model.PgpKeyDetails

/**
 * @author Denis Bondarenko
 *         Date: 11/2/20
 *         Time: 5:03 PM
 *         E-mail: DenBond7@gmail.com
 */
class SelectionPgpKeyDetails(
  private val position: Int,
  private val pgpKeyDetails: PgpKeyDetails?
) : ItemDetailsLookup.ItemDetails<PgpKeyDetails>() {
  override fun getSelectionKey() = pgpKeyDetails
  override fun getPosition() = position
}
