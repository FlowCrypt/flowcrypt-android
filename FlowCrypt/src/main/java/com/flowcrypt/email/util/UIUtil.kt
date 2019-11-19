/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.util

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Build
import android.text.Html
import android.text.Spanned
import android.text.TextUtils
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import com.google.android.material.snackbar.Snackbar
import java.io.ByteArrayOutputStream


/**
 * User interface util methods.
 *
 * @author DenBond7
 * Date: 26.04.2017
 * Time: 14:40
 * E-mail: DenBond7@gmail.com
 */

class UIUtil {
  companion object {
    /**
     * Show some information as Snackbar.
     *
     * @param view    The view to find a parent from.
     * @param msgText The text to show.  Can be formatted text..
     */
    @JvmStatic
    fun showInfoSnackbar(view: View, msgText: String): Snackbar {
      val snackbar = Snackbar.make(view, msgText, Snackbar.LENGTH_INDEFINITE)
          .setAction(android.R.string.ok) { }
      snackbar.show()

      return snackbar
    }

    /**
     * Show some information as Snackbar with custom message, action button mame and listener. .
     *
     * @param view            The view to find a parent from.
     * @param msgText         The text to show.  Can be formatted text..
     * @param buttonName      The text of the Snackbar button;
     * @param onClickListener The Snackbar button click listener.
     * @param duration        How long to display the message. Either [Snackbar.LENGTH_SHORT] or [Snackbar.LENGTH_LONG]
     */
    @JvmStatic
    @JvmOverloads
    fun showSnackbar(view: View, msgText: String, buttonName: String,
                     onClickListener: View.OnClickListener, duration: Int = Snackbar.LENGTH_INDEFINITE): Snackbar {
      val snackbar = Snackbar.make(view, msgText, duration).setAction(buttonName, onClickListener)
      snackbar.show()

      return snackbar
    }

    /**
     * Request to hide the soft input window from the
     * context of the window that is currently accepting input.
     *
     * @param context Interface to global information about an application environment.
     * @param view
     */
    @JvmStatic
    fun hideSoftInput(context: Context, view: View?) {
      val inputMethodManager = context.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
      if (view != null) {
        inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
      }
    }

    /**
     * This method can be used to exchange views visibility for some interactions.
     *
     * @param context Interface to global information about an application environment.
     * @param show    When true we show the firstView, when false we show the secondView;
     * @param first   The first view;
     * @param second  The second view.
     * @param useFastMode  if true we will change views visibility immediately.
     */
    @JvmStatic
    fun exchangeViewVisibility(context: Context?, show: Boolean, first: View?, second: View?,
                               useFastMode: Boolean = false) {

      if (useFastMode) {
        first?.visibility = if (show) View.VISIBLE else View.GONE
        second?.visibility = if (show) View.GONE else View.VISIBLE
        return
      }

      if (context == null) {
        return
      }

      if (show && first?.visibility == View.VISIBLE && second?.visibility == View.GONE) {
        return
      }

      if (!show && second?.visibility == View.VISIBLE && first?.visibility == View.GONE) {
        return
      }

      val shortAnimTime = context.resources.getInteger(android.R.integer.config_shortAnimTime)

      second?.visibility = if (show) View.GONE else View.VISIBLE
      second?.animate()?.setDuration(shortAnimTime.toLong())?.alpha((if (show) 0 else 1).toFloat())
          ?.setListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
              second.visibility = if (show) View.GONE else View.VISIBLE
            }
          })

      first?.visibility = if (show) View.VISIBLE else View.GONE
      first?.animate()?.setDuration(shortAnimTime.toLong())?.alpha((if (show) 1 else 0).toFloat())
          ?.setListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
              first.visibility = if (show) View.VISIBLE else View.GONE
            }
          })
    }

    /**
     * Set a HTML text to some TextView.
     *
     * @param text     The text which will be set to the current textView.
     * @param textView The textView where we will set the HTML text.
     */
    @JvmStatic
    @Suppress("DEPRECATION")
    fun setHtmlTextToTextView(text: String, textView: TextView?) {
      if (textView != null && !TextUtils.isEmpty(text)) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
          textView.text = Html.fromHtml(text, Html.FROM_HTML_MODE_LEGACY)
        } else {
          textView.text = Html.fromHtml(text)
        }
      }
    }

    /**
     * Get the HTML [Spanned] from a text.
     *
     * @param text The text which contains HTML.
     */
    @JvmStatic
    @Suppress("DEPRECATION")
    fun getHtmlSpannedFromText(text: String): CharSequence {
      return if (!TextUtils.isEmpty(text)) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
          Html.fromHtml(text, Html.FROM_HTML_MODE_LEGACY)
        } else {
          Html.fromHtml(text)
        }
      } else
        text
    }

    /**
     * Get a color value using the context.
     *
     * @param context          Interface to global information about an application environment.
     * @param colorResourcesId The resources id of the needed color.
     * @return The int value of the color.
     */
    @JvmStatic
    fun getColor(context: Context, colorResourcesId: Int): Int {
      return context.resources.getColor(colorResourcesId, context.theme)
    }

    /**
     * Get a screenshot of the given activity.
     * @return an array of bytes of a compressed screenshot.
     */
    fun getScreenShotByteArray(activity: Activity): ByteArray? {
      val view = activity.window.decorView ?: return null
      val width = view.width
      val height = view.height
      val bitmap = Bitmap.createBitmap(if (width > 0) width else 640,
          if (height > 0) height else 480, Bitmap.Config.RGB_565)
      val canvas = Canvas(bitmap)
      view.draw(canvas)
      return getCompressedByteArrayOfBitmap(bitmap, 50)
    }

    /**
     * Get a compressed byte array from the given bitmap using the given quality value.
     * @return an array of bytes of a compressed bitmap.
     */
    fun getCompressedByteArrayOfBitmap(bitmap: Bitmap?, quality: Int): ByteArray? {
      val stream = ByteArrayOutputStream()
      bitmap?.compress(Bitmap.CompressFormat.PNG, quality, stream)
      return stream.toByteArray()
    }
  }
}
