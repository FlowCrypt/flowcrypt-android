/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.javamail

import android.content.Context
import com.flowcrypt.email.api.email.model.AttachmentInfo
import com.flowcrypt.email.security.pgp.PgpDecryptAndOrVerify
import com.flowcrypt.email.security.pgp.PgpEncryptAndOrSign
import org.bouncycastle.openpgp.PGPSecretKeyRingCollection
import org.pgpainless.key.protection.SecretKeyRingProtector
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream

/**
 * @author Denis Bondarenko
 *         Date: 12/29/21
 *         Time: 5:28 PM
 *         E-mail: DenBond7@gmail.com
 */
class ForwardedAttachmentInfoDataSource(
  context: Context,
  att: AttachmentInfo,
  private val shouldBeEncrypted: Boolean,
  private val publicKeys: List<String>? = null,
  private val secretKeys: PGPSecretKeyRingCollection,
  private val protector: SecretKeyRingProtector
) : AttachmentInfoDataSource(context, att) {
  override fun getInputStream(): InputStream? {
    val inputStream = super.getInputStream() ?: return null
    val srcInputStream = if (att.decryptWhenForward) PgpDecryptAndOrVerify.genDecryptionStream(
      srcInputStream = inputStream,
      secretKeys = secretKeys,
      protector = protector
    ) else inputStream

    return if (shouldBeEncrypted) {
      //here we use [ByteArrayOutputStream] as a temp destination of encrypted data.
      //todo-denbond7 it should be improved in the future for better performance
      val tempByteArrayOutputStream = ByteArrayOutputStream()
      PgpEncryptAndOrSign.encryptAndOrSign(
        srcInputStream = srcInputStream,
        destOutputStream = tempByteArrayOutputStream,
        pubKeys = requireNotNull(publicKeys)
      )

      return ByteArrayInputStream(tempByteArrayOutputStream.toByteArray())
    } else {
      srcInputStream
    }
  }
}
