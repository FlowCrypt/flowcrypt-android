/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.ui.base

import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.contrib.RecyclerViewActions.scrollTo
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import com.flowcrypt.email.R
import com.flowcrypt.email.base.BaseTest
import com.flowcrypt.email.database.entity.PublicKeyEntity
import com.flowcrypt.email.database.entity.RecipientEntity
import org.hamcrest.Matchers.allOf
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

  protected fun testSomeContact(email: String, pgpIconVisibility: ViewMatchers.Visibility) {
    onView(withId(R.id.rVRecipients))
      .perform(
        scrollTo<RecyclerView.ViewHolder>(
          allOf(
            hasDescendant(
              allOf(
                withId(R.id.imageViewPgp),
                withEffectiveVisibility(pgpIconVisibility)
              )
            ),
            hasDescendant(ViewMatchers.withText(email)),
          )
        )
      )
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
