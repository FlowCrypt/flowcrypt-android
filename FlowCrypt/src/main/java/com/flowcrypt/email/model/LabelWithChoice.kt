/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.model

import androidx.annotation.IntRange
import com.google.android.material.checkbox.MaterialCheckBox

/**
 * @author Denys Bondarenko
 */
data class LabelWithChoice(
  val name: String,
  val id: String,
  val backgroundColor: String? = null,
  val textColor: String? = null,
  @IntRange(
    from = MaterialCheckBox.STATE_UNCHECKED * 1L,
    to = MaterialCheckBox.STATE_INDETERMINATE * 1L
  )
  val initialState: Int,
  @IntRange(
    from = MaterialCheckBox.STATE_UNCHECKED * 1L,
    to = MaterialCheckBox.STATE_INDETERMINATE * 1L
  )
  val state: Int
)