/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest

/**
 * It's a custom realization of a mock HTTP server. Just for testing. DON'T USE IT FOR THE
 * PRODUCTION ENVIRONMENT
 *
 * @author Denis Bondarenko
 *         Date: 10/30/19
 *         Time: 8:50 AM
 *         E-mail: DenBond7@gmail.com
 */
object MockFlowcryptServer {
  private val server = MockWebServer()
  private val dispatcher = object : Dispatcher() {
    override fun dispatch(request: RecordedRequest?): MockResponse {
      println(request)
      if (request?.path.equals("/account/login")) {
        return MockResponse().setResponseCode(200)
            .setBody("{\"registered\":true,\"verified\":true}")
      }

      if (request?.path.equals("/account/get")) {
        return MockResponse().setResponseCode(200)
            .setBody("{\"account\":{\"email\":\"user@all-restrictions.denbond7.com\",\"alias\":null,\"name\":null,\"photo\":null,\"photo_circle\":true,\"web\":null,\"phone\":null,\"intro\":null,\"default_message_expire\":3,\"token\":null},\"domain_org_rules\":{\"flags\":[\"NO_PRV_CREATE\",\"NO_PRV_BACKUP\",\"ENFORCE_ATTESTER_SUBMIT\"]}}")
      }
      return MockResponse().setResponseCode(404)
    }
  }

  fun init() {
    GlobalScope.launch { runServer() }
  }

  private suspend fun runServer() {
    withContext(Dispatchers.Default) {
      server.start(1212)
      server.setDispatcher(dispatcher)
    }
  }
}