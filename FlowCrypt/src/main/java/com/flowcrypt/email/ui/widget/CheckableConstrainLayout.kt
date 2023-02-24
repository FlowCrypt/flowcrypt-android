/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.widget

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.Checkable
import android.widget.CompoundButton
import androidx.constraintlayout.widget.ConstraintLayout

/**
 * It's a custom realization of [ConstraintLayout] which implements [Checkable] interface. This
 * view group should contain only one view which extends [CompoundButton].
 *
 * @author Denys Bondarenko
 */
class CheckableConstrainLayout(context: Context, attrs: AttributeSet) :
  ConstraintLayout(context, attrs), Checkable {
  private var compoundButton: CompoundButton? = null

  override fun isChecked(): Boolean {
    return compoundButton?.isChecked ?: false
  }

  override fun toggle() {
    compoundButton?.toggle()
  }

  override fun setChecked(checked: Boolean) {
    compoundButton?.isChecked = checked
  }

  override fun addView(child: View?, index: Int, params: ViewGroup.LayoutParams?) {
    super.addView(child, index, params)

    if (child is CompoundButton) {
      if (compoundButton == null) {
        compoundButton = child
        compoundButton?.isClickable = false
        compoundButton?.isFocusable = false
        compoundButton?.isFocusableInTouchMode = false
      } else throw IllegalStateException("Only one view which extends CompoundButton is supported")
    }
  }
}
