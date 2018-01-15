/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (human@flowcrypt.com).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.loader;

import android.content.Context;
import android.net.Uri;
import android.support.v4.content.AsyncTaskLoader;

import com.flowcrypt.email.api.email.gmail.GmailApiHelper;
import com.flowcrypt.email.database.dao.KeysDao;
import com.flowcrypt.email.database.dao.source.AccountDao;
import com.flowcrypt.email.database.dao.source.KeysDaoSource;
import com.flowcrypt.email.js.Js;
import com.flowcrypt.email.js.PgpContact;
import com.flowcrypt.email.js.PgpKey;
import com.flowcrypt.email.model.KeyDetails;
import com.flowcrypt.email.model.results.LoaderResult;
import com.flowcrypt.email.security.KeyStoreCryptoManager;
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
        try {
            PgpKey pgpKey = createPgpKey();

            if (pgpKey == null) {
                return new LoaderResult(false, new NullPointerException("The generated private key is null!"));
            }

            Uri uri = new KeysDaoSource().addRow(getContext(),
                    KeysDao.generateKeysDao(new KeyStoreCryptoManager(getContext()),
                            new KeyDetails(null, pgpKey.armor(), null,
                                    KeyDetails.Type.NEW, true, pgpKey.getPrimaryUserId()), pgpKey, passphrase));

            if (uri == null) {
                return new LoaderResult(false, new NullPointerException("Cannot save the generated private key"));
            }

            return new LoaderResult(pgpKey, null);
        } catch (Exception e) {
            e.printStackTrace();
            return new LoaderResult(false, e);
        }
    }

    @Override
    public void onStopLoading() {
        cancelLoad();
    }

    /**
     * Create a private PGP key.
     *
     * @return Generated {@link PgpKey}
     * @throws IOException Some exceptions can be throw.
     */
    private PgpKey createPgpKey() throws Exception {
        PgpContact pgpContactMain = new PgpContact(accountDao.getEmail(), accountDao.getDisplayName());
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

        return new Js(getContext(), null).crypto_key_create(pgpContacts, DEFAULT_KEY_SIZE, passphrase);
    }
}
