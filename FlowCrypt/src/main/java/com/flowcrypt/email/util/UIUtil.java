/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (human@flowcrypt.com).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.util;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.text.Html;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

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
    public static Snackbar showInfoSnackbar(View view, String messageText) {
        Snackbar snackbar = Snackbar.make(view, messageText, Snackbar.LENGTH_INDEFINITE)
                .setAction(android.R.string.ok, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                    }
                });
        snackbar.show();

        return snackbar;
    }

    /**
     * Show some information as Snackbar with custom message, action button mame and listener. .
     *
     * @param view            he view to find a parent from.
     * @param messageText     The text to show.  Can be formatted text..
     * @param buttonName      The text of the Snackbar button;
     * @param onClickListener The Snackbar button click listener.
     */
    public static Snackbar showSnackbar(View view, String messageText, String buttonName,
                                        @NonNull View.OnClickListener onClickListener) {
        Snackbar snackbar = Snackbar.make(view, messageText, Snackbar.LENGTH_INDEFINITE)
                .setAction(buttonName, onClickListener);
        snackbar.show();

        return snackbar;
    }

    /**
     * Request to hide the soft input window from the
     * context of the window that is currently accepting input.
     *
     * @param context Interface to global information about an application environment.
     * @param view
     */
    public static void hideSoftInput(Context context, View view) {
        InputMethodManager inputMethodManager =
                (InputMethodManager) context.getSystemService(Activity.INPUT_METHOD_SERVICE);
        if (view != null) {
            inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    /**
     * This method can be used to exchange views visibility for some interactions.
     *
     * @param context    Interface to global information about an application environment.
     * @param show       When true we show the firstView, when false we show the secondView;
     * @param firstView  The first view;
     * @param secondView The second view.
     */
    public static void exchangeViewVisibility(Context context, final boolean show,
                                              final View firstView, final View secondView) {
        int shortAnimTime = context.getResources().getInteger(android.R.integer
                .config_shortAnimTime);

        if (show && firstView.getVisibility() == View.VISIBLE
                && secondView.getVisibility() == View.GONE) {
            return;
        }

        if (!show && secondView.getVisibility() == View.VISIBLE
                && firstView.getVisibility() == View.GONE) {
            return;
        }

        secondView.setVisibility(show ? View.GONE : View.VISIBLE);
        secondView.animate().setDuration(shortAnimTime).alpha(
                show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                secondView.setVisibility(show ? View.GONE : View.VISIBLE);
            }
        });

        firstView.setVisibility(show ? View.VISIBLE : View.GONE);
        firstView.animate().setDuration(shortAnimTime).alpha(
                show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                firstView.setVisibility(show ? View.VISIBLE : View.GONE);
            }
        });
    }

    /**
     * Set a HTML text to some TextView.
     *
     * @param text     The text which will be set to the current textView.
     * @param textView The textView where we will set the HTML text.
     */
    @SuppressWarnings("deprecation")
    public static void setHtmlTextToTextView(String text, TextView textView) {
        if (textView != null && !TextUtils.isEmpty(text)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                textView.setText(Html.fromHtml(text, Html.FROM_HTML_MODE_LEGACY));
            } else {
                textView.setText(Html.fromHtml(text));
            }
        }
    }

    /**
     * Get a color value using the context.
     *
     * @param context          Interface to global information about an application environment.
     * @param colorResourcesId The resources id of the needed color.
     * @return The int value of the color.
     */
    @SuppressWarnings("deprecation")
    public static int getColor(Context context, int colorResourcesId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return context.getResources().getColor
                    (colorResourcesId, context.getTheme());
        } else {
            return context.getResources().getColor(colorResourcesId);
        }
    }
}
