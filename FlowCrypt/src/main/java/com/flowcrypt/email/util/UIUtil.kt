/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.util

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.text.Html
import android.text.Spanned
import android.text.TextUtils
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.core.graphics.createBitmap
import java.io.ByteArrayOutputStream

/**
 * User interface util methods.
 *
 * @author Denys Bondarenko
 */
class UIUtil {
  companion object {
    /**
     * Request to hide the soft input window from the
     * context of the window that is currently accepting input.
     *
     * @param context Interface to global information about an application environment.
     * @param view
     */
    fun hideSoftInput(context: Context?, view: View?) {
      val inputMethodManager = context?.getSystemService(Activity.INPUT_METHOD_SERVICE) as?
          InputMethodManager
      if (view != null) {
        inputMethodManager?.hideSoftInputFromWindow(view.windowToken, 0)
      }
    }

    /**
     * This method can be used to exchange views visibility for some interactions.
     *
     * @param show    When true we show the firstView, when false we show the secondView;
     * @param first   The first view;
     * @param second  The second view.
     */
    fun exchangeViewVisibility(show: Boolean, first: View?, second: View?) {
      first?.visibility = if (show) View.VISIBLE else View.GONE
      second?.visibility = if (show) View.GONE else View.VISIBLE
    }

    /**
     * Set a HTML text to some TextView.
     *
     * @param text     The text which will be set to the current textView.
     * @param textView The textView where we will set the HTML text.
     */
    fun setHtmlTextToTextView(text: String?, textView: TextView?) {
      if (textView != null && !TextUtils.isEmpty(text)) {
        textView.text = Html.fromHtml(text, Html.FROM_HTML_MODE_LEGACY)
      }
    }

    /**
     * Get the HTML [Spanned] from a text.
     *
     * @param text The text which contains HTML.
     */
    fun getHtmlSpannedFromText(text: String?): CharSequence? {
      return if (text?.isNotEmpty() == true) {
        Html.fromHtml(text, Html.FROM_HTML_MODE_LEGACY)
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
    fun getColor(context: Context, colorResourcesId: Int): Int {
      return context.resources.getColor(colorResourcesId, context.theme)
    }

    /**
     * Get a screenshot of the given activity.
     * @return an array of bytes of a compressed screenshot.
     */
    fun getScreenShotByteArray(activity: Activity): ByteArray? {
      val view = activity.window.decorView
      val width = view.width
      val height = view.height
      val bitmap = createBitmap(
        if (width > 0) width else 640,
        if (height > 0) height else 480,
        Bitmap.Config.RGB_565
      )
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
