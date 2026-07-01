/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.util

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.flowcrypt.email.BuildConfig
import com.flowcrypt.email.Constants
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, minSdk = BuildConfig.MIN_SDK_VERSION)
class OutgoingAttachmentUriValidatorTest {
  @get:Rule
  val temporaryFolder: TemporaryFolder = TemporaryFolder()

  private val context: Context
    get() = ApplicationProvider.getApplicationContext()

  @Test
  fun rejectFileUriPointingToPrivateDatabasePath() {
    val uri = Uri.parse("file:///data/data/${context.packageName}/databases/flowcrypt.db")

    assertThrows(IllegalArgumentException::class.java) {
      OutgoingAttachmentUriValidator.requireAllowedUri(context, uri)
    }
  }

  @Test
  fun rejectFileUriOutsideFlowCryptCache() {
    val externalFile = temporaryFolder.newFile("foreign.txt")

    assertThrows(IllegalArgumentException::class.java) {
      OutgoingAttachmentUriValidator.requireAllowedUri(context, Uri.fromFile(externalFile))
    }
  }

  @Test
  fun allowFileUriInsideDraftCache() {
    val draftDir = java.io.File(context.cacheDir, Constants.DRAFT_CACHE_DIR).apply {
      mkdirs()
    }
    val stagedAttachment = java.io.File(draftDir, "allowed.txt").apply {
      writeText("safe")
    }

    OutgoingAttachmentUriValidator.requireAllowedUri(context, Uri.fromFile(stagedAttachment))
  }
}
