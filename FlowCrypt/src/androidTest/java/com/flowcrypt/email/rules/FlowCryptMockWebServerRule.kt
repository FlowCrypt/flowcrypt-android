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
 * @author Denys Bondarenko
 */
class FlowCryptMockWebServerRule(val port: Int, val responseDispatcher: Dispatcher) : BaseRule() {
  override fun execute() {}

  override fun apply(base: Statement, description: Description): Statement {
    return object : Statement() {
      override fun evaluate() {
        val server = MockWebServer()
        try {
          server.dispatcher = responseDispatcher
          val sslSocketFactory = getSSLSocketFactory()
          server.useHttps(sslSocketFactory, false)
          server.start(port)
          base.evaluate()
        } finally {
          server.shutdown()
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
        .readResourceAsString("ssl/server_combined.pem")
    )

    val serverHandshakeCertificates: HandshakeCertificates = HandshakeCertificates.Builder()
      .heldCertificate(serverCertificate)
      .build()

    return serverHandshakeCertificates.sslSocketFactory()
  }
}
