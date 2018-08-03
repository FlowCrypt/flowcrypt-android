/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.loader;

import android.content.Context;
import android.net.Uri;
import android.support.v4.content.AsyncTaskLoader;

import com.flowcrypt.email.database.dao.source.AccountDao;
import com.flowcrypt.email.database.dao.source.UserIdEmailsKeysDaoSource;
import com.flowcrypt.email.js.Js;
import com.flowcrypt.email.js.PgpKey;
import com.flowcrypt.email.js.PgpKeyInfo;
import com.flowcrypt.email.model.results.LoaderResult;
import com.flowcrypt.email.security.SecurityStorageConnector;
import com.flowcrypt.email.util.GeneralUtil;
import com.flowcrypt.email.util.exception.ExceptionUtil;

import java.util.List;

/**
 * This loader tries to save the backup of the private key as a file.
 * <p>
 * Return true if the key saved, false otherwise;
 *
 * @author DenBond7
 *         Date: 26.07.2017
 *         Time: 13:18
 *         E-mail: DenBond7@gmail.com
 */

public class SavePrivateKeyAsFileAsyncTaskLoader extends AsyncTaskLoader<LoaderResult> {
    private Uri destinationUri;
    private AccountDao accountDao;

    public SavePrivateKeyAsFileAsyncTaskLoader(Context context, AccountDao accountDao, Uri destinationUri) {
        super(context);
        this.accountDao = accountDao;
        this.destinationUri = destinationUri;
        onContentChanged();
    }

    @Override
    public LoaderResult loadInBackground() {
        try {
            StringBuilder armoredPrivateKeysBackupStringBuilder = new StringBuilder();

            SecurityStorageConnector securityStorageConnector = new SecurityStorageConnector(getContext());
            Js js = new Js(getContext(), securityStorageConnector);

            List<String> longIdListOfAccountPrivateKeys = new UserIdEmailsKeysDaoSource().getLongIdsByEmail
                    (getContext(), accountDao.getEmail());

            PgpKeyInfo[] pgpKeyInfoArray = securityStorageConnector.getFilteredPgpPrivateKeys
                    (longIdListOfAccountPrivateKeys.toArray(new String[0]));

            for (int i = 0; i < pgpKeyInfoArray.length; i++) {
                PgpKeyInfo pgpKeyInfo = pgpKeyInfoArray[i];
                PgpKey pgpKey = js.crypto_key_read(pgpKeyInfo.getPrivate());
                pgpKey.encrypt(securityStorageConnector.getPassphrase(pgpKeyInfo.getLongid()));
                armoredPrivateKeysBackupStringBuilder.append(i > 0 ? "\n" + pgpKey.armor() : pgpKey.armor());
            }


            return new LoaderResult(GeneralUtil.writeFileFromStringToUri(getContext(),
                    destinationUri, armoredPrivateKeysBackupStringBuilder.toString()) > 0, null);
        } catch (Exception e) {
            e.printStackTrace();
            ExceptionUtil.handleError(e);
            return new LoaderResult(null, e);
        }
    }

    @Override
    public void onStartLoading() {
        if (takeContentChanged()) {
            forceLoad();
        }
    }

    @Override
    public void onStopLoading() {
        cancelLoad();
    }
}
