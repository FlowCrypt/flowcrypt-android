/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Simple POJO class which describes a dialog item.
 *
 * @author Denys Bondarenko
 */
@Parcelize
data class DialogItem constructor(
  val iconResourceId: Int = 0,
  val title: String = "",
  val id: Int = 0
) : Parcelable
