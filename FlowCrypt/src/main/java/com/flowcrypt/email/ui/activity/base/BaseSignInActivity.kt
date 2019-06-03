/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.base

import android.content.Intent
import android.os.Bundle
import android.view.View
import com.flowcrypt.email.R
import com.flowcrypt.email.ui.activity.AddNewAccountManuallyActivity
import com.flowcrypt.email.ui.activity.BaseNodeActivity
import com.flowcrypt.email.util.GeneralUtil
import com.flowcrypt.email.util.google.GoogleApiClientHelper
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient

/**
 * This activity will be a common point of a sign-in logic.
 *
 * @author Denis Bondarenko
 * Date: 06.10.2017
 * Time: 10:38
 * E-mail: DenBond7@gmail.com
 */

abstract class BaseSignInActivity : BaseNodeActivity(), View.OnClickListener, GoogleApiClient.OnConnectionFailedListener, GoogleApiClient.ConnectionCallbacks {
  /**
   * The main entry point for Google Play services integration.
   */
  protected lateinit var client: GoogleApiClient
  protected var isRunSignInWithGmailNeeded: Boolean = false

  @JvmField
  protected var sign: GoogleSignInAccount? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    if (savedInstanceState != null) {
      this.sign = savedInstanceState.getParcelable(KEY_CURRENT_GOOGLE_SIGN_IN_ACCOUNT)
    }

    initGoogleApiClient()
    initViews()
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    outState.putParcelable(KEY_CURRENT_GOOGLE_SIGN_IN_ACCOUNT, sign)
  }

  public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    when (requestCode) {
      REQUEST_CODE_ADD_OTHER_ACCOUNT -> when (resultCode) {
        AddNewAccountManuallyActivity.RESULT_CODE_CONTINUE_WITH_GMAIL -> this.isRunSignInWithGmailNeeded = true
      }

      else -> super.onActivityResult(requestCode, resultCode, data)
    }
  }

  override fun onClick(v: View) {
    when (v.id) {
      R.id.buttonSignInWithGmail -> GoogleApiClientHelper.signInWithGmailUsingOAuth2(this, client, rootView, REQUEST_CODE_SIGN_IN)

      R.id.buttonOtherEmailProvider -> if (GeneralUtil.isConnected(this)) {
        startActivityForResult(Intent(this, AddNewAccountManuallyActivity::class.java), REQUEST_CODE_ADD_OTHER_ACCOUNT)
      } else {
        showInfoSnackbar(rootView, getString(R.string.internet_connection_is_not_available))
      }
    }
  }

  override fun onConnected(bundle: Bundle?) {
    if (this.isRunSignInWithGmailNeeded) {
      this.isRunSignInWithGmailNeeded = false
      GoogleApiClientHelper.signInWithGmailUsingOAuth2(this, client, rootView, REQUEST_CODE_SIGN_IN)
    }
  }

  override fun onConnectionSuspended(i: Int) {

  }

  override fun onConnectionFailed(connResult: ConnectionResult) {
    showInfoSnackbar(rootView, connResult.errorMessage)
  }

  protected fun initGoogleApiClient() {
    val googleSignInOptions = GoogleApiClientHelper.generateGoogleSignInOptions()
    client = GoogleApiClientHelper.generateGoogleApiClient(this, this, this, this, googleSignInOptions)
  }

  private fun initViews() {
    if (findViewById<View>(R.id.buttonSignInWithGmail) != null) {
      findViewById<View>(R.id.buttonSignInWithGmail).setOnClickListener(this)
    }

    if (findViewById<View>(R.id.buttonOtherEmailProvider) != null) {
      findViewById<View>(R.id.buttonOtherEmailProvider).setOnClickListener(this)
    }
  }

  companion object {
    protected const val REQUEST_CODE_SIGN_IN = 10
    protected const val REQUEST_CODE_ADD_OTHER_ACCOUNT = 11

    private val KEY_CURRENT_GOOGLE_SIGN_IN_ACCOUNT = GeneralUtil.generateUniqueExtraKey("KEY_CURRENT_GOOGLE_SIGN_IN_ACCOUNT", BaseSignInActivity::class.java)
  }
}
