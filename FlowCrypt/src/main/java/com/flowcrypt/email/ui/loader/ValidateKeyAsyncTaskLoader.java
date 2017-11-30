/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (human@flowcrypt.com).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.loader;

import android.content.Context;
import android.net.Uri;
import android.support.v4.content.AsyncTaskLoader;
import android.text.TextUtils;

import com.flowcrypt.email.js.Js;
import com.flowcrypt.email.js.PgpKey;
import com.flowcrypt.email.model.KeyDetails;
import com.flowcrypt.email.model.ValidateKeyLoaderResult;
import com.flowcrypt.email.model.results.LoaderResult;
import com.flowcrypt.email.util.GeneralUtil;

import org.acra.ACRA;

/**
 * This loader does the work of checking a received key.
 *
 * @author Denis Bondarenko
 *         Date: 25.07.2017
 *         Time: 12:09
 *         E-mail: DenBond7@gmail.com
 */

public class ValidateKeyAsyncTaskLoader extends AsyncTaskLoader<LoaderResult> {
    /**
     * Max size of a key is 256k.
     */
    private static final int MAX_SIZE_IN_BYTES = 256 * 1024;
    private KeyDetails keyDetails;
    private boolean isCheckSizeEnable;

    public ValidateKeyAsyncTaskLoader(Context context,
                                      KeyDetails keyDetails,
                                      boolean isCheckSizeEnable) {
        super(context);
        this.keyDetails = keyDetails;
        this.isCheckSizeEnable = isCheckSizeEnable;
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
            if (keyDetails != null) {
                String armoredKey = null;
                switch (keyDetails.getBornType()) {
                    case FILE:
                        if (isCheckSizeEnable && isKeyTooBig(keyDetails.getUri())) {
                            return new LoaderResult(null,
                                    new IllegalArgumentException("The key is too big"));
                        }

                        armoredKey = GeneralUtil.readFileFromUriToString(getContext(),
                                keyDetails.getUri());
                        break;

                    case CLIPBOARD:
                        armoredKey = keyDetails.getValue();
                        break;
                }

                if (TextUtils.isEmpty(armoredKey)) {
                    return new LoaderResult(null,
                            new IllegalArgumentException("The key cannot be empty"));
                }

                Js js = new Js(getContext(), null);

                String normalizedArmoredKey = js.crypto_key_normalize(armoredKey);
                PgpKey pgpKey = js.crypto_key_read(normalizedArmoredKey);

                boolean isValidKey = js.is_valid_key(pgpKey, keyDetails.isPrivateKey());

                ValidateKeyLoaderResult validateKeyLoaderResult = new ValidateKeyLoaderResult
                        (armoredKey, isValidKey ? pgpKey.getPrimaryUserId() : null, isValidKey);

                return new LoaderResult(validateKeyLoaderResult, null);
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (ACRA.isInitialised()) {
                ACRA.getErrorReporter().handleException(e);
            }
            return new LoaderResult(null, e);
        }

        return null;
    }

    @Override
    public void onStopLoading() {
        cancelLoad();
    }

    /**
     * Check that the key size mot bigger then 1 MB.
     *
     * @param fileUri The {@link Uri} of the selected file.
     * @return true if the key size not bigger then
     * {@link ValidateKeyAsyncTaskLoader#MAX_SIZE_IN_BYTES}, otherwise false
     */
    private boolean isKeyTooBig(Uri fileUri) {
        long fileSize = GeneralUtil.getFileSizeFromUri(getContext(), fileUri);
        return fileSize > MAX_SIZE_IN_BYTES;
    }
}
