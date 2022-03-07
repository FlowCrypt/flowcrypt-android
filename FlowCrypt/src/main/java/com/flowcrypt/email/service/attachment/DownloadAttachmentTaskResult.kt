/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.service.attachment

import android.net.Uri
import com.flowcrypt.email.api.email.model.AttachmentInfo

/**
 * This class defines the download attachment task result.
 *
 * @author Denis Bondarenko
 * Date: 16.08.2017
 * Time: 16:22
 * E-mail: DenBond7@gmail.com
 */
data class DownloadAttachmentTaskResult constructor(
  val attInfo: AttachmentInfo? = null,
  val exception: Exception? = null,
  val uri: Uri? = null,
  val progressInPercentage: Int = 0,
  val timeLeft: Long = 0,
  val isLast: Boolean = false,
  val canBeOpened: Boolean = true
)
