/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.database.entity

import com.flowcrypt.email.database.entity.LabelEntity.LabelListVisibility
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * @author Denys Bondarenko
 */
class MessageEntityTest {
  @Test
  fun generateColoredLabelsCorrectInboxPositionTest() {
    val labelIds = (1..5).map {
      "Some_Name_$it"
    }.shuffled().toMutableList().apply {
      add("INBOX")
    }

    val labelEntities = labelIds.map {
      LabelEntity(
        email = "email",
        accountType = "accountType",
        name = it,
        isCustom = true,
        alias = it,
        messagesTotal = 0,
        attributes = "",
        labelColor = "labelColor",
        textColor = "textColor",
        labelListVisibility = LabelListVisibility.SHOW,
      )
    }
    val labels = MessageEntity.generateColoredLabels(
      labelIds,
      labelEntities
    )
    assertEquals("Inbox", labels.first().name)
  }
}