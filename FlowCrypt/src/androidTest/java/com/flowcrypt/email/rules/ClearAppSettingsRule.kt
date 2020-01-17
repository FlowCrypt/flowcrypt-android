/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.rules

import android.net.Uri
import androidx.test.internal.runner.junit4.statement.UiThreadStatement
import com.flowcrypt.email.api.email.MsgsCacheManager
import com.flowcrypt.email.database.provider.FlowcryptContract
import com.flowcrypt.email.security.KeysStorageImpl
import com.flowcrypt.email.util.FileAndDirectoryUtils
import com.flowcrypt.email.util.SharedPreferencesHelper
import org.junit.runner.Description
import org.junit.runners.model.Statement
import java.io.File
import java.io.IOException

/**
 * The rule which clears the application settings.
 *
 * @author Denis Bondarenko
 * Date: 27.12.2017
 * Time: 11:57
 * E-mail: DenBond7@gmail.com
 */

class ClearAppSettingsRule : BaseRule() {

  override fun apply(base: Statement, description: Description): Statement {
    return object : Statement() {
      override fun evaluate() {
        clearApp()
        base.evaluate()
      }
    }
  }

  /**
   * Clear the all application settings.
   *
   * @throws IOException Different errors can be occurred.
   */
  private fun clearApp() {
    SharedPreferencesHelper.clear(targetContext)
    FileAndDirectoryUtils.cleanDir(targetContext.cacheDir)
    FileAndDirectoryUtils.cleanDir(File(targetContext.filesDir, MsgsCacheManager.CACHE_DIR_NAME))
    targetContext.contentResolver.delete(Uri.parse(FlowcryptContract.AUTHORITY_URI.toString()
        + "/" + FlowcryptContract.ERASE_DATABASE), null, null)
    UiThreadStatement.runOnUiThread { KeysStorageImpl.getInstance(targetContext).refresh(targetContext) }
    Thread.sleep(1000)// Added timeout for a better sync between threads.
  }
}
