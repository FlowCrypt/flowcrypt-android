/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.ui.base

import android.util.Base64
import com.flowcrypt.email.base.BaseTest

/**
 * @author Denys Bondarenko
 */
abstract class BaseFeedbackFragmentTest : BaseTest() {
  companion object {
    private const val SCREENSHOT_BASE64 =
      "iVBORw0KGgoAAAANSUhEUgAAABgAAAAYCAQAAABKfvVzAAAA8klEQVQ" +
          "4y8WUsQrCMBCGv076BFLppiK02t2u0ocSEdqO+hIKPoegi7g7F0HxBdTZOBjSpNbYzdz23/+Ru+QS+McaspQR1" +
          "gNmCBlJPWClgLXNNmZBAIw4K+BCBPjMicv2BjkCwZGnsgsET44IBCeaJjAxbFUx0+0uNyO5JyVlb2gPvALocNF" +
          "SGQ4ADpmmXunpe7TYyMRB2t/IQapb3HLbfZlKDTWVqv95rL4VGJTtLltrSTuzpB7XGk13C8Dj8fNY77SrB+5bT" +
          "M0empyso5HTKLcdM8cHIu0Sz4yAgAVj29yuFbCq9x6S6oH7vkL1RIf/+CFeP17HNVfX5IMAAAAASUVORK5CYII="

    val SCREENSHOT_BYTE_ARRAY: ByteArray = Base64.decode(SCREENSHOT_BASE64, Base64.NO_WRAP)
  }
}
