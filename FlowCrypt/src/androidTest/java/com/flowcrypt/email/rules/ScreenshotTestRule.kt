/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.rules

import android.graphics.Bitmap.CompressFormat
import androidx.test.runner.screenshot.Screenshot
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import java.io.IOException


/**
 * @author Denys Bondarenko
 */
class ScreenshotTestRule : TestWatcher() {
  override fun failed(e: Throwable?, description: Description?) {
    super.failed(e, description)
    takeScreenshot(description)
  }

  private fun takeScreenshot(description: Description?) {
    val filename = description?.testClass?.simpleName + "-" + description?.methodName
    Screenshot.setScreenshotProcessors(setOf(CustomScreenCaptureProcessor()))
    val capture = Screenshot.capture()
    capture.name = filename
    capture.format = CompressFormat.PNG
    try {
      capture.process()
    } catch (e: IOException) {
      e.printStackTrace()
    }
  }
}
