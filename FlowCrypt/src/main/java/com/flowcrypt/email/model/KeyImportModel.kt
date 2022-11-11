/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.model

import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * This model describes info about an imported key.
 *
 * @author Denis Bondarenko
 * Date: 09.08.2018
 * Time: 15:47
 * E-mail: DenBond7@gmail.com
 */
@Parcelize
data class KeyImportModel constructor(
  val fileUri: Uri? = null,
  val keyString: String? = null,
  val isPrivateKey: Boolean = false,
  val sourceType: KeyImportDetails.SourceType
) : Parcelable
