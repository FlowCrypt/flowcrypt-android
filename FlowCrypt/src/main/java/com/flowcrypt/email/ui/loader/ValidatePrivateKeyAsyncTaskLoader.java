/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (tom@cryptup.org).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.loader;

import android.content.Context;
import android.net.Uri;
import android.support.v4.content.AsyncTaskLoader;
import android.text.TextUtils;

import com.flowcrypt.email.js.Js;
import com.flowcrypt.email.model.PrivateKeyDetails;
import com.flowcrypt.email.model.results.LoaderResult;
import com.flowcrypt.email.util.GeneralUtil;

/**
 * This loader does the work of checking a received private key.
 *
 * @author Denis Bondarenko
 *         Date: 25.07.2017
 *         Time: 12:09
 *         E-mail: DenBond7@gmail.com
 */

public class ValidatePrivateKeyAsyncTaskLoader extends AsyncTaskLoader<LoaderResult> {
    /**
     * Max size of a private key is 256k.
     */
    private static final int MAX_SIZE_IN_BYTES = 256 * 1024;
    private PrivateKeyDetails privateKeyDetails;
    private boolean isCheckSizeEnable;

    public ValidatePrivateKeyAsyncTaskLoader(Context context, PrivateKeyDetails privateKeyDetails,
                                             boolean isCheckSizeEnable) {
        super(context);
        this.privateKeyDetails = privateKeyDetails;
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
            if (privateKeyDetails != null) {
                String armoredPrivateKey = null;
                switch (privateKeyDetails.getType()) {
                    case FILE:
                        if (isCheckSizeEnable && isPrivateKeyTooBig(privateKeyDetails.getUri())) {
                            return new LoaderResult(null,
                                    new IllegalArgumentException("The private key is too big"));
                        }

                        armoredPrivateKey = GeneralUtil.readFileFromUriToString(getContext(),
                                privateKeyDetails.getUri());
                        break;

                    case CLIPBOARD:
                        armoredPrivateKey = privateKeyDetails.getValue();
                        break;
                }

                if (TextUtils.isEmpty(armoredPrivateKey)) {
                    return new LoaderResult(null,
                            new IllegalArgumentException("The private key cannot be empty"));
                }

                Js js = new Js(getContext(), null);

                return new LoaderResult(js.is_valid_private_key(armoredPrivateKey), null);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return new LoaderResult(null, e);
        }

        return null;
    }

    @Override
    public void onStopLoading() {
        cancelLoad();
    }

    /**
     * Check that the private key size mot bigger then 1 MB.
     *
     * @param fileUri The {@link Uri} of the selected file.
     * @return true if the private key size not bigger then
     * {@link ValidatePrivateKeyAsyncTaskLoader#MAX_SIZE_IN_BYTES}, otherwise false
     */
    private boolean isPrivateKeyTooBig(Uri fileUri) {
        long fileSize = GeneralUtil.getFileSizeFromUri(getContext(), fileUri);
        return fileSize > MAX_SIZE_IN_BYTES;
    }
}
