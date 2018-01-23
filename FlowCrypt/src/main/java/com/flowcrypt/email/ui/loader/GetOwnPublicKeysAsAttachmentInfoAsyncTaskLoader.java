/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (human@flowcrypt.com).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.loader;

import android.content.Context;
import android.net.Uri;
import android.support.v4.content.AsyncTaskLoader;
import android.util.Log;

import com.flowcrypt.email.Constants;
import com.flowcrypt.email.api.email.model.AttachmentInfo;
import com.flowcrypt.email.js.Js;
import com.flowcrypt.email.js.PgpContact;
import com.flowcrypt.email.js.PgpKey;
import com.flowcrypt.email.js.PgpKeyInfo;
import com.flowcrypt.email.model.results.LoaderResult;
import com.flowcrypt.email.security.SecurityStorageConnector;
import com.flowcrypt.email.util.GeneralUtil;
import com.flowcrypt.email.util.exception.ManualHandledException;

import org.acra.ACRA;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * This loader prepares information about the user private keys as a list of {@link AttachmentInfo} objects.
 *
 * @author Denis Bondarenko
 *         Date: 25.11.2017
 *         Time: 10:13
 *         E-mail: DenBond7@gmail.com
 */

public class GetOwnPublicKeysAsAttachmentInfoAsyncTaskLoader extends AsyncTaskLoader<LoaderResult> {

    private static final String TAG = GetOwnPublicKeysAsAttachmentInfoAsyncTaskLoader.class.getSimpleName();

    public GetOwnPublicKeysAsAttachmentInfoAsyncTaskLoader(Context context) {
        super(context);
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
            List<AttachmentInfo> attachmentInfoList = new ArrayList<>();

            SecurityStorageConnector securityStorageConnector = new SecurityStorageConnector(getContext());
            Js js = new Js(getContext(), securityStorageConnector);

            PgpKeyInfo[] pgpKeyInfoArray = securityStorageConnector.getAllPgpPrivateKeys();

            File pgpCacheDirectory = new File(getContext().getCacheDir(), Constants.PGP_CACHE_DIR);
            if (!pgpCacheDirectory.exists()) {
                if (!pgpCacheDirectory.mkdir()) {
                    Log.d(TAG, "Create cache directory " + pgpCacheDirectory.getName() + " filed!");
                }
            }

            for (PgpKeyInfo pgpKeyInfo : pgpKeyInfoArray) {
                PgpKey pgpKey = js.crypto_key_read(pgpKeyInfo.getPrivate());
                if (pgpKey != null) {
                    PgpKey publicKey = pgpKey.toPublic();
                    if (publicKey != null) {
                        PgpContact primaryUserId = pgpKey.getPrimaryUserId();
                        if (primaryUserId != null) {
                            String fileName = "0x" + publicKey.getLongid().toUpperCase() + ".asc";
                            String publicKeyValue = publicKey.armor();
                            File publicKeyFile = new File(pgpCacheDirectory, fileName);
                            Uri destinationUri = Uri.fromFile(publicKeyFile);

                            if (GeneralUtil.writeFileFromStringToUri(getContext(), destinationUri,
                                    publicKeyValue) > 0) {
                                AttachmentInfo attachmentInfo = new AttachmentInfo();

                                attachmentInfo.setName(fileName);
                                attachmentInfo.setEncodedSize(publicKeyFile.length());
                                attachmentInfo.setType(Constants.MIME_TYPE_PGP_KEY);
                                attachmentInfo.setUri(destinationUri);
                                attachmentInfo.setEmail(primaryUserId.getEmail());
                                attachmentInfoList.add(attachmentInfo);
                            }
                        }
                    }
                }
            }

            return new LoaderResult(attachmentInfoList, null);
        } catch (Exception e) {
            e.printStackTrace();
            if (ACRA.isInitialised()) {
                ACRA.getErrorReporter().handleException(new ManualHandledException(e));
            }
            return new LoaderResult(null, e);
        }
    }

    @Override
    public void onStopLoading() {
        cancelLoad();
    }
}