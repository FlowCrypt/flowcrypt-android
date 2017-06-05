package com.flowcrypt.email.ui.activity.fragment.base;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;

import com.flowcrypt.email.R;
import com.flowcrypt.email.model.results.LoaderResult;
import com.flowcrypt.email.util.UIUtil;

/**
 * The base fragment class.
 *
 * @author DenBond7
 *         Date: 27.04.2017
 *         Time: 15:39
 *         E-mail: DenBond7@gmail.com
 */

public abstract class BaseFragment extends Fragment implements LoaderManager
        .LoaderCallbacks<LoaderResult> {

    private boolean isBackPressedEnable = true;

    @Override
    public Loader<LoaderResult> onCreateLoader(int id, Bundle args) {
        return null;
    }

    @Override
    public void onLoadFinished(Loader<LoaderResult> loader, LoaderResult loaderResult) {
        if (loaderResult != null) {
            if (loaderResult.getResult() != null) {
                handleSuccessLoaderResult(loader.getId(), loaderResult.getResult());
            } else if (loaderResult.getException() != null) {
                handleFailureLoaderResult(loader.getId(), loaderResult.getException());
            } else {
                UIUtil.showInfoSnackbar(getView(), getString(R.string.unknown_error));
            }
        } else {
            UIUtil.showInfoSnackbar(getView(), getString(R.string.unknown_error));
        }
    }

    @Override
    public void onLoaderReset(Loader<LoaderResult> loader) {

    }

    /**
     * This method handles a success result of some loader.
     *
     * @param loaderId The loader id.
     * @param result   The object which contains an information about the loader results
     */
    public void handleSuccessLoaderResult(int loaderId, Object result) {

    }

    /**
     * This method handles a failure result of some loader. This method contains a base
     * realization of the failure behavior.
     *
     * @param loaderId The loader id.
     * @param e        The exception which happened when loader does it work.
     */
    public void handleFailureLoaderResult(int loaderId, Exception e) {
        UIUtil.showInfoSnackbar(getView(), e.getMessage());
    }

    public ActionBar getSupportActionBar() {
        if (getActivity() instanceof AppCompatActivity) {
            return ((AppCompatActivity) getActivity()).getSupportActionBar();
        } else return null;
    }

    /**
     * This method returns an information about an availability of a "back press action" at the
     * current moment.
     *
     * @return true if a back press action enable at current moment, false otherwise.
     */
    public boolean isBackPressedEnable() {
        return isBackPressedEnable;
    }

    public void setBackPressedEnable(boolean backPressedEnable) {
        isBackPressedEnable = backPressedEnable;
    }
}
