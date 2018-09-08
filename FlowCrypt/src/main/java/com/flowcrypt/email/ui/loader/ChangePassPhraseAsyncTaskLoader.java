/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.loader;

import android.content.ContentProviderResult;
import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v4.content.AsyncTaskLoader;

import com.flowcrypt.email.database.dao.KeysDao;
import com.flowcrypt.email.database.dao.source.AccountDao;
import com.flowcrypt.email.database.dao.source.KeysDaoSource;
import com.flowcrypt.email.database.dao.source.UserIdEmailsKeysDaoSource;
import com.flowcrypt.email.js.Js;
import com.flowcrypt.email.js.PgpKey;
import com.flowcrypt.email.js.PgpKeyInfo;
import com.flowcrypt.email.model.results.LoaderResult;
import com.flowcrypt.email.security.KeyStoreCryptoManager;
import com.flowcrypt.email.security.SecurityStorageConnector;
import com.flowcrypt.email.util.exception.ExceptionUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * This loader can be used for changing a pass phrase of private keys of some account.
 *
 * @author Denis Bondarenko
 * Date: 06.08.2018
 * Time: 9:25
 * E-mail: DenBond7@gmail.com
 */
public class ChangePassPhraseAsyncTaskLoader extends AsyncTaskLoader<LoaderResult> {

    private final String newPassphrase;
    private final AccountDao accountDao;
    private boolean isActionStarted;
    private LoaderResult data;

    public ChangePassPhraseAsyncTaskLoader(Context context, AccountDao accountDao, String newPassphrase) {
        super(context);
        this.accountDao = accountDao;
        this.newPassphrase = newPassphrase;
    }

    @Override
    public void onStartLoading() {
        if (data != null) {
            deliverResult(data);
        } else {
            if (!isActionStarted) {
                forceLoad();
            }
        }
    }

    @Override
    public LoaderResult loadInBackground() {
        isActionStarted = true;
        try {
            Js js = new Js(getContext(), new SecurityStorageConnector(getContext()));

            List<String> longIdListOfAccountPrivateKeys = new UserIdEmailsKeysDaoSource().getLongIdsByEmail
                    (getContext(), accountDao.getEmail());

            PgpKeyInfo[] pgpKeyInfoArray = js.getStorageConnector().getFilteredPgpPrivateKeys
                    (longIdListOfAccountPrivateKeys.toArray(new String[0]));

            if (pgpKeyInfoArray == null || pgpKeyInfoArray.length == 0) {
                throw new IllegalArgumentException("There are no private keys for " + accountDao.getEmail());
            }

            KeyStoreCryptoManager keyStoreCryptoManager = new KeyStoreCryptoManager(getContext());
            List<KeysDao> keysDaoList = new ArrayList<>();

            for (PgpKeyInfo pgpKeyInfo : pgpKeyInfoArray) {
                PgpKey pgpKey = js.crypto_key_read(pgpKeyInfo.getPrivate());
                keysDaoList.add(KeysDao.generateKeysDao(keyStoreCryptoManager, pgpKey, newPassphrase));
            }

            ContentProviderResult[] contentProviderResults = new KeysDaoSource().updateKeys(getContext(), keysDaoList);

            for (ContentProviderResult contentProviderResult : contentProviderResults) {
                if (contentProviderResult.count < 1) {
                    throw new IllegalArgumentException("An error occurred when we tried update " +
                            contentProviderResult.uri);
                }
            }

            return new LoaderResult(true, null);
        } catch (Exception e) {
            e.printStackTrace();
            ExceptionUtil.handleError(e);
            return new LoaderResult(null, e);
        }
    }

    @Override
    public void deliverResult(@Nullable LoaderResult data) {
        this.data = data;
        super.deliverResult(data);
    }
}
