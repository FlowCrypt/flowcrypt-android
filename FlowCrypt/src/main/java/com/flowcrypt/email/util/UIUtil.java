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
     * This method can be used to hide or show progress for some interactions.
     *
     * @param context      Interface to global information about an application environment.
     * @param show         When true we show the progress, when false we show content;
     * @param progressView The progress view;
     * @param contentView  The content view.
     */
    public static void showProgress(Context context, final boolean show,
                                    final View progressView, final View contentView) {
        int shortAnimTime = context.getResources().getInteger(android.R.integer
                .config_shortAnimTime);

        contentView.setVisibility(show ? View.GONE : View.VISIBLE);
        contentView.animate().setDuration(shortAnimTime).alpha(
                show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                contentView.setVisibility(show ? View.GONE : View.VISIBLE);
            }
        });

        progressView.setVisibility(show ? View.VISIBLE : View.GONE);
        progressView.animate().setDuration(shortAnimTime).alpha(
                show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                progressView.setVisibility(show ? View.VISIBLE : View.GONE);
            }
        });
    }

}
