/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors:
 * Ivan Pizhenko
 * DenBond7
 */

package com.flowcrypt.email.api.email

import com.flowcrypt.email.api.wkd.WkdClient
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
@Ignore(
  "Should be rewritten to don't use a real server." +
      "Sometimes https://openpgpkey.flowcrypt.com/.well-known/openpgpkey/flowcrypt.com/policy" +
      "is down and tests fail"
)
class WkdClientTest {
  @Test
  fun existingEmailTest() = runBlocking {
    val keys =
      WkdClient.lookupEmail(RuntimeEnvironment.getApplication(), "human@flowcrypt.com")
    assertTrue("Key not found", keys != null)
    assertTrue("There are no keys in the key collection", keys!!.keyRings.hasNext())
  }

  @Test
  fun nonExistingEmailTest1() = runBlocking {
    val keys = WkdClient.lookupEmail(
      RuntimeEnvironment.getApplication(),
      "no.such.email.for.sure@flowcrypt.com"
    )
    assertTrue("Key found for non-existing email", keys == null)
  }

  @Test
  fun nonExistingEmailTest2() = runBlocking {
    val keys = WkdClient.lookupEmail(RuntimeEnvironment.getApplication(), "doesnotexist@google.com")
    assertTrue("Key found for non-existing email", keys == null)
  }

  @Test
  fun nonExistingDomainTest() = runBlocking {
    val keys = WkdClient.lookupEmail(
      RuntimeEnvironment.getApplication(),
      "doesnotexist@thisdomaindoesnotexist.test"
    )
    assertTrue("Key found for non-existing email", keys == null)
  }
}

