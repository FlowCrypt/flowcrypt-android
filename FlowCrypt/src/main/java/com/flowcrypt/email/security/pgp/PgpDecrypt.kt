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
import org.bouncycastle.openpgp.PGPKeyRing
import org.bouncycastle.openpgp.PGPSecretKeyRingCollection
import org.pgpainless.PGPainless
import org.pgpainless.decryption_verification.ConsumerOptions
import org.pgpainless.decryption_verification.MessageInspector
import org.pgpainless.decryption_verification.OpenPgpMetadata
import org.pgpainless.exception.MessageNotIntegrityProtectedException
import org.pgpainless.exception.MissingDecryptionMethodException
import org.pgpainless.exception.ModificationDetectionException
import org.pgpainless.exception.WrongPassphraseException
import org.pgpainless.key.OpenPgpV4Fingerprint
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
    protector: SecretKeyRingProtector
  ): DecryptionResult {
    srcInputStream.use { srcStream ->
      val destOutputStream = ByteArrayOutputStream()
      destOutputStream.use { outStream ->
        if (srcStream.markSupported()) {
          srcStream.mark(0)
        }

        try {
          val decryptionStream = PGPainless.decryptAndOrVerify()
            .onInputStream(srcStream)
            .withOptions(
              ConsumerOptions()
                .addDecryptionKeys(pgpSecretKeyRingCollection, protector)
            )

          decryptionStream.use { it.copyTo(outStream) }
          return DecryptionResult.withDecrypted(
            content = destOutputStream,
            filename = decryptionStream.result.fileName
          )
        } catch (e: Exception) {
          if (e is DecryptionException && e.decryptionErrorType == DecryptionErrorType.NEED_PASSPHRASE) {
            if (srcStream.markSupported()) {
              srcStream.reset()
              val keyIds = MessageInspector.determineEncryptionInfoForMessage(srcInputStream).keyIds
              val fingerprints = mutableListOf<String>()
              for (id in keyIds) {
                var key: PGPKeyRing? = null
                try {
                  key = pgpSecretKeyRingCollection.getSecretKeyRing(id)
                  protector.getDecryptor(id)
                } catch (e: DecryptionException) {
                  key?.let {
                    val fingerprint = OpenPgpV4Fingerprint(key)
                    fingerprints.add(fingerprint.toString())
                  }
                }
              }

              if (fingerprints.isNotEmpty()) {
                return DecryptionResult.withError(
                  processDecryptionException(
                    DecryptionException(
                      decryptionErrorType = DecryptionErrorType.NEED_PASSPHRASE,
                      e = PGPException("flowcrypt: need passphrase"),
                      fingerprints = fingerprints
                    )
                  )
                )
              }
            }
          }

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

      fun withDecrypted(content: ByteArrayOutputStream, filename: String?): DecryptionResult {
        return DecryptionResult(content = content, isEncrypted = true, filename = filename)
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
