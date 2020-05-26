/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.widget

import android.content.Context
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
  private var hasPgp: Boolean? = null

  constructor(context: Context, text: CharSequence, icon: Drawable?, data: Any?) : super(context, text, icon, data)

  constructor(context: Context, pgpContactChipSpan: PGPContactChipSpan) : super(context, pgpContactChipSpan) {
    this.hasPgp = pgpContactChipSpan.hasPgp()
  }

  fun hasPgp(): Boolean? {
    return hasPgp
  }

  fun setHasPgp(hasPgp: Boolean?) {
    this.hasPgp = hasPgp
  }
}
