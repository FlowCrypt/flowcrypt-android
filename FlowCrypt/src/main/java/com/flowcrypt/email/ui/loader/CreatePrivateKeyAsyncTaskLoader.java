/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (human@flowcrypt.com).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.loader;

import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;

import com.flowcrypt.email.api.email.gmail.GmailApiHelper;
import com.flowcrypt.email.database.dao.source.AccountDao;
import com.flowcrypt.email.js.Js;
import com.flowcrypt.email.js.PgpContact;
import com.flowcrypt.email.js.PgpKey;
import com.flowcrypt.email.model.results.LoaderResult;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.ListSendAsResponse;
import com.google.api.services.gmail.model.SendAs;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * This loader does job of creating a private key.
 *
 * @author DenBond7
 *         Date: 12.01.2018.
 *         Time: 12:36.
 *         E-mail: DenBond7@gmail.com
 */
public class CreatePrivateKeyAsyncTaskLoader extends AsyncTaskLoader<LoaderResult> {

    private static final int DEFAULT_KEY_SIZE = 2048;

    private final String passphrase;
    private final AccountDao accountDao;

    public CreatePrivateKeyAsyncTaskLoader(Context context, AccountDao accountDao, String passphrase) {
        super(context);
        this.accountDao = accountDao;
        this.passphrase = passphrase;
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
        PgpContact pgpContactMain = new PgpContact(accountDao.getEmail(), accountDao.getDisplayName());
        try {
            PgpContact[] pgpContacts;
            switch (accountDao.getAccountType()) {
                case AccountDao.ACCOUNT_TYPE_GOOGLE:
                    List<PgpContact> pgpContactList = new ArrayList<>();
                    pgpContactList.add(pgpContactMain);
                    Gmail gmail = GmailApiHelper.generateGmailApiService(getContext(), accountDao);
                    ListSendAsResponse aliases = gmail.users().settings().sendAs().list("me").execute();
                    for (SendAs alias : aliases.getSendAs()) {
                        if (alias.getVerificationStatus() != null) {
                            pgpContactList.add(new PgpContact(alias.getSendAsEmail(), alias.getDisplayName()));
                        }
                    }
                    pgpContacts = pgpContactList.toArray(new PgpContact[0]);
                    break;

                default:
                    pgpContacts = new PgpContact[]{pgpContactMain};
                    break;
            }

            PgpKey pgpKey = new Js(getContext(), null).crypto_key_create(pgpContacts, DEFAULT_KEY_SIZE, passphrase);

            if (pgpKey == null) {
                return new LoaderResult(false, new NullPointerException("The generated private key is null!"));
            }

            return new LoaderResult(pgpKey, null);
        } catch (IOException e) {
            e.printStackTrace();
            return new LoaderResult(false, e);
        }
    }

    @Override
    public void onStopLoading() {
        cancelLoad();
    }
}
