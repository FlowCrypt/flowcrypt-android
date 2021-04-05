/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.security

import android.util.Base64
import android.util.Base64InputStream
import android.util.Base64OutputStream
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import com.flowcrypt.email.api.email.MsgsCacheManager
import com.flowcrypt.email.junit.annotations.DoesNotNeedMailserver
import com.flowcrypt.email.junit.annotations.ReadyForCIAnnotation
import com.flowcrypt.email.util.exception.SyncTaskTerminatedException
import okio.buffer
import org.apache.commons.io.IOUtils
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.util.*
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.mail.Session
import javax.mail.internet.MimeMessage

/**
 * @author Denis Bondarenko
 * Date: 6/9/20
 * Time: 9:09 AM
 * E-mail: DenBond7@gmail.com
 */
@SmallTest
@RunWith(AndroidJUnit4::class)
class KeyStoreCryptoManagerTest {
  private val originalData = IOUtils.toString(InstrumentationRegistry
      .getInstrumentation().context.assets.open("messages/mime/standard_msg_info_plain_text" +
          ".txt"), StandardCharsets.UTF_8)

  @Before
  fun setUp() {
  }

  @After
  fun tearDown() {
  }

  @Test
  @DoesNotNeedMailserver
  @ReadyForCIAnnotation
  fun testDataAsString() {
    val encryptedData = KeyStoreCryptoManager.encrypt(originalData)
    println(encryptedData)

    val decryptedData = KeyStoreCryptoManager.decrypt(encryptedData)
    assertTrue(originalData == decryptedData)
  }

  @Test
  @DoesNotNeedMailserver
  @ReadyForCIAnnotation
  fun testDataAsStream() {
    val msg = MimeMessage(Session.getInstance(Properties()), InstrumentationRegistry
        .getInstrumentation().context.assets.open("messages/mime/standard_msg_info_plain_text.txt"))

    val cipherForEncryption = KeyStoreCryptoManager.getCipherForEncryption()
    val byteArrayOutputStream = ByteArrayOutputStream()
    val base64OutputStream = Base64OutputStream(byteArrayOutputStream, KeyStoreCryptoManager.BASE64_FLAGS)
    val outputStream = CipherOutputStream(base64OutputStream, cipherForEncryption)

    outputStream.use {
      byteArrayOutputStream.write(Base64.encodeToString(cipherForEncryption.iv, KeyStoreCryptoManager.BASE64_FLAGS).toByteArray())
      byteArrayOutputStream.write("\n".toByteArray())
      msg.writeTo(outputStream)

      it.flush()
    }

    val cipherForDecryption = KeyStoreCryptoManager.getCipherForDecryption(Base64.encodeToString(cipherForEncryption.iv, KeyStoreCryptoManager.BASE64_FLAGS))
    val byteArrayInputStream = ByteArrayInputStream(byteArrayOutputStream.toByteArray())
    val base64InputStream = Base64InputStream(byteArrayInputStream, KeyStoreCryptoManager.BASE64_FLAGS)
    val inputStream = CipherInputStream(base64InputStream, cipherForDecryption)

    val outputStreamFoResult = ByteArrayOutputStream()

    while (true) {
      val c = byteArrayInputStream.read()

      if (c == -1 || c == '\n'.toInt()) {
        break
      }
    }

    inputStream.copyTo(outputStreamFoResult)

    val decodedDataAsString = String(outputStreamFoResult.toByteArray())
    assertTrue(originalData.replace("\r\n".toRegex(), "\n") == decodedDataAsString.replace("\r\n".toRegex(), "\n"))
  }

  @Test
  @DoesNotNeedMailserver
  @ReadyForCIAnnotation
  fun testDataAsStreamFromCacheManager() {
    val msg = MimeMessage(Session.getInstance(Properties()), InstrumentationRegistry
        .getInstrumentation().context.assets.open("messages/mime/standard_msg_info_plain_text.txt"))
    val key = "temp"
    val editor = MsgsCacheManager.diskLruCache.edit(key) ?: return

    val bufferedSink = editor.newSink(0).buffer()
    val outputStreamOfBufferedSink = bufferedSink.outputStream()
    val cipherForEncryption = KeyStoreCryptoManager.getCipherForEncryption()
    val base64OutputStream = Base64OutputStream(outputStreamOfBufferedSink, KeyStoreCryptoManager.BASE64_FLAGS)
    val outputStream = CipherOutputStream(base64OutputStream, cipherForEncryption)

    try {
      outputStream.use {
        outputStreamOfBufferedSink.write(Base64.encodeToString(cipherForEncryption.iv, KeyStoreCryptoManager.BASE64_FLAGS).toByteArray())
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
    val inputStreamFromUri = InstrumentationRegistry.getInstrumentation().targetContext?.contentResolver?.openInputStream(snapshot.getUri(0)
        ?: throw NullPointerException()) ?: throw   java.lang.NullPointerException()

    val cipherForDecryption = KeyStoreCryptoManager.getCipherForDecryption(Base64.encodeToString(cipherForEncryption.iv, KeyStoreCryptoManager.BASE64_FLAGS))
    val byteArrayInputStream = ByteArrayInputStream(inputStreamFromUri.readBytes())
    val base64InputStream = Base64InputStream(byteArrayInputStream, KeyStoreCryptoManager.BASE64_FLAGS)
    val inputStream = CipherInputStream(base64InputStream, cipherForDecryption)

    val outputStreamFoResult = ByteArrayOutputStream()

    while (true) {
      val c = byteArrayInputStream.read()

      if (c == -1 || c == '\n'.toInt()) {
        break
      }
    }

    inputStream.copyTo(outputStreamFoResult)

    val decodedDataAsString = String(outputStreamFoResult.toByteArray())
    assertTrue(originalData.replace("\r\n".toRegex(), "\n") == decodedDataAsString.replace("\r\n".toRegex(), "\n"))
  }
}