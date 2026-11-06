/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.jetpack.workmanager

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.testing.TestListenableWorkerBuilder
import com.flowcrypt.email.BuildConfig
import com.flowcrypt.email.Constants
import com.flowcrypt.email.database.FlowCryptRoomDatabase
import com.flowcrypt.email.database.MessageState
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.database.entity.AttachmentEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.File
import java.util.UUID

/**
 * Checks that [DownloadForwardedAttachmentsWorker] validates a forwarded attachment name
 * before any file operation, so a crafted name can't escape the forwarded attachments directory.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, minSdk = BuildConfig.MIN_SDK_VERSION)
class DownloadForwardedAttachmentsWorkerTest {
  private lateinit var context: Context
  private lateinit var worker: DownloadForwardedAttachmentsWorker
  private lateinit var parentDirectory: File
  private var isAttachmentRequested = false

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    worker = TestListenableWorkerBuilder<DownloadForwardedAttachmentsWorker>(context).build()
    parentDirectory = File(
      File(File(context.cacheDir, Constants.ATTACHMENTS_CACHE_DIR), Constants.FORWARDED_ATTACHMENTS_CACHE_DIR),
      UUID.randomUUID().toString()
    ).apply { mkdirs() }
    isAttachmentRequested = false
  }

  @Test
  fun rejectDatabaseJournalTraversalWhenTargetIsAbsent() {
    val name = "../../../../databases/flowcrypt.db-journal"
    val target = File(
      context.getDatabasePath(FlowCryptRoomDatabase.DB_NAME).parentFile,
      "flowcrypt.db-journal"
    ).canonicalFile
    //make sure the crafted name really points to the database directory
    assertEquals(target, File(parentDirectory, name).canonicalFile)
    target.delete()

    val state = loadAttachment(name)

    assertEquals(MessageState.ERROR_ORIGINAL_ATTACHMENT_NOT_FOUND, state)
    assertFalse(isAttachmentRequested)
    assertFalse(target.exists())
    assertTrue(parentDirectory.listFiles().isNullOrEmpty())
  }

  @Test
  fun rejectTraversalWhenTargetExists() {
    val name = "../../escape-marker"
    val target = File(parentDirectory, name).canonicalFile.apply {
      parentFile?.mkdirs()
      writeText(ORIGINAL_CONTENT)
    }

    val state = loadAttachment(name)

    assertEquals(MessageState.ERROR_ORIGINAL_ATTACHMENT_NOT_FOUND, state)
    assertFalse(isAttachmentRequested)
    assertEquals(ORIGINAL_CONTENT, target.readText())
  }

  @Test
  fun downloadAttachmentWithSafeNameInsideDirectory() {
    val state = loadAttachment("photo.jpg")

    assertEquals(MessageState.QUEUED, state)
    assertTrue(isAttachmentRequested)
    assertEquals(PAYLOAD, File(parentDirectory, "photo.jpg").readText())
  }

  private fun loadAttachment(name: String): MessageState = runBlocking {
    worker.loadAttachments(listOf(createForwardedAttachment(name)), parentDirectory) {
      isAttachmentRequested = true
      ByteArrayInputStream(PAYLOAD.toByteArray())
    }
  }

  private fun createForwardedAttachment(name: String) = AttachmentEntity(
    id = null,
    account = "user@flowcrypt.test",
    accountType = AccountEntity.ACCOUNT_TYPE_GOOGLE,
    folder = "Outbox",
    uid = 1,
    name = name,
    encodedSize = PAYLOAD.length.toLong(),
    type = Constants.MIME_TYPE_BINARY_DATA,
    attachmentId = null,
    fileUri = null,
    forwardedFolder = "INBOX",
    forwardedUid = 1,
    decryptWhenForward = false,
    path = "0/1"
  )

  companion object {
    private const val PAYLOAD = "attacker-controlled bytes"
    private const val ORIGINAL_CONTENT = "original content"
  }
}
