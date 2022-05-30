/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.jetpack.workmanager

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.workDataOf
import com.flowcrypt.email.database.FlowCryptRoomDatabase
import com.flowcrypt.email.database.entity.RecipientEntity
import kotlinx.coroutines.runBlocking
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * @author Denis Bondarenko
 * Date: 5/30/22
 * Time: 2:36 PM
 * E-mail: DenBond7@gmail.com
 */
@RunWith(AndroidJUnit4::class)
class EmailAndNameWorkerTest {
  private lateinit var context: Context

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
  }

  @Test
  fun testUpdateExistingRecipient() {
    val recipientEmail = "user@flowcrypt.test"
    val recipientName = "Bob Alisa"
    val recipientDao = FlowCryptRoomDatabase.getDatabase(context).recipientDao()
    val worker = TestListenableWorkerBuilder<EmailAndNameWorker>(
      context = context,
      inputData = workDataOf(
        EmailAndNameWorker.EXTRA_KEY_EMAILS to arrayOf(recipientEmail),
        EmailAndNameWorker.EXTRA_KEY_NAMES to arrayOf(recipientName)
      )
    ).build()
    runBlocking {
      recipientDao.insertSuspend(RecipientEntity(email = recipientEmail))
      val existingUserBeforeUpdating = recipientDao.getRecipientByEmail(recipientEmail)
      assertNotNull(existingUserBeforeUpdating)
      //check that a user have no name before updating
      assertNull(existingUserBeforeUpdating?.name)
      val result = worker.doWork()
      assertThat(result, `is`(ListenableWorker.Result.success()))
      //check that a user have some name after updating
      val existingUserAfterUpdating = recipientDao.getRecipientByEmail(recipientEmail)
      assertNotNull(existingUserAfterUpdating)
      assertEquals(recipientName, existingUserAfterUpdating?.name)
    }
  }

  @Test
  fun testCreateNewRecipient() {
    val recipientEmail = "user@flowcrypt.test"
    val recipientName = "Bob Alisa"
    val recipientDao = FlowCryptRoomDatabase.getDatabase(context).recipientDao()
    val worker = TestListenableWorkerBuilder<EmailAndNameWorker>(
      context = context,
      inputData = workDataOf(
        EmailAndNameWorker.EXTRA_KEY_EMAILS to arrayOf(recipientEmail),
        EmailAndNameWorker.EXTRA_KEY_NAMES to arrayOf(recipientName)
      )
    ).build()
    runBlocking {
      //check that a user is not exist
      val existingUserBeforeUpdating = recipientDao.getRecipientByEmail(recipientEmail)
      assertNull(existingUserBeforeUpdating)

      val result = worker.doWork()
      assertThat(result, `is`(ListenableWorker.Result.success()))

      //check that a new user was added
      val existingUserAfterUpdating = recipientDao.getRecipientByEmail(recipientEmail)
      assertNotNull(existingUserAfterUpdating)
      assertEquals(recipientName, existingUserAfterUpdating?.name)
    }
  }
}
