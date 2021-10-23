/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.security.pgp

import android.os.Parcel
import android.os.Parcelable
import com.flowcrypt.email.util.exception.DecryptionException
import org.bouncycastle.openpgp.PGPDataValidationException
import org.bouncycastle.openpgp.PGPException
import org.bouncycastle.openpgp.PGPSecretKeyRingCollection
import org.pgpainless.PGPainless
import org.pgpainless.decryption_verification.ConsumerOptions
import org.pgpainless.decryption_verification.MissingKeyPassphraseStrategy
import org.pgpainless.decryption_verification.OpenPgpMetadata
import org.pgpainless.exception.MessageNotIntegrityProtectedException
import org.pgpainless.exception.MissingDecryptionMethodException
import org.pgpainless.exception.MissingPassphraseException
import org.pgpainless.exception.ModificationDetectionException
import org.pgpainless.exception.WrongPassphraseException
import org.pgpainless.key.protection.SecretKeyRingProtector
import java.io.ByteArrayOutputStream
import java.io.IOException
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
            .withOptions(
              ConsumerOptions()
                .addDecryptionKeys(pgpSecretKeyRingCollection, protector)
                .setMissingKeyPassphraseStrategy(MissingKeyPassphraseStrategy.THROW_EXCEPTION)
            )
          decryptionStream.use { it.copyTo(outStream) }
          return decryptionStream.result
        } catch (e: Exception) {
          throw processDecryptionException(e)
        }
      }
    }
  }

  fun decryptWithResult(
    srcInputStream: InputStream,
    pgpSecretKeyRingCollection: PGPSecretKeyRingCollection,
    protector: SecretKeyRingProtector,
    ignoreMdcErrors: Boolean = false
  ): DecryptionResult {
    srcInputStream.use { srcStream ->
      val destOutputStream = ByteArrayOutputStream()
      destOutputStream.use { outStream ->
        try {
          val decryptionStream = PGPainless.decryptAndOrVerify()
            .onInputStream(srcStream)
            .withOptions(
              ConsumerOptions()
                .addDecryptionKeys(pgpSecretKeyRingCollection, protector)
                .setMissingKeyPassphraseStrategy(MissingKeyPassphraseStrategy.THROW_EXCEPTION)
                .setIgnoreMDCErrors(ignoreMdcErrors)
            )

          decryptionStream.use { it.copyTo(outStream) }
          return DecryptionResult(
            content = destOutputStream,
            isEncrypted = decryptionStream.result.isEncrypted,
            isSigned = decryptionStream.result.isSigned,
            filename = decryptionStream.result.fileName
          )
        } catch (e: Exception) {
          return DecryptionResult.withError(
            processDecryptionException(e)
          )
        }
      }
    }
  }

  private fun processDecryptionException(e: Exception): DecryptionException {
    return when (e) {
      is WrongPassphraseException -> {
        DecryptionException(DecryptionErrorType.WRONG_PASSPHRASE, e)
      }

      is MissingPassphraseException -> {
        DecryptionException(
          decryptionErrorType = DecryptionErrorType.NEED_PASSPHRASE,
          e = PGPException("flowcrypt: need passphrase"),
          fingerprints = e.keyIds.map { it.fingerprint.toString().uppercase() }
        )
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

      else -> when {
        e is IOException && e.message.equals("crc check failed in armored message.", true) -> {
          DecryptionException(DecryptionErrorType.FORMAT, e)
        }

        e is IOException && e.message?.startsWith("unexpected packet in stream", true) == true -> {
          DecryptionException(DecryptionErrorType.FORMAT, e)
        }

        else -> DecryptionException(DecryptionErrorType.OTHER, e)
      }
    }
  }

  data class DecryptionResult(
    // provided if decryption was successful
    val content: ByteArrayOutputStream? = null,

    // true if message was encrypted.
    // Alternatively false (because it could have also been plaintext signed,
    // or wrapped in PGP armor as plaintext packet without encrypting)
    // also false when error happens.
    val isEncrypted: Boolean = false,

    val isSigned: Boolean = false,

    // pgp messages may include original filename in them
    val filename: String? = null,

    // todo later - signature verification not supported on Android yet
    val signature: String? = null,

    // provided if error happens
    val exception: DecryptionException? = null
  ) {
    companion object {
      fun withError(exception: DecryptionException): DecryptionResult {
        return DecryptionResult(exception = exception)
      }
    }
  }

  enum class DecryptionErrorType : Parcelable {
    KEY_MISMATCH,
    WRONG_PASSPHRASE,
    NO_MDC,
    BAD_MDC,
    NEED_PASSPHRASE,
    FORMAT,
    OTHER;

    override fun describeContents(): Int {
      return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
      dest.writeInt(ordinal)
    }

    companion object CREATOR : Parcelable.Creator<DecryptionErrorType> {
      override fun createFromParcel(parcel: Parcel) = values()[parcel.readInt()]
      override fun newArray(size: Int): Array<DecryptionErrorType?> = arrayOfNulls(size)
    }
  }
}
