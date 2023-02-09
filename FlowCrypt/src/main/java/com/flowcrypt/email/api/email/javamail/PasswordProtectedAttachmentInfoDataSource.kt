/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.javamail

import android.content.Context
import com.flowcrypt.email.api.email.model.AttachmentInfo
import com.flowcrypt.email.security.pgp.PgpDecryptAndOrVerify
import org.apache.commons.io.FilenameUtils
import org.bouncycastle.openpgp.PGPSecretKeyRingCollection
import org.pgpainless.key.protection.SecretKeyRingProtector
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream

/**
 * It's a special version of [AttachmentInfoDataSource] which decrypts a source before using.
 * Also, it drops a file extension(as this an incoming attachment here always will have '.pgp').
 *
 * @author Denis Bondarenko
 *         Date: 2/6/23
 *         Time: 4:38 PM
 *         E-mail: DenBond7@gmail.com
 */
class PasswordProtectedAttachmentInfoDataSource(
  context: Context,
  att: AttachmentInfo,
  private val secretKeys: PGPSecretKeyRingCollection,
  private val protector: SecretKeyRingProtector
) : AttachmentInfoDataSource(context, att) {
  override fun getInputStream(): InputStream? {
    val inputStream = super.getInputStream() ?: return null
    val buffer = ByteArrayOutputStream()
    val decryptionStream = PgpDecryptAndOrVerify.genDecryptionStream(
      srcInputStream = inputStream,
      secretKeys = secretKeys,
      protector = protector
    )
    decryptionStream.copyTo(buffer)
    //todo-denbond7 should be improved to use a stream directly
    return ByteArrayInputStream(buffer.toByteArray())
  }

  override fun getName(): String {
    return FilenameUtils.removeExtension(super.getName())
  }
}
