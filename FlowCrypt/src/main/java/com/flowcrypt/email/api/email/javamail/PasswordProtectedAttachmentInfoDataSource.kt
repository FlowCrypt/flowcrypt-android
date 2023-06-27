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
import java.io.BufferedInputStream
import java.io.InputStream

/**
 * It's a special version of [AttachmentInfoDataSource] which decrypts a source before using.
 * Due to https://github.com/pgpainless/pgpainless/pull/360 we have to use a custom implementation
 * for streams here. For the first call of [jakarta.activation.DataSource.getInputStream]
 * we will not care about errors for [InputStream.close].
 *
 * Also, it drops a file extension(as this an incoming attachment here always will have '.pgp').
 *
 * @author Denys Bondarenko
 */
class PasswordProtectedAttachmentInfoDataSource(
  context: Context,
  att: AttachmentInfo,
  private val secretKeys: PGPSecretKeyRingCollection,
  private val protector: SecretKeyRingProtector
) : AttachmentInfoDataSource(context, att) {
  private var isFirstUsage = false
  override fun getInputStream(): InputStream? {
    val inputStream = super.getInputStream() ?: return null
    val decryptionStream = PgpDecryptAndOrVerify.genDecryptionStream(
      srcInputStream = inputStream,
      secretKeys = secretKeys,
      protector = protector
    )

    return if (isFirstUsage) {
      decryptionStream
    } else {
      isFirstUsage = true
      object : BufferedInputStream(decryptionStream) {
        override fun close() {
          try {
            super.close()
          } catch (e: Exception) {
            //we don't care about an exception here. Can be skipped.
          }
        }
      }
    }
  }

  override fun getName(): String {
    return FilenameUtils.removeExtension(super.getName())
  }
}
