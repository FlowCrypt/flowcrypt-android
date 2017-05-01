package com.flowcrypt.email.util;

import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.view.View;

/**
 * User interface util methods.
 *
 * @author DenBond7
 *         Date: 26.04.2017
 *         Time: 14:40
 *         E-mail: DenBond7@gmail.com
 */

public class UIUtil {
    /**
     * Show some information as Snackbar.
     *
     * @param view        he view to find a parent from.
     * @param messageText The text to show.  Can be formatted text..
     */
    public static void showInfoSnackbar(View view, String messageText) {
        Snackbar.make(view, messageText, Snackbar.LENGTH_INDEFINITE)
                .setAction(android.R.string.ok, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                    }
                }).show();
    }

    /**
     * Show some information as Snackbar with custom message, action button mame and listener. .
     *
     * @param view            he view to find a parent from.
     * @param messageText     The text to show.  Can be formatted text..
     * @param buttonName      The text of the Snackbar button;
     * @param onClickListener The Snackbar button click listener.
     */
    public static void showSnackbar(View view, String messageText, String buttonName,
                                    @NonNull View.OnClickListener onClickListener) {
        Snackbar.make(view, messageText, Snackbar.LENGTH_INDEFINITE)
                .setAction(buttonName, onClickListener).show();
    }
}
