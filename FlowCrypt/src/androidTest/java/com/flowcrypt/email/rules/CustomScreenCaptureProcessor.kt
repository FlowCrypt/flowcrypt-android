/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.rules

import android.content.ContentValues
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import androidx.test.runner.screenshot.BasicScreenCaptureProcessor
import androidx.test.runner.screenshot.ScreenCapture
import org.apache.commons.io.FilenameUtils
import java.io.BufferedOutputStream

/**
 * @author Denys Bondarenko
 */
class CustomScreenCaptureProcessor : BasicScreenCaptureProcessor() {
  override fun process(capture: ScreenCapture?): String {
    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) return super.process(capture)

    capture ?: return ""
    try {
      val context = getInstrumentation().targetContext
      var filename = if (capture.name == null) defaultFilename else getFilename(capture.name)
      filename += "." + capture.format.toString().lowercase()

      val contentResolver = context.contentResolver
      val fileExtension = FilenameUtils.getExtension(filename).lowercase()
      val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension)

      val contentValues = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, filename)
        put(MediaStore.Images.Media.MIME_TYPE, mimeType)
        put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/screenshots")
      }

      val imageUri = contentResolver.insert(
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        contentValues
      ) ?: return ""
      val outputStream = contentResolver.openOutputStream(imageUri) ?: return ""
      BufferedOutputStream(outputStream).use { out ->
        capture.bitmap.compress(capture.format, 100, out)
        out.flush()
      }
      return filename!!
    } catch (e: Exception) {
      e.printStackTrace()
      return ""
    }
  }
}
