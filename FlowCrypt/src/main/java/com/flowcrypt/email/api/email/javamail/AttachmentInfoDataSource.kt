/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.javamail

import android.content.Context
import android.net.Uri
import android.text.TextUtils
import com.flowcrypt.email.Constants
import com.flowcrypt.email.api.email.model.AttachmentInfo
import jakarta.activation.DataSource
import java.io.BufferedInputStream
import java.io.InputStream
import java.io.OutputStream

/**
 * The [DataSource] realization for a file which received from [Uri]
 *
 * @author Denys Bondarenko
 */
open class AttachmentInfoDataSource(private val context: Context, val att: AttachmentInfo) :
  DataSource {

  override fun getInputStream(): InputStream? {
    return att.uri?.let { uri ->
      context.contentResolver.openInputStream(uri)?.let { stream -> BufferedInputStream(stream) }
    } ?: att.rawData?.inputStream()
  }

  override fun getOutputStream(): OutputStream? = null

  /**
   * If a content type is unknown we return "application/octet-stream".
   * http://www.rfc-editor.org/rfc/rfc2046.txt (section 4.5.1.  Octet-Stream Subtype)
   */
  override fun getContentType(): String =
    if (TextUtils.isEmpty(att.type)) Constants.MIME_TYPE_BINARY_DATA else att.type

  override fun getName(): String = att.getSafeName()
}
