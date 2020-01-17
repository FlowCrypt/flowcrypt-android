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
        server.setDispatcher(responseDispatcher)
        //todo-DenBond7 need to think about this server.useHttps(SslContextBuilder.localhost()
        // .getSocketFactory(), false);
        server.start(port)
        base.evaluate()
        server.shutdown()
      }
    }
  }
}