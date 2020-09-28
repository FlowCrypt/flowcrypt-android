/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.rules

import android.os.Environment
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import androidx.test.runner.screenshot.BasicScreenCaptureProcessor
import java.io.File

/**
 * @author Denis Bondarenko
 *         Date: 9/28/20
 *         Time: 8:32 AM
 *         E-mail: DenBond7@gmail.com
 */
class CustomScreenCaptureProcessor : BasicScreenCaptureProcessor() {
  init {
    mDefaultScreenshotPath = getNewFilename()
  }

  private fun getNewFilename(): File? {
    val context = getInstrumentation().targetContext.applicationContext
    return context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
  }
}