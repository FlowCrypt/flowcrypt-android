/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.security.pgp

import android.os.Parcelable
import android.util.Log
import com.flowcrypt.email.util.exception.DecryptionException
import kotlinx.parcelize.Parcelize
import org.bouncycastle.openpgp.PGPDataValidationException
import org.bouncycastle.openpgp.PGPException
import org.bouncycastle.openpgp.PGPPublicKeyRingCollection
import org.bouncycastle.openpgp.PGPSecretKeyRingCollection
import org.pgpainless.PGPainless
import org.pgpainless.decryption_verification.ConsumerOptions
import org.pgpainless.decryption_verification.DecryptionStream
import org.pgpainless.decryption_verification.MissingKeyPassphraseStrategy
import org.pgpainless.decryption_verification.OpenPgpMetadata
import org.pgpainless.exception.MessageNotIntegrityProtectedException
import org.pgpainless.exception.MissingDecryptionMethodException
import org.pgpainless.exception.MissingPassphraseException
import org.pgpainless.exception.ModificationDetectionException
import org.pgpainless.exception.WrongPassphraseException
import org.pgpainless.key.protection.SecretKeyRingProtector
import org.pgpainless.util.Passphrase
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/**
 * @author Denys Bondarenko
 */
object PgpDecryptAndOrVerify {
  fun decrypt(
    srcInputStream: InputStream,
    destOutputStream: OutputStream,
    secretKeys: PGPSecretKeyRingCollection,
    protector: SecretKeyRingProtector,
    passphrase: Passphrase? = null,
  ): OpenPgpMetadata {
    srcInputStream.use { srcStream ->
      destOutputStream.use { outStream ->
        try {
          val decryptionStream = genDecryptionStream(
            srcInputStream = srcStream,
            secretKeys = secretKeys,
            protector = protector,
            passphrase = passphrase
          )
          decryptionStream.use { it.copyTo(outStream) }
          return decryptionStream.result
        } catch (e: Exception) {
          throw processDecryptionException(e)
        }
      }
    }
  }

  fun decryptAndOrVerifyWithResult(
    srcInputStream: InputStream,
    publicKeys: PGPPublicKeyRingCollection? = null,
    secretKeys: PGPSecretKeyRingCollection? = null,
    protector: SecretKeyRingProtector? = null,
    passphrase: Passphrase? = null,
    ignoreMdcErrors: Boolean = false,
    attempts: Int = 1,
    action: (i: Int) -> Unit
  ): DecryptionResult {
    var decryptionResult: DecryptionResult =
      DecryptionResult.withError(processDecryptionException(IllegalStateException()))
    for (i in 0 until attempts) {
      if (srcInputStream.markSupported()) {
        srcInputStream.reset()
      }
      try {
        srcInputStream.use { srcStream ->
          val destOutputStream = ByteArrayOutputStream()
          destOutputStream.use { outStream ->

            val decryptionStream = genDecryptionStream(
              srcInputStream = srcStream,
              publicKeys = publicKeys,
              secretKeys = secretKeys,
              protector = protector,
              passphrase = passphrase,
              ignoreMdcErrors = ignoreMdcErrors
            )

            Log.d("DDDDDD", "try to decrypt = $i + ${Thread.currentThread().name}")

            decryptionStream.use { it.copyTo(outStream) }
            decryptionResult = DecryptionResult(
              openPgpMetadata = decryptionStream.result,
              content = destOutputStream
            )
            action.invoke(i)
          }
        }
      } catch (e: Exception) {
        Log.d(
          "DDDDDD",
          "Attempt with exception = $i + ${Thread.currentThread().name} | $srcInputStream | $publicKeys | $secretKeys | $protector"
        )
        e.printStackTrace()
        return DecryptionResult.withError(
          processDecryptionException(e)
        )
      }
    }

    Log.d(
      "DDDDDD",
      "Attempt = $attempts + ${Thread.currentThread().name} | $srcInputStream | $publicKeys | $secretKeys | $protector"
    )
    return decryptionResult
  }

  @Suppress("DEPRECATION")
  //@Synchronized
  fun genDecryptionStream(
    srcInputStream: InputStream,
    publicKeys: PGPPublicKeyRingCollection? = null,
    secretKeys: PGPSecretKeyRingCollection? = null,
    protector: SecretKeyRingProtector? = null,
    passphrase: Passphrase? = null,
    ignoreMdcErrors: Boolean = false
  ): DecryptionStream {
    return PGPainless.decryptAndOrVerify()
      .onInputStream(srcInputStream)
      .withOptions(
        ConsumerOptions()
          .setMissingKeyPassphraseStrategy(MissingKeyPassphraseStrategy.THROW_EXCEPTION)
          .setIgnoreMDCErrors(ignoreMdcErrors)
          .apply {
            if (secretKeys != null && protector != null) {
              addDecryptionKeys(secretKeys, protector)
            }
            passphrase?.let { addDecryptionPassphrase(it) }
            publicKeys?.let { addVerificationCerts(it) }
          }
      )
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

      else -> {
        val exception = e.cause ?: e

        when {
          exception is IOException && exception.message.equals(
            "crc check failed in armored message.",
            true
          ) -> {
            DecryptionException(DecryptionErrorType.FORMAT, e)
          }

          exception is IOException && exception.message?.startsWith(
            "unexpected packet in stream",
            true
          ) == true -> {
            DecryptionException(DecryptionErrorType.FORMAT, e)
          }

          else -> DecryptionException(DecryptionErrorType.OTHER, e)
        }
      }
    }
  }

  data class DecryptionResult(
    val openPgpMetadata: OpenPgpMetadata? = null,
    // provided if decryption was successful
    val content: ByteArrayOutputStream? = null,

    // todo later - signature verification not supported on Android yet
    val signature: String? = null,

    // provided if error happens
    val exception: DecryptionException? = null
  ) {
    /**
     * true if message was encrypted.
     * Alternatively false (because it could have also been plaintext signed, or wrapped in
     * PGP armor as plaintext packet without encrypting). Also false when error happens.
     */
    val isEncrypted: Boolean = openPgpMetadata?.isEncrypted ?: false
    val isSigned = openPgpMetadata?.isSigned ?: false

    // pgp messages may include original filename in them
    val filename = openPgpMetadata?.fileName

    companion object {
      fun withError(exception: DecryptionException): DecryptionResult {
        return DecryptionResult(exception = exception)
      }
    }
  }

  @Parcelize
  enum class DecryptionErrorType : Parcelable {
    KEY_MISMATCH,
    WRONG_PASSPHRASE,
    NO_MDC,
    BAD_MDC,
    NEED_PASSPHRASE,
    FORMAT,
    OTHER;
  }
}
