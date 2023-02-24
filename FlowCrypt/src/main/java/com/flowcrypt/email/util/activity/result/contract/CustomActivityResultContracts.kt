/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.util.activity.result.contract

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContract
import androidx.annotation.CallSuper

/**
 * This class is based on [ActivityResultContracts.CreateDocument()]
 *
 * @author Denys Bondarenko
 */
open class CreateCustomDocument(private val type: String) : ActivityResultContract<String, Uri?>() {
  @CallSuper
  override fun createIntent(context: Context, input: String): Intent {
    return Intent(Intent.ACTION_CREATE_DOCUMENT)
      .addCategory(Intent.CATEGORY_OPENABLE)
      .setType(type)
      .putExtra(Intent.EXTRA_TITLE, input)
  }

  final override fun getSynchronousResult(
    context: Context,
    input: String
  ): SynchronousResult<Uri?>? = null

  final override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
    return intent.takeIf { resultCode == Activity.RESULT_OK }?.data
  }
}
