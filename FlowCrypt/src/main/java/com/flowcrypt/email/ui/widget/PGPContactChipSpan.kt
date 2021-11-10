/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.widget

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable

import com.hootsuite.nachos.chip.ChipSpan

/**
 * This class describes the representation of [ChipSpan] with PGP existing.
 *
 * @author Denis Bondarenko
 * Date: 15.08.2017
 * Time: 16:28
 * E-mail: DenBond7@gmail.com
 */
class PGPContactChipSpan : ChipSpan {
  var hasAtLeastOnePubKey: Boolean? = false
  var hasNotExpiredPubKey: Boolean? = false
  var hasUsablePubKey: Boolean? = false
  var hasNotRevokedPubKey: Boolean? = false

  /**
   * The last modified value that saved after [setBackgroundColor]. Can be null
   */
  var chipBackgroundColor: ColorStateList? = null

  constructor(context: Context, text: CharSequence, icon: Drawable?, data: Any?) : super(
    context,
    text,
    icon,
    data
  )

  constructor(context: Context, pgpContactChipSpan: PGPContactChipSpan) : super(
    context,
    pgpContactChipSpan
  ) {
    this.hasAtLeastOnePubKey = pgpContactChipSpan.hasAtLeastOnePubKey
    this.hasNotExpiredPubKey = pgpContactChipSpan.hasNotExpiredPubKey
    this.hasUsablePubKey = pgpContactChipSpan.hasUsablePubKey
    this.hasNotRevokedPubKey = pgpContactChipSpan.hasNotRevokedPubKey
  }

  override fun setBackgroundColor(backgroundColor: ColorStateList?) {
    super.setBackgroundColor(backgroundColor)
    chipBackgroundColor = backgroundColor
  }
}
