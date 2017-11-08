/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (human@flowcrypt.com).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.widget;

import android.content.Context;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.support.annotation.NonNull;

import com.flowcrypt.email.R;
import com.flowcrypt.email.database.dao.source.ContactsDaoSource;
import com.flowcrypt.email.js.PgpContact;
import com.flowcrypt.email.util.UIUtil;
import com.hootsuite.nachos.ChipConfiguration;
import com.hootsuite.nachos.chip.Chip;
import com.hootsuite.nachos.chip.ChipCreator;
import com.hootsuite.nachos.chip.ChipSpan;
import com.hootsuite.nachos.chip.ChipSpanChipCreator;

/**
 * This {@link ChipSpanChipCreator} responsible for displaying {@link Chip}.
 *
 * @author Denis Bondarenko
 *         Date: 31.07.2017
 *         Time: 13:09
 *         E-mail: DenBond7@gmail.com
 */

public class CustomChipSpanChipCreator implements ChipCreator<PGPContactChipSpan> {
    private int backgroundColorPgpExists;
    private int backgroundColorPgpNoExists;
    private int textColorPgpExists;
    private int textColorNoPgpNoExists;

    public CustomChipSpanChipCreator(Context context) {
        backgroundColorPgpExists = UIUtil.getColor(context, R.color.colorPrimary);
        textColorPgpExists = UIUtil.getColor(context, android.R.color.white);
        backgroundColorPgpNoExists = UIUtil.getColor(context, R.color.aluminum);
        textColorNoPgpNoExists = UIUtil.getColor(context, R.color.dark);
    }

    @Override
    public PGPContactChipSpan createChip(@NonNull Context context, @NonNull CharSequence text,
                                         Object data) {
        return new PGPContactChipSpan(context, text, null, data);
    }

    @Override
    public PGPContactChipSpan createChip(@NonNull Context context, @NonNull PGPContactChipSpan
            PGPContactChipSpan) {
        return new PGPContactChipSpan(context, PGPContactChipSpan);
    }

    @Override
    public void configureChip(@NonNull PGPContactChipSpan pgpContactChipSpan,
                              @NonNull ChipConfiguration chipConfiguration) {
        int chipSpacing = chipConfiguration.getChipSpacing();
        int chipTextColor = chipConfiguration.getChipTextColor();
        int chipTextSize = chipConfiguration.getChipTextSize();
        int chipHeight = chipConfiguration.getChipHeight();
        int chipVerticalSpacing = chipConfiguration.getChipVerticalSpacing();
        int maxAvailableWidth = chipConfiguration.getMaxAvailableWidth();

        if (chipSpacing != -1) {
            pgpContactChipSpan.setLeftMargin(chipSpacing / 2);
            pgpContactChipSpan.setRightMargin(chipSpacing / 2);
        }

        if (chipTextColor != -1) {
            pgpContactChipSpan.setTextColor(chipTextColor);
        }

        if (chipTextSize != -1) {
            pgpContactChipSpan.setTextSize(chipTextSize);
        }

        if (chipHeight != -1) {
            pgpContactChipSpan.setChipHeight(chipHeight);
        }

        if (chipVerticalSpacing != -1) {
            pgpContactChipSpan.setChipVerticalSpacing(chipVerticalSpacing);
        }

        if (maxAvailableWidth != -1) {
            pgpContactChipSpan.setMaxAvailableWidth(maxAvailableWidth);
        }

        if (pgpContactChipSpan.isHasPgp() != null) {
            updateChipSpanBackground(pgpContactChipSpan, pgpContactChipSpan.isHasPgp());
        } else if (pgpContactChipSpan.getData() != null
                && pgpContactChipSpan.getData() instanceof Cursor) {
            Cursor cursor = (Cursor) pgpContactChipSpan.getData();
            if (cursor != null && !cursor.isClosed()) {
                PgpContact pgpContact = new ContactsDaoSource().getCurrentPgpContact(cursor);
                pgpContactChipSpan.setHasPgp(pgpContact.getHasPgp());
                updateChipSpanBackground(pgpContactChipSpan, pgpContact.getHasPgp());
            }
        } else {
            ColorStateList chipBackground = chipConfiguration.getChipBackground();
            if (chipBackground != null) {
                pgpContactChipSpan.setBackgroundColor(chipBackground);
            }
        }
    }

    /**
     * Update the {@link ChipSpan} background.
     *
     * @param pgpContactChipSpan The {@link ChipSpan} object.
     * @param isHasPgp           true if the contact has pgp key, otherwise false.
     */
    private void updateChipSpanBackground(@NonNull PGPContactChipSpan pgpContactChipSpan, boolean
            isHasPgp) {
        if (isHasPgp) {
            pgpContactChipSpan.setBackgroundColor(ColorStateList.valueOf(backgroundColorPgpExists));
            pgpContactChipSpan.setTextColor(textColorPgpExists);
        } else {
            pgpContactChipSpan.setBackgroundColor(ColorStateList.valueOf
                    (backgroundColorPgpNoExists));
            pgpContactChipSpan.setTextColor(textColorNoPgpNoExists);
        }
    }
}

