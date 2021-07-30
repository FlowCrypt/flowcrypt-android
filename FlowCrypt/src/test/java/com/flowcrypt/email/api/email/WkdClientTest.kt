/*
 * © 2021-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors:
 *   Ivan Pizhenko
 */

package com.flowcrypt.email.api.email

import com.flowcrypt.email.api.wkd.WkdClient
import org.junit.Assert.assertTrue
import org.junit.Test

class WkdClientTest {
  @Test
  fun existingEmailTest() {
    val keys = WkdClient.lookupEmail("human@flowcrypt.com")
    assertTrue("Key not found", keys != null)
    assertTrue("There are no keys in the key collection", keys!!.keyRings.hasNext())
  }

  @Test
  fun nonExistingEmailTest() {
    val keys = WkdClient.lookupEmail("no.such.email.for.sure@flowcrypt.com")
    assertTrue("Key found for non-existing email", keys == null)
  }
}
