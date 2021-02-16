/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.rules

import com.flowcrypt.email.util.TestGeneralUtil
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockWebServer
import okhttp3.tls.HandshakeCertificates
import okhttp3.tls.HeldCertificate
import org.junit.runner.Description
import org.junit.runners.model.Statement
import java.net.InetAddress
import java.security.KeyFactory
import java.security.KeyPair
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import javax.net.ssl.SSLSocketFactory


/**
 * This rule describes a logic of work of a mock web server
 *
 * @author Denis Bondarenko
 *         Date: 10/31/19
 *         Time: 2:59 PM
 *         E-mail: DenBond7@gmail.com
 */
class FlowCryptMockWebServerRule(val port: Int, val responseDispatcher: Dispatcher) : BaseRule() {
  var server = MockWebServer()

  override fun apply(base: Statement, description: Description): Statement {
    return object : Statement() {
      override fun evaluate() {
        try {
          server.dispatcher = responseDispatcher
          val sslSocketFactory = getSSLSocketFactory()
          server.useHttps(sslSocketFactory, false)
          server.start(port)
          base.evaluate()
          server.shutdown()
        } catch (e: Exception) {
          e.printStackTrace()
        }
      }
    }
  }

  /**
   * Generate [SSLSocketFactory] to support for self-signed (local) SSL certificates.
   * Based on  http://developer.android.com/training/articles/security-ssl.html#UnknownCa
   *
   * https://github.com/square/okhttp/tree/master/okhttp-tls
   * https://github.com/square/okhttp/blob/master/samples/guide/src/main/java/okhttp3/recipes/HttpsServer.java
   */
  private fun getSSLSocketFactory(): SSLSocketFactory {
    val rootCertificate = HeldCertificate.Builder()
        .keyPair(prepareKeyPairFromResources("ssl/ca_private_key.der", "ssl/ca_public_key.der"))
        .build()

    val localhost: String = InetAddress.getByName("localhost").canonicalHostName
    val localhostCertificate = HeldCertificate.Builder()
        .addSubjectAlternativeName(localhost)
        .signedBy(rootCertificate)
        .rsa2048()
        .keyPair(prepareKeyPairFromResources("ssl/mock_web_server_private_key.der", "ssl/mock_web_server_public_key.der"))
        .build()
    val serverCertificates = HandshakeCertificates.Builder()
        .heldCertificate(localhostCertificate)
        .build()
    return serverCertificates.sslSocketFactory()
  }

  private fun prepareKeyPairFromResources(prvKeyResId: String, pubKeyResId: String): KeyPair {
    val keyFactory = KeyFactory.getInstance("RSA")
    val prvKeyRaw: ByteArray = TestGeneralUtil.readObjectFromResourcesAsByteArray(prvKeyResId)
    val pkcS8EncodedKeySpec = PKCS8EncodedKeySpec(prvKeyRaw)
    val privateKey = keyFactory.generatePrivate(pkcS8EncodedKeySpec)
    val pubKeyRaw: ByteArray = TestGeneralUtil.readObjectFromResourcesAsByteArray(pubKeyResId)
    val x509EncodedKeySpec = X509EncodedKeySpec(pubKeyRaw)
    val publicKey = keyFactory.generatePublic(x509EncodedKeySpec)
    return KeyPair(publicKey, privateKey)
  }
}