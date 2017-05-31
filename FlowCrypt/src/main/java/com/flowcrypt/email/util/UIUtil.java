package com.flowcrypt.email.util;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

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

}
