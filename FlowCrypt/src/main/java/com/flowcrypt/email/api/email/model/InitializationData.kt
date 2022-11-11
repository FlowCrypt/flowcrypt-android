/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.api.email.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class InitializationData(
  var subject: String? = null,
  var body: String? = null,
  val toAddresses: java.util.ArrayList<String> = arrayListOf(),
  val ccAddresses: java.util.ArrayList<String> = arrayListOf(),
  val bccAddresses: java.util.ArrayList<String> = arrayListOf()
) : Parcelable
