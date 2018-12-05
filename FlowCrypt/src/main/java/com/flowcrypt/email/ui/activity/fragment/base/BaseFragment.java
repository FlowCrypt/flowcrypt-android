/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment.base;

import android.os.Bundle;
import android.view.View;

import com.flowcrypt.email.R;
import com.flowcrypt.email.model.results.LoaderResult;
import com.flowcrypt.email.ui.activity.base.BaseActivity;
import com.flowcrypt.email.util.UIUtil;
import com.google.android.material.snackbar.Snackbar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;

/**
 * The base fragment class.
 *
 * @author DenBond7
 * Date: 27.04.2017
 * Time: 15:39
 * E-mail: DenBond7@gmail.com
 */

public abstract class BaseFragment extends Fragment implements LoaderManager.LoaderCallbacks<LoaderResult> {

  private boolean isBackPressedEnabled = true;
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
  public void onSuccess(int loaderId, Object result) {

  }

  /**
   * This method handles a failure result of some loader. This method contains a base
   * realization of the failure behavior.
   *
   * @param loaderId The loader id.
   * @param e        The exception which happened when loader does it work.
   */
  public void onError(int loaderId, Exception e) {
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
  public boolean isBackPressedEnabled() {
    return isBackPressedEnabled;
  }

  public void setBackPressedEnabled(boolean backPressedEnabled) {
    isBackPressedEnabled = backPressedEnabled;
  }

  /**
   * Show information as Snackbar.
   *
   * @param view    The view to find a parent from.
   * @param msgText The text to show.  Can be formatted text.
   */
  public void showInfoSnackbar(View view, String msgText) {
    showInfoSnackbar(view, msgText, Snackbar.LENGTH_INDEFINITE);
  }

  /**
   * Show information as Snackbar.
   *
   * @param view     The view to find a parent from.
   * @param msgText  The text to show.  Can be formatted text.
   * @param duration How long to display the message.
   */
  public void showInfoSnackbar(View view, String msgText, int duration) {
    snackbar = Snackbar.make(view, msgText, duration)
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
   * @param msgText         The text to show.  Can be formatted text
   * @param btnName         The text of the Snackbar button
   * @param onClickListener The Snackbar button click listener.
   */
  public void showSnackbar(View view, String msgText, String btnName,
                           @NonNull View.OnClickListener onClickListener) {
    showSnackbar(view, msgText, btnName, Snackbar.LENGTH_INDEFINITE, onClickListener);
  }

  /**
   * Show some information as Snackbar with custom message, action button mame and listener.
   *
   * @param view            he view to find a parent from
   * @param msgText         The text to show.  Can be formatted text
   * @param btnName         The text of the Snackbar button
   * @param duration        How long to display the message.
   * @param onClickListener The Snackbar button click listener.
   */
  public void showSnackbar(View view, String msgText, String btnName, int duration,
                           @NonNull View.OnClickListener onClickListener) {
    snackbar = Snackbar.make(view, msgText, duration).setAction(btnName, onClickListener);
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
        onSuccess(loaderId, loaderResult.getResult());
      } else if (loaderResult.getException() != null) {
        onError(loaderId, loaderResult.getException());
      } else {
        UIUtil.showInfoSnackbar(getView(), getString(R.string.unknown_error));
      }
    } else {
      UIUtil.showInfoSnackbar(getView(), getString(R.string.unknown_error));
    }
  }
}
