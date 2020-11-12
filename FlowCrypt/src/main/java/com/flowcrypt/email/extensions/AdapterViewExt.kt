/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.extensions

import android.view.View
import android.widget.AdapterView

inline fun AdapterView<*>.onItemSelected(
    crossinline action: (parent: AdapterView<*>?,
                         view: View?,
                         position: Int,
                         id: Long) -> Unit
) = setOnItemSelectedListener(onItemSelected = action)

inline fun AdapterView<*>.setOnItemSelectedListener(
    crossinline onItemSelected: (
        parent: AdapterView<*>?,
        view: View?,
        position: Int,
        id: Long
    ) -> Unit = { _, _, _, _ -> },
    crossinline onNothingSelected: (
        parent: AdapterView<*>?
    ) -> Unit = { _ -> }
): AdapterView.OnItemSelectedListener {
  val listener = object : AdapterView.OnItemSelectedListener {
    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
      onItemSelected.invoke(parent, view, position, id)
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
      onNothingSelected.invoke(parent)
    }
  }
  onItemSelectedListener = listener
  return listener
}