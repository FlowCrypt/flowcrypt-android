/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (human@flowcrypt.com).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.loader;

import android.content.Context;
import android.net.Uri;
import android.support.v4.content.AsyncTaskLoader;

import com.flowcrypt.email.js.Js;
import com.flowcrypt.email.js.PgpKey;
import com.flowcrypt.email.model.results.LoaderResult;
import com.flowcrypt.email.security.SecurityStorageConnector;
import com.flowcrypt.email.security.SecurityUtils;
import com.flowcrypt.email.security.model.PrivateKeyInfo;
import com.flowcrypt.email.util.GeneralUtil;
import com.flowcrypt.email.util.exception.ManualHandledException;

import org.acra.ACRA;

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

    public SavePrivateKeyAsFileAsyncTaskLoader(Context context, Uri destinationUri) {
        super(context);
        this.destinationUri = destinationUri;
        onContentChanged();
    }

    @Override
    public LoaderResult loadInBackground() {
        try {
            Js js = new Js(getContext(), new SecurityStorageConnector(getContext()));

            PrivateKeyInfo privateKeyInfo = SecurityUtils.getPrivateKeysInfo(getContext()).get(0);
            String decryptedKey = privateKeyInfo.getPgpKeyInfo().getPrivate();
            PgpKey pgpKey = js.crypto_key_read(decryptedKey);
            pgpKey.encrypt(privateKeyInfo.getPassphrase());

            return new LoaderResult(GeneralUtil.writeFileFromStringToUri(getContext(),
                    destinationUri, pgpKey.armor()) > 0, null);
        } catch (Exception e) {
            e.printStackTrace();
            if (ACRA.isInitialised()) {
                ACRA.getErrorReporter().handleException(new ManualHandledException(e));
            }
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
