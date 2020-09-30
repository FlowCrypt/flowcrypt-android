/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.rules

import android.content.ContentValues
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import androidx.test.runner.screenshot.BasicScreenCaptureProcessor
import androidx.test.runner.screenshot.ScreenCapture
import org.apache.commons.io.FilenameUtils
import java.io.BufferedOutputStream
import java.util.*

/**
 * @author Denis Bondarenko
 *         Date: 9/28/20
 *         Time: 8:32 AM
 *         E-mail: DenBond7@gmail.com
 */
class CustomScreenCaptureProcessor : BasicScreenCaptureProcessor() {
  override fun process(capture: ScreenCapture?): String {
    capture ?: return ""
    try {
      val context = getInstrumentation().targetContext
      var filename = if (capture.name == null) defaultFilename else getFilename(capture.name)
      filename += "." + capture.format.toString().toLowerCase()

      val contentResolver = context.contentResolver
      val fileExtension = FilenameUtils.getExtension(filename).toLowerCase(Locale.getDefault())
      val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension)

      val contentValues = ContentValues().apply {
        put(MediaStore.DownloadColumns.DISPLAY_NAME, filename)
        put(MediaStore.DownloadColumns.MIME_TYPE, mimeType)
      }

      val imageUri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI,
          contentValues) ?: return ""
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