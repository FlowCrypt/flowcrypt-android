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
  private val originalData = "It's a simple text for testing"

  @Before
  fun setUp() {
  }

  @After
  fun tearDown() {
  }

  @Test
  fun testDataAsString() {
    val encryptedData = KeyStoreCryptoManager.encrypt(originalData)
    println(encryptedData)

    val decryptedData = KeyStoreCryptoManager.decrypt(encryptedData)
    assertTrue(originalData == decryptedData)
  }

  @Test
  fun testDataAsStream() {
    val originalMsgText = IOUtils.toString(InstrumentationRegistry
        .getInstrumentation().context.assets.open("messages/mime/standard_msg_info_plain_text" +
            ".txt"), StandardCharsets.UTF_8).replace("\n".toRegex(), "\r\n")
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
    assertTrue(originalMsgText.replace("\r\n".toRegex(), "\n") == decodedDataAsString.replace("\r\n".toRegex(), "\n"))
  }
}