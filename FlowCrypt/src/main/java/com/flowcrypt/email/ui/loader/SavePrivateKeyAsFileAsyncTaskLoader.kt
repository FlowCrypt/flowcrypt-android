/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.loader

import android.content.Context
import android.net.Uri
import androidx.loader.content.AsyncTaskLoader
import com.flowcrypt.email.database.dao.source.AccountDao
import com.flowcrypt.email.model.results.LoaderResult
import com.flowcrypt.email.security.SecurityUtils
import com.flowcrypt.email.util.GeneralUtil
import com.flowcrypt.email.util.exception.ExceptionUtil

/**
 * This loader tries to save the backup of the private key as a file.
 *
 *
 * Return true if the key saved, false otherwise;
 *
 * @author DenBond7
 * Date: 26.07.2017
 * Time: 13:18
 * E-mail: DenBond7@gmail.com
 */

class SavePrivateKeyAsFileAsyncTaskLoader(context: Context,
                                          private val account: AccountDao,
                                          private val destinationUri: Uri) : AsyncTaskLoader<LoaderResult>(context) {

  init {
    onContentChanged()
  }

  override fun loadInBackground(): LoaderResult? {
    return try {
      val backup = SecurityUtils.genPrivateKeysBackup(context, account)
      val result = GeneralUtil.writeFileFromStringToUri(context, destinationUri, backup) > 0
      LoaderResult(result, null)
    } catch (e: Exception) {
      e.printStackTrace()
      ExceptionUtil.handleError(e)
      LoaderResult(null, e)
    }
  }

  public override fun onStartLoading() {
    if (takeContentChanged()) {
      forceLoad()
    }
  }

  public override fun onStopLoading() {
    cancelLoad()
  }
}
