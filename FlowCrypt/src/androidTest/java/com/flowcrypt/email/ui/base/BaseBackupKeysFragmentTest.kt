/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.ui.base

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.net.Uri
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasCategories
import androidx.test.espresso.intent.matcher.IntentMatchers.hasType
import com.flowcrypt.email.Constants
import com.flowcrypt.email.base.BaseTest
import com.flowcrypt.email.junit.annotations.FlowCryptTestSettings
import org.hamcrest.Matchers.allOf
import java.io.File

/**
 * @author Denys Bondarenko
 */
@FlowCryptTestSettings(useIntents = true)
abstract class BaseBackupKeysFragmentTest : BaseTest(), AddAccountToDatabaseRuleInterface {
  protected fun intendingFileChoose(file: File) {
    val resultData = Intent()
    resultData.data = Uri.fromFile(file)
    intending(
      allOf(
        hasAction(Intent.ACTION_CREATE_DOCUMENT),
        hasCategories(setOf(Intent.CATEGORY_OPENABLE)),
        hasType(Constants.MIME_TYPE_PGP_KEY)
      )
    ).respondWith(Instrumentation.ActivityResult(Activity.RESULT_OK, resultData))
  }
}
