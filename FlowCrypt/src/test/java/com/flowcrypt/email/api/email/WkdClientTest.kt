/*
 * © 2021-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors:
 *   Ivan Pizhenko
 */

package com.flowcrypt.email.api.email

import com.flowcrypt.email.api.wkd.WkdClient
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.Test

class WkdClientTest {
  @Test
  fun existingEmailTest() {
    /*val keys = WkdClient.lookupEmail("human@flowcrypt.com")
    assertTrue("Key not found", keys != null)
    assertTrue("There are no keys in the key collection", keys!!.keyRings.hasNext())*/
  }

  @Test
  fun nonExistingEmailTest1() {
    /*val keys = WkdClient.lookupEmail("no.such.email.for.sure@flowcrypt.com")
    assertTrue("Key found for non-existing email", keys == null)*/
  }

  @Test
  fun nonExistingEmailTest2() {
    /*val keys = WkdClient.lookupEmail("doesnotexist@google.com")
    assertTrue("Key found for non-existing email", keys == null)*/
  }

  @Test
  fun nonExistingDomainTest() {
    /*val keys = WkdClient.lookupEmail("doesnotexist@thisdomaindoesnotexist.test")
    assertTrue("Key found for non-existing email", keys == null)*/
  }

  @Test
  fun requestTimeoutTest() {
    val mockWebServer = MockWebServer()
    mockWebServer.dispatcher = object: Dispatcher() {
      private val sleepTimeout = (WkdClient.DEFAULT_REQUEST_TIMEOUT + 2) * 1000
      override fun dispatch(request: RecordedRequest): MockResponse {
        Thread.sleep(sleepTimeout)
        return MockResponse().setResponseCode(200)
      }
    }
    mockWebServer.start()
    val port = mockWebServer.port
    mockWebServer.use {
      /*val keys = WkdClient.lookupEmail(email = "user@localhost", wkdPort = port, useHttps = false)
      assertTrue("Key found for non-existing email", keys == null)*/
    }
  }
}

