/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.rules

import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockWebServer
import org.junit.runner.Description
import org.junit.runners.model.Statement


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
          /*
          It would be great to setup SSL here
          https://codelabs.developers.google.com/codelabs/android-network-security-config/index.html#0
          val sslSocketFactory = getSSLSocketFactory()
          server.useHttps(sslSocketFactory, false)*/
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
   * https://github.com/square/okhttp/tree/master/okhttp-tls
   *  https://github.com/square/okhttp/blob/master/samples/guide/src/main/java/okhttp3/recipes/HttpsServer.java
   */
  /*private fun getSSLSocketFactory(): SSLSocketFactory {
    val keyPair = prepareKeyPairFromResources()
    val localhost: String = InetAddress.getByName("localhost").canonicalHostName
    val localhostCertificate = HeldCertificate.Builder()
        .addSubjectAlternativeName(localhost)
        .rsa2048()
        .keyPair(keyPair)
        .build()
    val serverCertificates = HandshakeCertificates.Builder()
        .heldCertificate(localhostCertificate)
        .build()
    return serverCertificates.sslSocketFactory()
  }

  private fun prepareKeyPairFromResources(): KeyPair {
    val keyFactory = KeyFactory.getInstance("RSA")
    val prvKeyRaw: ByteArray = TestGeneralUtil.readObjectFromResourcesAsByteArray("mock_web_server_private_key.der")
    val pkcS8EncodedKeySpec = PKCS8EncodedKeySpec(prvKeyRaw)
    val privateKey = keyFactory.generatePrivate(pkcS8EncodedKeySpec)
    val pubKeyRaw: ByteArray = TestGeneralUtil.readObjectFromResourcesAsByteArray("mock_web_server_public_key.der")
    val x509EncodedKeySpec = X509EncodedKeySpec(pubKeyRaw)
    val publicKey = keyFactory.generatePublic(x509EncodedKeySpec)
    return KeyPair(publicKey, privateKey)
  }*/
}