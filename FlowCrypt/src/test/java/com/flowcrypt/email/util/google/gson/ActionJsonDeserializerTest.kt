/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.util.google.gson

import com.flowcrypt.email.service.actionqueue.actions.Action
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * @author Denys Bondarenko
 */
class ActionJsonDeserializerTest {

  @Test
  fun testDeserializeUnknownTypeAsNone() {
    val someJson = """
      {
         "id":"0",
         "email":"0",
         "version":"0",
         "actionType":"UNKNOWN"
      }
    """.trimIndent()
    val gson: Gson =
      GsonBuilder().registerTypeAdapter(Action::class.java, ActionJsonDeserializer()).create()
    val action = gson.fromJson(someJson, Action::class.java)
    assertEquals(Action.Type.NONE, action.type)
  }
}
