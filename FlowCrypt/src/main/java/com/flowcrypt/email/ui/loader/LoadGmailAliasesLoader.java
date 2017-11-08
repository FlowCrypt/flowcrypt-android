/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (human@flowcrypt.com).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.loader;

import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;

import com.flowcrypt.email.api.email.gmail.GmailApiHelper;
import com.flowcrypt.email.database.dao.source.AccountAliasesDao;
import com.flowcrypt.email.database.dao.source.AccountDao;
import com.flowcrypt.email.model.results.LoaderResult;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.ListSendAsResponse;
import com.google.api.services.gmail.model.SendAs;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * This loader finds and returns Gmail aliases.
 *
 * @author DenBond7
 *         Date: 26.10.2017.
 *         Time: 12:28.
 *         E-mail: DenBond7@gmail.com
 */
public class LoadGmailAliasesLoader extends AsyncTaskLoader<LoaderResult> {

    /**
     * An user account.
     */
    private AccountDao accountDao;

    public LoadGmailAliasesLoader(Context context, AccountDao accountDao) {
        super(context);
        this.accountDao = accountDao;
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
            Gmail mService = GmailApiHelper.generateGmailApiService(getContext(), accountDao);
            ListSendAsResponse aliases = mService.users().settings().sendAs().list("me").execute();
            List<AccountAliasesDao> accountAliasesDaoList = new ArrayList<>();
            for (SendAs alias : aliases.getSendAs()) {
                if (alias.getVerificationStatus() != null) {
                    AccountAliasesDao accountAliasesDao = new AccountAliasesDao();
                    accountAliasesDao.setEmail(accountDao.getEmail());
                    accountAliasesDao.setAccountType(accountDao.getAccountType());
                    accountAliasesDao.setSendAsEmail(alias.getSendAsEmail());
                    accountAliasesDao.setDisplayName(alias.getDisplayName());
                    accountAliasesDao.setDefault(alias.getIsDefault());
                    accountAliasesDao.setVerificationStatus(alias.getVerificationStatus());
                    accountAliasesDaoList.add(accountAliasesDao);
                }
            }

            return new LoaderResult(accountAliasesDaoList, null);
        } catch (IOException e) {
            e.printStackTrace();
            return new LoaderResult(null, e);
        }
    }

    @Override
    public void onStopLoading() {
        cancelLoad();
    }
}
