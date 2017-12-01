/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (human@flowcrypt.com).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.loader;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.content.AsyncTaskLoader;

import com.flowcrypt.email.database.dao.source.KeysDaoSource;
import com.flowcrypt.email.js.Js;
import com.flowcrypt.email.js.PgpKey;
import com.flowcrypt.email.js.PgpKeyInfo;
import com.flowcrypt.email.model.PrivateKeyModel;
import com.flowcrypt.email.model.results.LoaderResult;
import com.flowcrypt.email.security.SecurityStorageConnector;

import org.acra.ACRA;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * This loader loads information about private keys and create a list of {@link PrivateKeyModel} objects.
 *
 * @author DenBond7
 *         Date: 01.12.2017
 *         Time: 12:18
 *         E-mail: DenBond7@gmail.com
 */

public class PreparePrivateKeyModelListAsyncTaskLoader extends AsyncTaskLoader<LoaderResult> {

    public PreparePrivateKeyModelListAsyncTaskLoader(Context context) {
        super(context);
        onContentChanged();
    }

    @Override
    public LoaderResult loadInBackground() {
        try {
            DateFormat dateFormat = android.text.format.DateFormat.getMediumDateFormat(getContext());
            SecurityStorageConnector securityStorageConnector = new SecurityStorageConnector(getContext());
            Js js = new Js(getContext(), securityStorageConnector);

            List<PrivateKeyModel> privateKeyModelList = new ArrayList<>();

            Cursor cursor = getContext().getContentResolver().query(new KeysDaoSource().getBaseContentUri(), null,
                    null, null, null);

            if (cursor != null) {
                while (cursor.moveToNext()) {
                    String longId = cursor.getString(cursor.getColumnIndex(KeysDaoSource.COL_LONG_ID));
                    PgpKeyInfo keyInfo = securityStorageConnector.getPgpPrivateKey(longId);
                    PgpKey pgpKey = js.crypto_key_read(keyInfo.getPrivate());
                    privateKeyModelList.add(new PrivateKeyModel(pgpKey.getPrimaryUserId().getEmail(),
                            js.mnemonic(longId), dateFormat.format(new Date(pgpKey.getCreated()))));
                }

                cursor.close();
            }

            return new LoaderResult(privateKeyModelList, null);
        } catch (Exception e) {
            e.printStackTrace();
            if (ACRA.isInitialised()) {
                ACRA.getErrorReporter().handleException(e);
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
