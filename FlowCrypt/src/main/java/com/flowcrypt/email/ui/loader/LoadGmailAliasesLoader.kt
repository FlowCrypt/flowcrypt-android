/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.loader

import android.content.Context
import androidx.loader.content.AsyncTaskLoader
import com.flowcrypt.email.api.email.gmail.GmailApiHelper
import com.flowcrypt.email.database.dao.source.AccountAliases
import com.flowcrypt.email.database.dao.source.AccountDao
import com.flowcrypt.email.model.results.LoaderResult
import com.flowcrypt.email.util.exception.ExceptionUtil
import java.io.IOException
import java.util.*

/**
 * This loader finds and returns Gmail aliases.
 *
 * @author DenBond7
 * Date: 26.10.2017.
 * Time: 12:28.
 * E-mail: DenBond7@gmail.com
 */
class LoadGmailAliasesLoader(context: Context,
                             private val account: AccountDao) : AsyncTaskLoader<LoaderResult>(context) {

  init {
    onContentChanged()
  }

  public override fun onStartLoading() {
    if (takeContentChanged()) {
      forceLoad()
    }
  }

  override fun loadInBackground(): LoaderResult? {
    try {
      val mService = GmailApiHelper.generateGmailApiService(context, account)
      val aliases = mService.users().settings().sendAs().list(GmailApiHelper.DEFAULT_USER_ID).execute()
      val accountAliases = ArrayList<AccountAliases>()
      for (alias in aliases.sendAs) {
        if (alias.verificationStatus != null) {
          val accountAliasesDao = AccountAliases()
          accountAliasesDao.email = account.email
          accountAliasesDao.accountType = account.accountType
          accountAliasesDao.sendAsEmail = alias.sendAsEmail
          accountAliasesDao.displayName = alias.displayName
          accountAliasesDao.isDefault = alias.isDefault != null && alias.isDefault!!
          accountAliasesDao.verificationStatus = alias.verificationStatus
          accountAliases.add(accountAliasesDao)
        }
      }

      return LoaderResult(accountAliases, null)
    } catch (e: IOException) {
      e.printStackTrace()
      ExceptionUtil.handleError(e)
      return LoaderResult(null, e)
    }
  }

  public override fun onStopLoading() {
    cancelLoad()
  }
}
