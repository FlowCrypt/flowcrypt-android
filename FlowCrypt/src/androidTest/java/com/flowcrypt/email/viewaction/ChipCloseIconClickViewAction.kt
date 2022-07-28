/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.viewaction

import android.view.View
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import com.google.android.material.chip.Chip

class ChipCloseIconClickViewAction : ViewAction {
  override fun getConstraints() = null
  override fun getDescription() = "Click on a close icon"
  override fun perform(uiController: UiController, view: View) {
    (view as? Chip)?.performCloseIconClick()
  }
}
