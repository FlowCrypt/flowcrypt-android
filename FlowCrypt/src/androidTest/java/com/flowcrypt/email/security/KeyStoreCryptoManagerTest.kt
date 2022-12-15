/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.security

import android.net.Uri
import android.util.Base64
import android.util.Base64InputStream
import android.util.Base64OutputStream
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.flowcrypt.email.api.email.MsgsCacheManager
import com.flowcrypt.email.base.BaseTest
import com.flowcrypt.email.util.exception.SyncTaskTerminatedException
import jakarta.mail.Session
import jakarta.mail.internet.MimeMessage
import okio.buffer
import org.apache.commons.io.IOUtils
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.util.Properties
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream

/**
 * @author Denis Bondarenko
 * Date: 6/9/20
 * Time: 9:09 AM
 * E-mail: DenBond7@gmail.com
 */
@SmallTest
@RunWith(AndroidJUnit4::class)
class KeyStoreCryptoManagerTest : BaseTest() {
  private val originalData = IOUtils.toString(
    getContext().assets.open(
      "messages/mime/standard_msg_info_plaintext.txt"
    ), StandardCharsets.UTF_8
  )

  @Before
  fun setUp() {
  }

  @After
  fun tearDown() {
  }

  @Test
  fun testDataAsString() {
    val encryptedData = KeyStoreCryptoManager.encrypt(originalData)
    val decryptedData = KeyStoreCryptoManager.decrypt(encryptedData)
    assertEquals(originalData, decryptedData)
  }

  @Test
  fun testDataAsStream() {
    val msg = MimeMessage(
      Session.getInstance(Properties()),
      getContext().assets.open("messages/mime/standard_msg_info_plaintext.txt")
    )

    val cipherForEncryption = KeyStoreCryptoManager.getCipherForEncryption()
    val byteArrayOutputStream = ByteArrayOutputStream()
    val base64OutputStream = Base64OutputStream(
      byteArrayOutputStream,
      KeyStoreCryptoManager.BASE64_FLAGS
    )
    val outputStream = CipherOutputStream(base64OutputStream, cipherForEncryption)

    outputStream.use {
      byteArrayOutputStream.write(
        Base64.encodeToString(
          cipherForEncryption.iv,
          KeyStoreCryptoManager.BASE64_FLAGS
        ).toByteArray()
      )
      byteArrayOutputStream.write("\n".toByteArray())
      msg.writeTo(outputStream)

      it.flush()
    }

    val cipherForDecryption = KeyStoreCryptoManager.getCipherForDecryption(
      Base64.encodeToString(cipherForEncryption.iv, KeyStoreCryptoManager.BASE64_FLAGS)
    )
    val byteArrayInputStream = ByteArrayInputStream(byteArrayOutputStream.toByteArray())
    val base64InputStream = Base64InputStream(
      byteArrayInputStream,
      KeyStoreCryptoManager.BASE64_FLAGS
    )
    val inputStream = CipherInputStream(base64InputStream, cipherForDecryption)

    val outputStreamFoResult = ByteArrayOutputStream()

    while (true) {
      val c = byteArrayInputStream.read()

      if (c == -1 || c == '\n'.code) {
        break
      }
    }

    inputStream.copyTo(outputStreamFoResult)

    val decodedDataAsString = String(outputStreamFoResult.toByteArray())
    assertEquals(
      originalData.replace("\r\n".toRegex(), "\n"),
      decodedDataAsString.replace("\r\n".toRegex(), "\n")
    )
  }

  @Test
  fun testDataAsStreamFromCacheManager() {
    val msg = MimeMessage(
      Session.getInstance(Properties()),
      getContext().assets.open("messages/mime/standard_msg_info_plaintext.txt")
    )
    val key = "temp"
    val editor = MsgsCacheManager.diskLruCache.edit(key) ?: return

    val bufferedSink = editor.newSink(0).buffer()
    val outputStreamOfBufferedSink = bufferedSink.outputStream()
    val cipherForEncryption = KeyStoreCryptoManager.getCipherForEncryption()
    val base64OutputStream = Base64OutputStream(
      outputStreamOfBufferedSink,
      KeyStoreCryptoManager.BASE64_FLAGS
    )
    val outputStream = CipherOutputStream(base64OutputStream, cipherForEncryption)

    try {
      outputStream.use {
        outputStreamOfBufferedSink.write(
          Base64.encodeToString(
            cipherForEncryption.iv,
            KeyStoreCryptoManager.BASE64_FLAGS
          ).toByteArray()
        )
        outputStreamOfBufferedSink.write("\n".toByteArray())
        msg.writeTo(it)
        bufferedSink.flush()
        editor.commit()
      }
    } catch (e: SyncTaskTerminatedException) {
      e.printStackTrace()
      editor.abort()
    }

    val snapshot = MsgsCacheManager.getMsgSnapshot(key) ?: throw IllegalArgumentException()
    val inputStreamFromUri = getTargetContext().contentResolver?.openInputStream(
      snapshot.getUri(0) ?: Uri.EMPTY
    )
    assertNotNull(inputStreamFromUri)

    val cipherForDecryption = KeyStoreCryptoManager.getCipherForDecryption(
      Base64.encodeToString(cipherForEncryption.iv, KeyStoreCryptoManager.BASE64_FLAGS)
    )
    val byteArrayInputStream = ByteArrayInputStream(inputStreamFromUri?.readBytes())
    val base64InputStream = Base64InputStream(
      byteArrayInputStream,
      KeyStoreCryptoManager.BASE64_FLAGS
    )
    val inputStream = CipherInputStream(base64InputStream, cipherForDecryption)

    val outputStreamFoResult = ByteArrayOutputStream()

    while (true) {
      val c = byteArrayInputStream.read()

      if (c == -1 || c == '\n'.code) {
        break
      }
    }

    inputStream.copyTo(outputStreamFoResult)

    val decodedDataAsString = String(outputStreamFoResult.toByteArray())
    assertEquals(
      originalData.replace("\r\n".toRegex(), "\n"),
      decodedDataAsString.replace("\r\n".toRegex(), "\n")
    )
  }
}
