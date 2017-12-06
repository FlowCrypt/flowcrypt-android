/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (human@flowcrypt.com).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment.dialog;

import android.support.v4.app.DialogFragment;
import android.support.v4.content.Loader;
import android.widget.Toast;

import com.flowcrypt.email.R;
import com.flowcrypt.email.model.results.LoaderResult;

/**
 * The base dialog fragment.
 *
 * @author Denis Bondarenko
 *         Date: 01.08.2017
 *         Time: 10:04
 *         E-mail: DenBond7@gmail.com
 */

public class BaseDialogFragment extends DialogFragment {

    public void handleLoaderResult(Loader loader, LoaderResult loaderResult) {
        if (loaderResult != null) {
            if (loaderResult.getResult() != null) {
                handleSuccessLoaderResult(loader.getId(), loaderResult.getResult());
            } else if (loaderResult.getException() != null) {
                handleFailureLoaderResult(loader.getId(), loaderResult.getException());
            } else {
                showToast(getString(R.string.unknown_error));
            }
        } else {
            showToast(getString(R.string.unknown_error));
        }
    }

    public void handleFailureLoaderResult(int loaderId, Exception e) {

    }

    public void handleSuccessLoaderResult(int loaderId, Object result) {

    }

    public void showToast(String string) {
        Toast.makeText(getContext(), string, Toast.LENGTH_SHORT).show();
    }
}
