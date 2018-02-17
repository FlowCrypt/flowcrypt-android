/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.widget;

import android.content.Context;
import android.support.annotation.NonNull;

import com.hootsuite.nachos.NachoTextView;
import com.hootsuite.nachos.chip.ChipCreator;
import com.hootsuite.nachos.tokenizer.SpanChipTokenizer;

/**
 * Define a custom chip separator in {@link NachoTextView}
 *
 * @author DenBond7
 *         Date: 19.05.2017
 *         Time: 14:14
 *         E-mail: DenBond7@gmail.com
 */

public class SingleCharacterSpanChipTokenizer extends SpanChipTokenizer {
    public static final char CHIP_SEPARATOR_WHITESPACE = ' ';
    private final char symbol;

    public SingleCharacterSpanChipTokenizer(Context context, @NonNull ChipCreator chipCreator,
                                            @NonNull Class chipClass, char symbol) {
        super(context, chipCreator, chipClass);
        this.symbol = symbol;
    }

    @Override
    public int findTokenStart(CharSequence text, int cursor) {
        int i = cursor;

        while (i > 0 && text.charAt(i - 1) != symbol) {
            i--;
        }
        while (i < cursor && text.charAt(i) == symbol) {
            i++;
        }

        return i;
    }

    @Override
    public int findTokenEnd(CharSequence text, int cursor) {
        int i = cursor;
        int len = text.length();

        while (i < len) {
            if (text.charAt(i) == symbol) {
                return i;
            } else {
                i++;
            }
        }

        return len;
    }
}
