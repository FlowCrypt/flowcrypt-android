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
 * @author Denis Bondarenko
 * Date: 01.08.2017
 * Time: 11:29
 * E-mail: DenBond7@gmail.com
 */
@Parcelize
data class DialogItem constructor(
  val iconResourceId: Int = 0,
  val title: String = "",
  val id: Int = 0
) : Parcelable
