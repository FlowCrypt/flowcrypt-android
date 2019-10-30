/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.base

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.flowcrypt.email.FlavourSettings
import com.flowcrypt.email.FlavourSettingsImpl
import com.flowcrypt.email.R
import com.flowcrypt.email.api.retrofit.response.base.ApiResponse
import com.flowcrypt.email.api.retrofit.response.base.ApiResult
import com.flowcrypt.email.jetpack.viewmodel.EnterpriseDomainRulesViewModel
import com.flowcrypt.email.security.SecurityUtils
import com.flowcrypt.email.ui.activity.AddNewAccountManuallyActivity
import com.flowcrypt.email.ui.activity.BaseNodeActivity
import com.flowcrypt.email.util.GeneralUtil
import com.flowcrypt.email.util.UIUtil
import com.flowcrypt.email.util.google.GoogleApiClientHelper
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task

/**
 * This activity will be a common point of a sign-in logic.
 *
 * @author Denis Bondarenko
 * Date: 06.10.2017
 * Time: 10:38
 * E-mail: DenBond7@gmail.com
 */

abstract class BaseSignInActivity : BaseNodeActivity(), View.OnClickListener {

  protected lateinit var client: GoogleSignInClient
  protected var isRunSignInWithGmailNeeded: Boolean = false
  protected lateinit var enterpriseDomainRulesViewModel: EnterpriseDomainRulesViewModel
  protected var uuid: String? = null

  abstract val progressView: View?

  @JvmField
  protected var googleSignInAccount: GoogleSignInAccount? = null

  abstract fun onSignSuccess(googleSignInAccount: GoogleSignInAccount?)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    if (savedInstanceState != null) {
      this.googleSignInAccount = savedInstanceState.getParcelable(KEY_CURRENT_GOOGLE_SIGN_IN_ACCOUNT)
      this.uuid = savedInstanceState.getString(KEY_UUID)
    }

    client = GoogleSignIn.getClient(this, GoogleApiClientHelper.generateGoogleSignInOptions())
    initViews()
    setupEnterpriseViewModel()
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    outState.putParcelable(KEY_CURRENT_GOOGLE_SIGN_IN_ACCOUNT, googleSignInAccount)
    outState.putString(KEY_UUID, uuid)
  }

  public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    when (requestCode) {
      REQUEST_CODE_RESOLVE_SIGN_IN_ERROR -> {
        if (resultCode == Activity.RESULT_OK) {
          GoogleApiClientHelper.signInWithGmailUsingOAuth2(this, client, rootView, REQUEST_CODE_SIGN_IN)
        }
      }

      REQUEST_CODE_SIGN_IN -> {
        handleSignInResult(resultCode, GoogleSignIn.getSignedInAccountFromIntent(data))
      }

      REQUEST_CODE_ADD_OTHER_ACCOUNT -> when (resultCode) {
        AddNewAccountManuallyActivity.RESULT_CODE_CONTINUE_WITH_GMAIL -> this.isRunSignInWithGmailNeeded = true
      }

      else -> super.onActivityResult(requestCode, resultCode, data)
    }
  }

  override fun onClick(v: View) {
    when (v.id) {
      R.id.buttonSignInWithGmail ->
        GoogleApiClientHelper.signInWithGmailUsingOAuth2(this, client, rootView, REQUEST_CODE_SIGN_IN)

      R.id.buttonOtherEmailProvider -> if (GeneralUtil.isConnected(this)) {
        startActivityForResult(Intent(this, AddNewAccountManuallyActivity::class.java), REQUEST_CODE_ADD_OTHER_ACCOUNT)
      } else {
        showInfoSnackbar(rootView, getString(R.string.internet_connection_is_not_available))
      }
    }
  }

  private fun initViews() {
    if (findViewById<View>(R.id.buttonSignInWithGmail) != null) {
      findViewById<View>(R.id.buttonSignInWithGmail).setOnClickListener(this)
    }

    if (findViewById<View>(R.id.buttonOtherEmailProvider) != null) {
      findViewById<View>(R.id.buttonOtherEmailProvider).setOnClickListener(this)
    }
  }

  private fun handleSignInResult(resultCode: Int, task: Task<GoogleSignInAccount>) {
    try {
      if (task.isSuccessful) {
        googleSignInAccount = task.getResult(ApiException::class.java)

        if (FlavourSettingsImpl.buildType == FlavourSettings.BuildType.ENTERPRISE) {
          val account = googleSignInAccount?.account?.name ?: return
          val idToken = googleSignInAccount?.idToken ?: return
          uuid = SecurityUtils.generateRandomUUID()
          uuid?.let { enterpriseDomainRulesViewModel.getDomainRules(account, it, idToken) }
        } else {
          onSignSuccess(googleSignInAccount)
        }
      } else {
        val error = task.exception

        if (error is ApiException) {
          throw error
        }

        UIUtil.showInfoSnackbar(rootView, error?.message ?: getString(R.string.unknown_error))
      }
    } catch (e: ApiException) {
      val msg = GoogleSignInStatusCodes.getStatusCodeString(e.statusCode)
      if (resultCode == Activity.RESULT_OK) {
        UIUtil.showInfoSnackbar(rootView, msg)
      } else {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
      }
    }
  }

  private fun setupEnterpriseViewModel() {
    enterpriseDomainRulesViewModel = ViewModelProvider(this).get(EnterpriseDomainRulesViewModel::class.java)
    val observer = Observer<ApiResult<ApiResponse>?> {
      it?.let {
        when (it.status) {
          ApiResult.Status.LOADING -> {
            UIUtil.exchangeViewVisibility(this, true, progressView, rootView)
          }

          ApiResult.Status.SUCCESS -> {
            onSignSuccess(googleSignInAccount)
          }

          ApiResult.Status.ERROR -> {
            UIUtil.exchangeViewVisibility(this, false, progressView, rootView)
            Toast.makeText(this, it.error?.message
                ?: getString(R.string.unknown_error), Toast.LENGTH_SHORT).show()
          }
        }
      }
    }

    enterpriseDomainRulesViewModel.domainRulesLiveData.observe(this, observer)
  }

  companion object {
    const val REQUEST_CODE_SIGN_IN = 10
    const val REQUEST_CODE_ADD_OTHER_ACCOUNT = 11
    const val REQUEST_CODE_RESOLVE_SIGN_IN_ERROR = 12

    private val KEY_CURRENT_GOOGLE_SIGN_IN_ACCOUNT =
        GeneralUtil.generateUniqueExtraKey("KEY_CURRENT_GOOGLE_SIGN_IN_ACCOUNT", BaseSignInActivity::class.java)
    private val KEY_UUID =
        GeneralUtil.generateUniqueExtraKey("KEY_UUID", BaseSignInActivity::class.java)
  }
}
