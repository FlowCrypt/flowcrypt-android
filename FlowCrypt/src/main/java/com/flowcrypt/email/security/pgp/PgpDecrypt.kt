/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.security.pgp

import com.flowcrypt.email.util.exception.DecryptionException
import org.bouncycastle.openpgp.PGPDataValidationException
import org.bouncycastle.openpgp.PGPSecretKeyRingCollection
import org.pgpainless.PGPainless
import org.pgpainless.decryption_verification.OpenPgpMetadata
import org.pgpainless.exception.MessageNotIntegrityProtectedException
import org.pgpainless.exception.MissingDecryptionMethodException
import org.pgpainless.exception.ModificationDetectionException
import org.pgpainless.exception.WrongPassphraseException
import org.pgpainless.key.protection.SecretKeyRingProtector
import java.io.InputStream
import java.io.OutputStream

/**
 * @author Denis Bondarenko
 *         Date: 5/11/21
 *         Time: 2:10 PM
 *         E-mail: DenBond7@gmail.com
 */
object PgpDecrypt {
  val DETECT_SEPARATE_ENCRYPTED_ATTACHMENTS_PATTERN =
    "(?i)(\\.pgp$)|(\\.gpg$)|(\\.[a-zA-Z0-9]{3,4}\\.asc$)".toRegex()

  fun decrypt(
    srcInputStream: InputStream,
    destOutputStream: OutputStream,
    pgpSecretKeyRingCollection: PGPSecretKeyRingCollection,
    protector: SecretKeyRingProtector
  ): OpenPgpMetadata {
    srcInputStream.use { srcStream ->
      destOutputStream.use { outStream ->
        try {
          val decryptionStream = PGPainless.decryptAndOrVerify()
            .onInputStream(srcStream)
            .decryptWith(protector, pgpSecretKeyRingCollection)
            .doNotVerify()
            .build()

          decryptionStream.use { it.copyTo(outStream) }
          return decryptionStream.result
        } catch (e: Exception) {
          throw processDecryptionException(e)
        }
      }
    }
  }

  private fun processDecryptionException(e: Exception): Exception {
    return when (e) {
      is WrongPassphraseException -> {
        DecryptionException(DecryptionErrorType.WRONG_PASSPHRASE, e)
      }

      is MessageNotIntegrityProtectedException -> {
        DecryptionException(DecryptionErrorType.NO_MDC, e)
      }

      is ModificationDetectionException -> {
        DecryptionException(DecryptionErrorType.BAD_MDC, e)
      }

      is PGPDataValidationException, is MissingDecryptionMethodException -> {
        DecryptionException(DecryptionErrorType.KEY_MISMATCH, e)
      }

      is DecryptionException -> e

      else -> DecryptionException(DecryptionErrorType.OTHER, e)
    }
  }

  enum class DecryptionErrorType {
    KEY_MISMATCH,
    WRONG_PASSPHRASE,
    NO_MDC,
    BAD_MDC,
    NEED_PASSPHRASE,
    FORMAT,
    OTHER
  }
}
