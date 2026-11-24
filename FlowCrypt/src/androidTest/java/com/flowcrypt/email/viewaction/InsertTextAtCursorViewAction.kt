/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.viewaction

import android.view.View
import android.widget.EditText
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import org.hamcrest.Matcher
import org.hamcrest.Matchers.allOf

/**
 * Inserts [text] at the cursor position of an [EditText] and keeps the existing text.
 *
 * Typing with key events can lose the first characters on a slow emulator while the IME
 * connection is restarted after a focus change. This action changes the text directly, like
 * replaceText(), but does not remove the text that is already there (e.g. a signature).
 */
class InsertTextAtCursorViewAction(private val text: String) : ViewAction {
  override fun getConstraints(): Matcher<View> {
    return allOf(isDisplayed(), isAssignableFrom(EditText::class.java))
  }

  override fun getDescription(): String {
    return "insert text \"$text\" at the cursor position"
  }

  override fun perform(uiController: UiController, view: View) {
    val editText = view as EditText
    val editable = editText.text
    val position = editText.selectionStart.takeIf { it in 0..editable.length } ?: 0
    editable.insert(position, text)
    editText.setSelection(position + text.length)
    uiController.loopMainThreadUntilIdle()
  }
}
