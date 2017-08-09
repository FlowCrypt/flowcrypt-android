/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (tom@cryptup.org).
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

public class CustomChipSpanChipCreator extends ChipSpanChipCreator {
    private Context context;

    public CustomChipSpanChipCreator(Context context) {
        this.context = context;
    }

    /**
     * Update the {@link ChipSpan} background.
     *
     * @param context  Interface to global information about an application environment.
     * @param chipSpan The {@link ChipSpan} object.
     * @param isHasPgp true if the contact has pgp key, otherwise false.
     */
    public static void updateChipSpanBackground(Context context, @NonNull ChipSpan chipSpan,
                                                boolean isHasPgp) {
        int color;
        if (isHasPgp) {
            color = UIUtil.getColor(context, R.color.colorPrimary);
        } else {
            color = UIUtil.getColor(context, R.color.gray);
        }
        chipSpan.setBackgroundColor(ColorStateList.valueOf(color));
    }

    @Override
    public ChipSpan createChip(@NonNull Context context, @NonNull CharSequence text, Object data) {
        return super.createChip(context, text, data);
    }

    @Override
    public void configureChip(@NonNull ChipSpan chip,
                              @NonNull ChipConfiguration chipConfiguration) {
        super.configureChip(chip, chipConfiguration);
        if (chip.getData() != null && chip.getData() instanceof Cursor) {
            Cursor cursor = (Cursor) chip.getData();
            if (cursor != null && !cursor.isClosed()) {
                PgpContact pgpContact = new ContactsDaoSource().getCurrentPgpContact(cursor);

                updateChipSpanBackground(context, chip, pgpContact.getHasPgp());
            }
        }
    }
}
