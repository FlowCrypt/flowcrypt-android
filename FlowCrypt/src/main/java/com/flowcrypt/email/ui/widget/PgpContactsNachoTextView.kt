/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.widget

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.text.Spannable
import android.text.SpannableString
import android.text.Spanned
import android.text.style.SuggestionSpan
import android.util.AttributeSet
import android.view.ActionMode
import android.view.GestureDetector
import android.view.HapticFeedbackConstants
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.widget.AdapterView
import com.flowcrypt.email.util.exception.ExceptionUtil
import com.hootsuite.nachos.NachoTextView
import com.hootsuite.nachos.chip.Chip
import java.util.*

/**
 * The custom realization of [NachoTextView].
 *
 * @author DenBond7
 * Date: 19.05.2017
 * Time: 8:52
 * E-mail: DenBond7@gmail.com
 */
class PgpContactsNachoTextView(context: Context, attrs: AttributeSet) :
  NachoTextView(context, attrs) {
  private val gestureDetector: GestureDetector
  private var listener: OnChipLongClickListener? = null
  private val gestureListener: ChipLongClickOnGestureListener

  init {
    this.gestureListener = ChipLongClickOnGestureListener()
    this.gestureDetector = GestureDetector(getContext(), gestureListener)
    customSelectionActionModeCallback = CustomActionModeCallback()
  }

  /**
   * This method prevents add a duplicate email from the dropdown to TextView.
   */
  override fun onItemClick(adapterView: AdapterView<*>?, view: View?, position: Int, id: Long) {
    val text = this.filter.convertResultToString(this.adapter.getItem(position))

    if (!getText().toString().contains(text)) {
      super.onItemClick(adapterView, view, position, id)
    }

  }

  override fun toString(): String {
    //Todo In this code I received a crash. Need to fix it.
    try {
      return super.toString()
    } catch (e: Exception) {
      e.printStackTrace()
      ExceptionUtil.handleError(e)
    }

    return text.toString()
  }

  @SuppressLint("ClickableViewAccessibility")
  override fun onTouchEvent(event: MotionEvent): Boolean {
    gestureDetector.onTouchEvent(event)
    return super.onTouchEvent(event)
  }

  override fun onTextContextMenuItem(id: Int): Boolean {
    val start = selectionStart
    val end = selectionEnd

    when (id) {
      android.R.id.cut -> {
        setClipboardData(
          ClipData.newPlainText(
            null,
            removeSuggestionSpans(getTextWithPlainTextSpans(start, end))
          )
        )
        text.delete(selectionStart, selectionEnd)
        return true
      }

      android.R.id.copy -> {
        setClipboardData(
          ClipData.newPlainText(
            null,
            removeSuggestionSpans(getTextWithPlainTextSpans(start, end))
          )
        )
        return true
      }

      android.R.id.paste -> {
        val stringBuilder = StringBuilder()
        val clipboardManager =
          context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = clipboardManager.primaryClip
        if (clip != null) {
          for (i in 0 until clip.itemCount) {
            stringBuilder.append(clip.getItemAt(i).coerceToStyledText(context))
          }
        }

        val emails = chipValues
        if (emails.contains(stringBuilder.toString())) {
          clipboardManager.setPrimaryClip(ClipData.newPlainText(null, " "))
        }

        return super.onTextContextMenuItem(id)
      }

      else -> return super.onTextContextMenuItem(id)
    }
  }

  fun setListener(listener: OnChipLongClickListener) {
    this.listener = listener
  }

  private fun setClipboardData(clip: ClipData) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(clip)
  }

  /**
   * Get a formatted text of a selection.
   *
   * @param start The begin position of the selected text.
   * @param end   The end position of the selected text.
   * @return A formatted text.
   */
  private fun getTextWithPlainTextSpans(start: Int, end: Int): CharSequence {
    val editable = text

    if (chipTokenizer != null) {
      val stringBuilder = StringBuilder()

      val chips = listOf(*chipTokenizer!!.findAllChips(start, end, editable))
      for (i in chips.indices) {
        val chip = chips[i]
        stringBuilder.append(chip.text)
        if (i != chips.size - 1) {
          stringBuilder.append(CHIP_SEPARATOR_WHITESPACE)
        }
      }

      return stringBuilder.toString()
    }
    return editable.subSequence(start, end).toString()
  }

  private fun removeSuggestionSpans(text: CharSequence): CharSequence {
    var tempText = text
    if (tempText is Spanned) {
      val spannable: Spannable
      if (tempText is Spannable) {
        spannable = tempText
      } else {
        spannable = SpannableString(tempText)
        tempText = spannable
      }

      val spans = spannable.getSpans(0, tempText.length, SuggestionSpan::class.java)
      for (span in spans) {
        spannable.removeSpan(span)
      }
    }
    return tempText
  }

  interface OnChipLongClickListener {
    /**
     * Called when a chip in this TextView is long clicked.
     *
     * @param nachoTextView A current view
     * @param chip          the [Chip] that was clicked
     * @param event         the [MotionEvent] that caused the touch
     */
    fun onChipLongClick(nachoTextView: NachoTextView, chip: Chip, event: MotionEvent)
  }

  /**
   * A custom realization of [ActionMode.Callback] which describes a logic of the text manipulation.
   */
  private inner class CustomActionModeCallback : ActionMode.Callback {

    override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
      return true
    }

    override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
      var isMenuModified = false

      val items = mutableListOf<MenuItem>()
      for (i in 0 until menu.size()) {
        items.add(menu.getItem(i))
      }

      for (item in items) {
        when (item.itemId) {
          android.R.id.cut, android.R.id.copy -> {
          }

          else -> {
            menu.removeItem(item.itemId)
            isMenuModified = true
          }
        }
      }

      return isMenuModified
    }

    override fun onActionItemClicked(mode: ActionMode, menuItem: MenuItem): Boolean {
      when (menuItem.itemId) {
        android.R.id.copy -> {
          onTextContextMenuItem(android.R.id.copy)
          mode.finish()
          return true
        }
      }
      return false
    }

    override fun onDestroyActionMode(mode: ActionMode) {

    }
  }

  private inner class ChipLongClickOnGestureListener : GestureDetector.SimpleOnGestureListener() {
    override fun onLongPress(event: MotionEvent) {
      super.onLongPress(event)
      performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)

      if (listener != null) {
        val chip = findLongClickedChip(event)

        if (chip != null) {
          listener!!.onChipLongClick(this@PgpContactsNachoTextView, chip, event)
        }
      }
    }

    private fun findLongClickedChip(event: MotionEvent): Chip? {
      if (chipTokenizer == null) {
        return null
      }

      val text = text
      val offset = getOffsetForPosition(event.x, event.y)
      val chips = allChips
      for (chip in chips) {
        val chipStart = chipTokenizer!!.findChipStart(chip, text)
        val chipEnd = chipTokenizer!!.findChipEnd(chip, text)
        if (offset in chipStart..chipEnd) {
          val eventX = event.x
          val startX = getPrimaryHorizontalForX(chipStart)
          val endX = getPrimaryHorizontalForX(chipEnd - 1)

          val offsetLineNumber = getLineForOffset(offset)
          val chipLineNumber = getLineForOffset(chipEnd - 1)

          if ((eventX in startX..endX) && offsetLineNumber == chipLineNumber) {
            return chip
          }
        }
      }
      return null
    }

    private fun getPrimaryHorizontalForX(offset: Int): Float {
      val layout = layout
      return layout.getPrimaryHorizontal(offset)
    }

    private fun getLineForOffset(offset: Int): Int {
      return layout.getLineForOffset(offset)
    }
  }

  companion object {
    const val CHIP_SEPARATOR_WHITESPACE = ' '
  }
}
