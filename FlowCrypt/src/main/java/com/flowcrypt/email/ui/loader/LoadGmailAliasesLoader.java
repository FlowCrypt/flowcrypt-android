/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.loader;

import android.content.Context;

import com.flowcrypt.email.api.email.gmail.GmailApiHelper;
import com.flowcrypt.email.database.dao.source.AccountAliasesDao;
import com.flowcrypt.email.database.dao.source.AccountDao;
import com.flowcrypt.email.model.results.LoaderResult;
import com.flowcrypt.email.util.exception.ExceptionUtil;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.ListSendAsResponse;
import com.google.api.services.gmail.model.SendAs;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import androidx.loader.content.AsyncTaskLoader;

/**
 * This loader finds and returns Gmail aliases.
 *
 * @author DenBond7
 * Date: 26.10.2017.
 * Time: 12:28.
 * E-mail: DenBond7@gmail.com
 */
public class LoadGmailAliasesLoader extends AsyncTaskLoader<LoaderResult> {

  /**
   * An user account.
   */
  private AccountDao account;

  public LoadGmailAliasesLoader(Context context, AccountDao account) {
    super(context);
    this.account = account;
    onContentChanged();
  }

  @Override
  public void onStartLoading() {
    if (takeContentChanged()) {
      forceLoad();
    }
  }

  @Override
  public LoaderResult loadInBackground() {
    try {
      Gmail mService = GmailApiHelper.generateGmailApiService(getContext(), account);
      ListSendAsResponse aliases = mService.users().settings().sendAs().list(GmailApiHelper.DEFAULT_USER_ID)
          .execute();
      List<AccountAliasesDao> accountAliasesDaoList = new ArrayList<>();
      for (SendAs alias : aliases.getSendAs()) {
        if (alias.getVerificationStatus() != null) {
          AccountAliasesDao accountAliasesDao = new AccountAliasesDao();
          accountAliasesDao.setEmail(account.getEmail());
          accountAliasesDao.setAccountType(account.getAccountType());
          accountAliasesDao.setSendAsEmail(alias.getSendAsEmail());
          accountAliasesDao.setDisplayName(alias.getDisplayName());
          accountAliasesDao.setDefault(alias.getIsDefault() != null && alias.getIsDefault());
          accountAliasesDao.setVerificationStatus(alias.getVerificationStatus());
          accountAliasesDaoList.add(accountAliasesDao);
        }
      }

      return new LoaderResult(accountAliasesDaoList, null);
    } catch (IOException e) {
      e.printStackTrace();
      ExceptionUtil.handleError(e);
      return new LoaderResult(null, e);
    }
  }

  @Override
  public void onStopLoading() {
    cancelLoad();
  }
}
