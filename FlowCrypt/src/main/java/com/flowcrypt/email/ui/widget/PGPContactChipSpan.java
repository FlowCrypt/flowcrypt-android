/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.widget;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.hootsuite.nachos.chip.ChipSpan;

/**
 * This class describes the representation of {@link ChipSpan} with PGP existing.
 *
 * @author Denis Bondarenko
 * Date: 15.08.2017
 * Time: 16:28
 * E-mail: DenBond7@gmail.com
 */

public class PGPContactChipSpan extends ChipSpan {
  private Boolean isHasPgp;

  public PGPContactChipSpan(@NonNull Context context, @NonNull CharSequence text, @Nullable
      Drawable icon, Object
                                data) {
    super(context, text, icon, data);
  }

  public PGPContactChipSpan(@NonNull Context context, @NonNull PGPContactChipSpan
      pgpContactChipSpan) {
    super(context, pgpContactChipSpan);
    this.isHasPgp = pgpContactChipSpan.isHasPgp();
  }

  public Boolean isHasPgp() {
    return isHasPgp;
  }

  public void setHasPgp(Boolean hasPgp) {
    isHasPgp = hasPgp;
  }
}
