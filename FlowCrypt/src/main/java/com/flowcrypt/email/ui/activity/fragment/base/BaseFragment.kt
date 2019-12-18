/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment.base

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.loader.app.LoaderManager
import androidx.loader.content.Loader
import com.flowcrypt.email.R
import com.flowcrypt.email.model.results.LoaderResult
import com.flowcrypt.email.ui.activity.base.BaseActivity
import com.flowcrypt.email.util.UIUtil
import com.google.android.material.snackbar.Snackbar

/**
 * The base fragment class.
 *
 * @author DenBond7
 * Date: 27.04.2017
 * Time: 15:39
 * E-mail: DenBond7@gmail.com
 */

abstract class BaseFragment : Fragment(), LoaderManager.LoaderCallbacks<LoaderResult> {

  /**
   * This method returns information about an availability of a "back press action" at the
   * current moment.
   *
   * @return true if a back press action enable at current moment, false otherwise.
   */
  @JvmField
  var isBackPressedEnabled = true
  var snackBar: Snackbar? = null
    private set

  val supportActionBar: ActionBar?
    get() = if (activity is AppCompatActivity) {
      (activity as AppCompatActivity).supportActionBar
    } else
      null

  val baseActivity: BaseActivity
    get() = activity as BaseActivity

  override fun onCreateLoader(id: Int, args: Bundle?): Loader<LoaderResult> {
    return Loader(baseActivity)
  }

  override fun onLoadFinished(loader: Loader<LoaderResult>, loaderResult: LoaderResult) {
    handleLoaderResult(loader.id, loaderResult)
  }

  override fun onLoaderReset(loader: Loader<LoaderResult>) {

  }

  /**
   * This method handles a success result of some loader.
   *
   * @param loaderId The loader id.
   * @param result   The object which contains information about the loader results
   */
  open fun onSuccess(loaderId: Int, result: Any?) {

  }

  /**
   * This method handles a failure result of some loader. This method contains a base
   * realization of the failure behavior.
   *
   * @param loaderId The loader id.
   * @param e        The exception which happened when loader does it work.
   */
  open fun onError(loaderId: Int, e: Exception?) {
    e?.message?.let {
      if (view != null) {
        UIUtil.showInfoSnackbar(view!!, it)
      }
    }
  }

  fun setSupportActionBarTitle(title: String) {
    supportActionBar?.title = title
  }

  /**
   * Show information as Snackbar.
   *
   * @param view     The view to find a parent from.
   * @param msgText  The text to show.  Can be formatted text.
   * @param duration How long to display the message.
   */
  @JvmOverloads
  fun showInfoSnackbar(view: View?, msgText: String, duration: Int = Snackbar.LENGTH_INDEFINITE) {
    view?.let { Snackbar.make(it, msgText, duration).setAction(android.R.string.ok) {}.show() }
  }

  /**
   * Show some information as Snackbar with custom message, action button mame and listener.
   *
   * @param view            he view to find a parent from
   * @param msgText         The text to show.  Can be formatted text
   * @param btnName         The text of the Snackbar button
   * @param onClickListener The Snackbar button click listener.
   */
  fun showSnackbar(view: View, msgText: String, btnName: String, onClickListener: View.OnClickListener) {
    showSnackbar(view, msgText, btnName, Snackbar.LENGTH_INDEFINITE, onClickListener)
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
  fun showSnackbar(view: View, msgText: String, btnName: String, duration: Int, onClickListener: View.OnClickListener) {
    Snackbar.make(view, msgText, duration).setAction(btnName, onClickListener).show()
  }

  fun dismissCurrentSnackBar() {
    snackBar?.dismiss()
  }

  protected fun handleLoaderResult(loaderId: Int, loaderResult: LoaderResult?) {
    if (loaderResult != null) {
      when {
        loaderResult.result != null -> onSuccess(loaderId, loaderResult.result)
        loaderResult.exception != null -> onError(loaderId, loaderResult.exception)
        else -> UIUtil.showInfoSnackbar(view!!, getString(R.string.unknown_error))
      }
    } else {
      UIUtil.showInfoSnackbar(view!!, getString(R.string.unknown_error))
    }
  }
}
