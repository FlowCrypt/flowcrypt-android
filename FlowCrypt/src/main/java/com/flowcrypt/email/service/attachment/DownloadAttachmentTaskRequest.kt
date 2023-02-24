/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.service.attachment

import android.content.Context

import com.flowcrypt.email.api.email.model.AttachmentInfo

/**
 * This class will be used to define information about a new download attachment task.
 *
 * @author Denys Bondarenko
 */
class DownloadAttachmentTaskRequest(context: Context, val attInfo: AttachmentInfo) {
  val context: Context = context.applicationContext
}
