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
   * Get [SSLSocketFactory] to support self-signed (local) SSL certificates.
   *
   * https://github.com/square/okhttp/tree/master/okhttp-tls
   * https://github.com/square/okhttp/blob/master/samples/guide/src/main/java/okhttp3/recipes/HttpsServer.java
   */
  private fun getSSLSocketFactory(): SSLSocketFactory {
    val serverCertificate: HeldCertificate = HeldCertificate.decode(
      TestGeneralUtil
        .readResourcesAsString("ssl/server_combined.pem")
    )

    val serverHandshakeCertificates: HandshakeCertificates = HandshakeCertificates.Builder()
      .heldCertificate(serverCertificate)
      .build()

    return serverHandshakeCertificates.sslSocketFactory()
  }

  /**
   * Generate [SSLSocketFactory]  using [HeldCertificate] to support self-signed (local) SSL certificates.
   * After generation combined result can be saved to file via the following command:
   *
   * "server_combined.pem" = serverCertificate.certificatePem() + serverCertificate.privateKeyPkcs8Pem()
   *
   * Later this string can be used to create a new instance of [HeldCertificate] via the
   * following command:
   *
   * val serverCertificate: HeldCertificate = HeldCertificate.decode(getString("server_combined.pem"))
   *
   * https://github.com/square/okhttp/tree/master/okhttp-tls
   * https://github.com/square/okhttp/blob/master/samples/guide/src/main/java/okhttp3/recipes/HttpsServer.java
   */
  private fun genSSLSocketFactory(): SSLSocketFactory {
    val rootCertificate: HeldCertificate = HeldCertificate.Builder()
      .certificateAuthority(1)
      .build()

    val intermediateCertificate: HeldCertificate = HeldCertificate.Builder()
      .certificateAuthority(0)
      .signedBy(rootCertificate)
      .build()

    val localhost: String = InetAddress.getByName("localhost").canonicalHostName
    val serverCertificate: HeldCertificate = HeldCertificate.Builder()
      .addSubjectAlternativeName(localhost)
      .signedBy(intermediateCertificate)
      .build()

    val serverHandshakeCertificates: HandshakeCertificates = HandshakeCertificates.Builder()
      .heldCertificate(serverCertificate)
      .build()

    return serverHandshakeCertificates.sslSocketFactory()
  }
}
