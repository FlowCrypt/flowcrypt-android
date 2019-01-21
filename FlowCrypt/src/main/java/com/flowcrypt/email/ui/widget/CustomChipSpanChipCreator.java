/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.widget;

import android.content.Context;
import android.content.res.ColorStateList;
import android.database.Cursor;

import com.flowcrypt.email.R;
import com.flowcrypt.email.database.dao.source.ContactsDaoSource;
import com.flowcrypt.email.js.PgpContact;
import com.flowcrypt.email.util.UIUtil;
import com.hootsuite.nachos.ChipConfiguration;
import com.hootsuite.nachos.chip.Chip;
import com.hootsuite.nachos.chip.ChipCreator;
import com.hootsuite.nachos.chip.ChipSpan;
import com.hootsuite.nachos.chip.ChipSpanChipCreator;

import androidx.annotation.NonNull;

/**
 * This {@link ChipSpanChipCreator} responsible for displaying {@link Chip}.
 *
 * @author Denis Bondarenko
 * Date: 31.07.2017
 * Time: 13:09
 * E-mail: DenBond7@gmail.com
 */

public class CustomChipSpanChipCreator implements ChipCreator<PGPContactChipSpan> {
  private int backgroundColorPgpExists;
  private int backgroundColorPgpNotExists;
  private int textColorPgpExists;
  private int textColorNoPgpNoExists;

  public CustomChipSpanChipCreator(Context context) {
    backgroundColorPgpExists = UIUtil.getColor(context, R.color.colorPrimary);
    textColorPgpExists = UIUtil.getColor(context, android.R.color.white);
    backgroundColorPgpNotExists = UIUtil.getColor(context, R.color.aluminum);
    textColorNoPgpNoExists = UIUtil.getColor(context, R.color.dark);
  }

  @Override
  public PGPContactChipSpan createChip(@NonNull Context context, @NonNull CharSequence text, Object data) {
    return new PGPContactChipSpan(context, text, null, data);
  }

  @Override
  public PGPContactChipSpan createChip(@NonNull Context context, @NonNull PGPContactChipSpan pgpContactChipSpan) {
    return new PGPContactChipSpan(context, pgpContactChipSpan);
  }

  @Override
  public void configureChip(@NonNull PGPContactChipSpan span, @NonNull ChipConfiguration chipConfiguration) {
    int chipSpacing = chipConfiguration.getChipSpacing();
    if (chipSpacing != -1) {
      span.setLeftMargin(chipSpacing / 2);
      span.setRightMargin(chipSpacing / 2);
    }

    int chipTextColor = chipConfiguration.getChipTextColor();
    if (chipTextColor != -1) {
      span.setTextColor(chipTextColor);
    }

    int chipTextSize = chipConfiguration.getChipTextSize();
    if (chipTextSize != -1) {
      span.setTextSize(chipTextSize);
    }

    int chipHeight = chipConfiguration.getChipHeight();
    if (chipHeight != -1) {
      span.setChipHeight(chipHeight);
    }

    int chipVerticalSpacing = chipConfiguration.getChipVerticalSpacing();
    if (chipVerticalSpacing != -1) {
      span.setChipVerticalSpacing(chipVerticalSpacing);
    }

    int maxAvailableWidth = chipConfiguration.getMaxAvailableWidth();
    if (maxAvailableWidth != -1) {
      span.setMaxAvailableWidth(maxAvailableWidth);
    }

    if (span.hasPgp() != null) {
      updateChipSpanBackground(span, span.hasPgp());
    } else if (span.getData() != null && span.getData() instanceof Cursor) {
      Cursor cursor = (Cursor) span.getData();
      if (cursor != null && !cursor.isClosed()) {
        PgpContact pgpContact = new ContactsDaoSource().getCurrentPgpContact(cursor);
        span.setHasPgp(pgpContact.getHasPgp());
        updateChipSpanBackground(span, pgpContact.getHasPgp());
      }
    } else {
      ColorStateList chipBackground = chipConfiguration.getChipBackground();
      if (chipBackground != null) {
        span.setBackgroundColor(chipBackground);
      }
    }
  }

  /**
   * Update the {@link ChipSpan} background.
   *
   * @param span   The {@link ChipSpan} object.
   * @param hasPgp true if the contact has pgp key, otherwise false.
   */
  private void updateChipSpanBackground(@NonNull PGPContactChipSpan span, boolean hasPgp) {
    if (hasPgp) {
      span.setBackgroundColor(ColorStateList.valueOf(backgroundColorPgpExists));
      span.setTextColor(textColorPgpExists);
    } else {
      span.setBackgroundColor(ColorStateList.valueOf(backgroundColorPgpNotExists));
      span.setTextColor(textColorNoPgpNoExists);
    }
  }
}

