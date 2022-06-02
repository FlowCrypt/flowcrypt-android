/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.base

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.net.Uri
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers
import com.flowcrypt.email.Constants
import com.flowcrypt.email.base.BaseTest
import org.hamcrest.Matchers
import java.io.File

/**
 * @author Denis Bondarenko
 *         Date: 6/17/21
 *         Time: 5:26 PM
 *         E-mail: DenBond7@gmail.com
 */
abstract class BaseBackupKeysFragmentTest : BaseTest(), AddAccountToDatabaseRuleInterface {
  override val useIntents: Boolean = true

  protected fun intendingFileChoose(file: File) {
    val resultData = Intent()
    resultData.data = Uri.fromFile(file)
    Intents.intending(
      Matchers.allOf(
        IntentMatchers.hasAction(Intent.ACTION_CREATE_DOCUMENT),
        IntentMatchers.hasCategories(Matchers.hasItem(Matchers.equalTo(Intent.CATEGORY_OPENABLE))),
        IntentMatchers.hasType(Constants.MIME_TYPE_PGP_KEY)
      )
    )
      .respondWith(Instrumentation.ActivityResult(Activity.RESULT_OK, resultData))
  }
}
