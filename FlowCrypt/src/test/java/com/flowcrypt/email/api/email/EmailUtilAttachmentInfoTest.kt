/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.api.email

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.provider.OpenableColumns
import androidx.test.core.app.ApplicationProvider
import com.flowcrypt.email.BuildConfig
import com.flowcrypt.email.util.AttachmentSizeValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowContentResolver

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, minSdk = BuildConfig.MIN_SDK_VERSION)
class EmailUtilAttachmentInfoTest {
  private val context: Context
    get() = ApplicationProvider.getApplicationContext()

  @Test
  fun getAttInfoFromUriNormalizesNegativeReportedSize() {
    val uri = registerProvider("negative", -1L, "payload.txt")

    val attInfo = requireNotNull(EmailUtil.getAttInfoFromUri(context, uri))

    assertEquals(0L, attInfo.encodedSize)
  }

  @Test
  fun getAttInfoFromUriKeepsWrappedReportedSizeOutOfValidRange() {
    val size = Int.MAX_VALUE.toLong() + 1
    val uri = registerProvider("wrapped", size, "payload.txt")

    val attInfo = requireNotNull(EmailUtil.getAttInfoFromUri(context, uri))

    assertEquals(size, attInfo.encodedSize)
    assertFalse(AttachmentSizeValidator.canAddAttachment(emptyList(), attInfo.encodedSize))
  }

  @Test
  fun getAttInfoFromUriSanitizesProviderSuppliedName() {
    val uri = registerProvider("names", 10L, "../../evil.txt")

    val attInfo = requireNotNull(EmailUtil.getAttInfoFromUri(context, uri))

    assertEquals("evil.txt", attInfo.name)
  }

  private fun registerProvider(tag: String, size: Long, name: String): Uri {
    val authority = "com.flowcrypt.email.test.provider.$tag"
    val provider = object : ContentProvider() {
      override fun onCreate(): Boolean = true

      override fun query(
        uri: Uri,
        projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?
      ): Cursor = MatrixCursor(
        arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE)
      ).apply { addRow(arrayOf<Any?>(name, size)) }

      override fun getType(uri: Uri): String = "application/octet-stream"
      override fun insert(uri: Uri, values: ContentValues?): Uri? = null
      override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int = 0
      override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<String>?
      ): Int = 0
    }
    ShadowContentResolver.registerProviderInternal(authority, provider)
    return Uri.parse("content://$authority/document")
  }
}
