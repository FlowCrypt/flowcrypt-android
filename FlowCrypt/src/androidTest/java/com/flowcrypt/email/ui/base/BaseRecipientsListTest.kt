/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.ui.base

import com.flowcrypt.email.base.BaseTest
import com.flowcrypt.email.database.entity.PublicKeyEntity
import com.flowcrypt.email.database.entity.RecipientEntity
import org.junit.Before

/**
 * @author Denys Bondarenko
 */
abstract class BaseRecipientsListTest : BaseTest() {

  @Before
  fun clearContactsFromDatabase() {
    for (email in EMAILS) {
      val contact = roomDatabase.recipientDao().getRecipientByEmail(email) ?: continue
      roomDatabase.recipientDao().delete(contact)
    }
  }

  protected fun addContactsToDatabase() {
    for (email in EMAILS) {
      addContactToDatabase(email)
    }
  }

  protected fun addContactToDatabase(email: String, hasPgp: Boolean = true) {
    roomDatabase.recipientDao().insert(RecipientEntity(email = email))
    if (hasPgp) {
      roomDatabase.pubKeyDao().insert(
        PublicKeyEntity(
          recipient = email,
          fingerprint = "FINGER",
          publicKey = "KEY".toByteArray()
        )
      )
    }
  }

  companion object {
    val EMAILS = arrayOf(
      "contact_0@flowcrypt.test",
      "contact_1@flowcrypt.test",
      "contact_2@flowcrypt.test",
      "contact_3@flowcrypt.test"
    )
  }
}
