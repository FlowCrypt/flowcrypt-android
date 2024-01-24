/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.rules

import android.content.ContentValues
import android.graphics.Bitmap
import android.os.Environment
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import androidx.test.core.app.canTakeScreenshot
import androidx.test.core.app.takeScreenshot
import androidx.test.platform.app.InstrumentationRegistry
import org.apache.commons.io.FilenameUtils
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import java.io.BufferedOutputStream
import java.io.IOException
import java.util.UUID


/**
 * It will work well on Android > Build.VERSION_CODES.P
 *
 * @author Denys Bondarenko
 */
class ScreenshotTestRule : TestWatcher() {
  override fun failed(e: Throwable?, description: Description?) {
    super.failed(e, description)
    makeScreenshot(description)
  }

  private fun makeScreenshot(description: Description?) {
    try {
      if (canTakeScreenshot()) {
        val filename = description?.testClass?.simpleName +
            "-" +
            description?.methodName +
            "-" +
            UUID.randomUUID() +
            "." +
            Bitmap.CompressFormat.PNG.toString().lowercase()


        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val contentResolver = context.contentResolver
        val fileExtension = FilenameUtils.getExtension(filename).lowercase()
        val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension)

        val contentValues = ContentValues().apply {
          put(MediaStore.Images.Media.DISPLAY_NAME, filename)
          put(MediaStore.Images.Media.MIME_TYPE, mimeType)
          put(
            MediaStore.Images.Media.RELATIVE_PATH,
            Environment.DIRECTORY_PICTURES + "/" + SCREENSHOTS_DIRECTORY
          )
        }

        val imageUri = contentResolver.insert(
          MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
          contentValues
        ) ?: throw IllegalStateException("file $filename was not created!")
        val outputStream = contentResolver.openOutputStream(imageUri)
          ?: throw IllegalStateException("ContentResolver issue")
        BufferedOutputStream(outputStream).use { out ->
          takeScreenshot().compress(Bitmap.CompressFormat.PNG, 100, out)
          out.flush()
        }
      }
    } catch (e: IOException) {
      e.printStackTrace()
    }
  }

  companion object {
    const val SCREENSHOTS_DIRECTORY = "screenshots"
  }
}
