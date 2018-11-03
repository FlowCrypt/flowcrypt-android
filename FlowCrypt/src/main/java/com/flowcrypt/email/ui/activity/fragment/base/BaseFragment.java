/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment.base;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.flowcrypt.email.R;
import com.flowcrypt.email.model.results.LoaderResult;
import com.flowcrypt.email.ui.activity.base.BaseActivity;
import com.flowcrypt.email.util.UIUtil;

/**
 * The base fragment class.
 *
 * @author DenBond7
 * Date: 27.04.2017
 * Time: 15:39
 * E-mail: DenBond7@gmail.com
 */

public abstract class BaseFragment extends Fragment implements LoaderManager.LoaderCallbacks<LoaderResult> {

  private boolean isBackPressedEnable = true;
  private Snackbar snackbar;

  @Override
  public Loader<LoaderResult> onCreateLoader(int id, Bundle args) {
    return null;
  }

  @Override
  public void onLoadFinished(Loader<LoaderResult> loader, LoaderResult loaderResult) {
    handleLoaderResult(loader != null ? loader.getId() : -1, loaderResult);
  }

  @Override
  public void onLoaderReset(Loader<LoaderResult> loader) {

  }

  /**
   * This method handles a success result of some loader.
   *
   * @param loaderId The loader id.
   * @param result   The object which contains information about the loader results
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

  public void setSupportActionBarTitle(String title) {
    if (((AppCompatActivity) getActivity()).getSupportActionBar() != null) {
      ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(title);
    }
  }

  /**
   * This method returns information about an availability of a "back press action" at the
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

  /**
   * Show information as Snackbar.
   *
   * @param view        The view to find a parent from.
   * @param messageText The text to show.  Can be formatted text.
   */
  public void showInfoSnackbar(View view, String messageText) {
    showInfoSnackbar(view, messageText, Snackbar.LENGTH_INDEFINITE);
  }

  /**
   * Show information as Snackbar.
   *
   * @param view        The view to find a parent from.
   * @param messageText The text to show.  Can be formatted text.
   * @param duration    How long to display the message.
   */
  public void showInfoSnackbar(View view, String messageText, int duration) {
    snackbar = Snackbar.make(view, messageText, duration)
        .setAction(android.R.string.ok, new View.OnClickListener() {
          @Override
          public void onClick(View v) {
          }
        });
    snackbar.show();
  }

  /**
   * Show some information as Snackbar with custom message, action button mame and listener.
   *
   * @param view            he view to find a parent from
   * @param messageText     The text to show.  Can be formatted text
   * @param buttonName      The text of the Snackbar button
   * @param onClickListener The Snackbar button click listener.
   */
  public void showSnackbar(View view, String messageText, String buttonName,
                           @NonNull View.OnClickListener onClickListener) {
    showSnackbar(view, messageText, buttonName, Snackbar.LENGTH_INDEFINITE, onClickListener);
  }

  /**
   * Show some information as Snackbar with custom message, action button mame and listener.
   *
   * @param view            he view to find a parent from
   * @param messageText     The text to show.  Can be formatted text
   * @param buttonName      The text of the Snackbar button
   * @param duration        How long to display the message.
   * @param onClickListener The Snackbar button click listener.
   */
  public void showSnackbar(View view, String messageText, String buttonName, int duration,
                           @NonNull View.OnClickListener onClickListener) {
    snackbar = Snackbar.make(view, messageText, duration)
        .setAction(buttonName, onClickListener);
    snackbar.show();
  }

  public Snackbar getSnackBar() {
    return snackbar;
  }

  public void dismissCurrentSnackBar() {
    if (getSnackBar() != null) {
      getSnackBar().dismiss();
    }
  }

  public BaseActivity getBaseActivity() {
    return (BaseActivity) getActivity();
  }

  protected void handleLoaderResult(int loaderId, LoaderResult loaderResult) {
    if (loaderResult != null) {
      if (loaderResult.getResult() != null) {
        handleSuccessLoaderResult(loaderId, loaderResult.getResult());
      } else if (loaderResult.getException() != null) {
        handleFailureLoaderResult(loaderId, loaderResult.getException());
      } else {
        UIUtil.showInfoSnackbar(getView(), getString(R.string.unknown_error));
      }
    } else {
      UIUtil.showInfoSnackbar(getView(), getString(R.string.unknown_error));
    }
  }
}
