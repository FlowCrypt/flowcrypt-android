/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.rules

import android.os.Build
import androidx.test.rule.GrantPermissionRule
import com.flowcrypt.email.TestConstants
import org.junit.rules.TestRule
import org.junit.runners.model.Statement

/**
 * @author Denys Bondarenko
 */
object GrantPermissionRuleChooser {
  fun grant(
    vararg permissions: String?,
    androidVersionCode: Int = TestConstants.ANDROID_EMULATOR_VERSION
  ): TestRule {
    val processedPermissions = when {
      androidVersionCode >= Build.VERSION_CODES.TIRAMISU -> permissions
      else -> permissions.toMutableList().apply {
        //we need to remove this permission as unsupported for Android 12 and less
        remove(android.Manifest.permission.POST_NOTIFICATIONS)
      }.toTypedArray()
    }

    return if (processedPermissions.isEmpty()) {
      TestRule { base, _ -> //do nothing
        object : Statement() {
          override fun evaluate() {
            base.evaluate()
          }
        }
      }
    } else {
      GrantPermissionRule.grant(*permissions)
    }
  }
}
