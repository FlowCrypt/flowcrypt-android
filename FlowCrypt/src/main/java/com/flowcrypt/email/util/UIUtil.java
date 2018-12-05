/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.util;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.text.Html;
import android.text.Spanned;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import com.google.android.material.snackbar.Snackbar;

import androidx.annotation.NonNull;

/**
 * User interface util methods.
 *
 * @author DenBond7
 * Date: 26.04.2017
 * Time: 14:40
 * E-mail: DenBond7@gmail.com
 */

public class UIUtil {
  /**
   * Show some information as Snackbar.
   *
   * @param view    The view to find a parent from.
   * @param msgText The text to show.  Can be formatted text..
   */
  public static Snackbar showInfoSnackbar(View view, String msgText) {
    Snackbar snackbar = Snackbar.make(view, msgText, Snackbar.LENGTH_INDEFINITE)
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
   * @param view            The view to find a parent from.
   * @param msgText         The text to show.  Can be formatted text..
   * @param buttonName      The text of the Snackbar button;
   * @param onClickListener The Snackbar button click listener.
   */
  public static Snackbar showSnackbar(View view, String msgText, String buttonName,
                                      @NonNull View.OnClickListener onClickListener) {
    return showSnackbar(view, msgText, buttonName, onClickListener, Snackbar.LENGTH_INDEFINITE);
  }

  /**
   * Show some information as Snackbar with custom message, action button mame and listener. .
   *
   * @param view            The view to find a parent from.
   * @param msgText         The text to show.  Can be formatted text..
   * @param buttonName      The text of the Snackbar button;
   * @param onClickListener The Snackbar button click listener.
   * @param duration        How long to display the message.  Either {@link Snackbar#LENGTH_SHORT} or {@link
   *                        Snackbar#LENGTH_LONG}
   */
  public static Snackbar showSnackbar(View view, String msgText, String buttonName,
                                      @NonNull View.OnClickListener onClickListener, int duration) {
    Snackbar snackbar = Snackbar.make(view, msgText, duration).setAction(buttonName, onClickListener);
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
   * @param context Interface to global information about an application environment.
   * @param show    When true we show the firstView, when false we show the secondView;
   * @param first   The first view;
   * @param second  The second view.
   */
  public static void exchangeViewVisibility(Context context, final boolean show, final View first, final View second) {
    if (context == null) {
      return;
    }

    if (show && first.getVisibility() == View.VISIBLE && second.getVisibility() == View.GONE) {
      return;
    }

    if (!show && second.getVisibility() == View.VISIBLE && first.getVisibility() == View.GONE) {
      return;
    }

    int shortAnimTime = context.getResources().getInteger(android.R.integer.config_shortAnimTime);

    second.setVisibility(show ? View.GONE : View.VISIBLE);
    second.animate().setDuration(shortAnimTime).alpha(show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
      @Override
      public void onAnimationEnd(Animator animation) {
        second.setVisibility(show ? View.GONE : View.VISIBLE);
      }
    });

    first.setVisibility(show ? View.VISIBLE : View.GONE);
    first.animate().setDuration(shortAnimTime).alpha(show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
      @Override
      public void onAnimationEnd(Animator animation) {
        first.setVisibility(show ? View.VISIBLE : View.GONE);
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
   * Get the HTML {@link Spanned} from a text.
   *
   * @param text The text which contains HTML.
   */
  @SuppressWarnings("deprecation")
  public static CharSequence getHtmlSpannedFromText(String text) {
    if (!TextUtils.isEmpty(text)) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        return Html.fromHtml(text, Html.FROM_HTML_MODE_LEGACY);
      } else {
        return Html.fromHtml(text);
      }
    } else return text;
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
      return context.getResources().getColor(colorResourcesId, context.getTheme());
    } else {
      return context.getResources().getColor(colorResourcesId);
    }
  }
}
