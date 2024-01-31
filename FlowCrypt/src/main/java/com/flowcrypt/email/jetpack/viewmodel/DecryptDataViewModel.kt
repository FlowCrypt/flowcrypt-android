/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.jetpack.viewmodel

import android.app.Application
import android.content.Context
import com.flowcrypt.email.security.KeysStorageImpl
import com.flowcrypt.email.security.pgp.PgpDecryptAndOrVerify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.bouncycastle.openpgp.PGPSecretKeyRingCollection
import java.io.ByteArrayOutputStream
import java.io.InputStream

/**
 * @author Denys Bondarenko
 */
abstract class DecryptDataViewModel(application: Application) : AccountViewModel(application) {
  open suspend fun decryptDataIfNeeded(
    context: Context,
    inputStream: InputStream
  ): ByteArray =
    withContext(Dispatchers.IO) {
      inputStream.use {
        val byteArrayOutputStream = ByteArrayOutputStream()
        val pgpSecretKeyRings = KeysStorageImpl.getInstance(context).getPGPSecretKeyRings()
        val pgpSecretKeyRingCollection = PGPSecretKeyRingCollection(pgpSecretKeyRings)
        val protector = KeysStorageImpl.getInstance(context).getSecretKeyRingProtector()

        PgpDecryptAndOrVerify.decrypt(
          srcInputStream = inputStream,
          destOutputStream = byteArrayOutputStream,
          secretKeys = pgpSecretKeyRingCollection,
          protector = protector
        )

        return@withContext byteArrayOutputStream.toByteArray()
      }
    }
}