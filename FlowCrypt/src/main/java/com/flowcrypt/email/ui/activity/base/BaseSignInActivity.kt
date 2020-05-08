/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.base

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.Observer
import com.flowcrypt.email.R
import com.flowcrypt.email.api.email.EmailUtil
import com.flowcrypt.email.api.email.JavaEmailConstants
import com.flowcrypt.email.api.retrofit.response.api.DomainRulesResponse
import com.flowcrypt.email.api.retrofit.response.base.ApiResponse
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.api.retrofit.response.model.node.NodeKeyDetails
import com.flowcrypt.email.jetpack.viewmodel.EnterpriseDomainRulesViewModel
import com.flowcrypt.email.jetpack.viewmodel.LoadPrivateKeysViewModel
import com.flowcrypt.email.jetpack.viewmodel.PrivateKeysViewModel
import com.flowcrypt.email.model.KeyDetails
import com.flowcrypt.email.security.SecurityUtils
import com.flowcrypt.email.ui.activity.AddNewAccountManuallyActivity
import com.flowcrypt.email.ui.activity.BaseNodeActivity
import com.flowcrypt.email.util.GeneralUtil
import com.flowcrypt.email.util.UIUtil
import com.flowcrypt.email.util.exception.SavePrivateKeyToDatabaseException
import com.flowcrypt.email.util.google.GoogleApiClientHelper
import com.flowcrypt.email.util.idling.SingleIdlingResources
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.android.material.snackbar.Snackbar
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import java.util.*

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
  private var isRunSignInWithGmailNeeded: Boolean = false
  protected var uuid: String? = null
  protected var domainRules: List<String>? = null

  private val enterpriseDomainRulesViewModel: EnterpriseDomainRulesViewModel by viewModels()
  protected val loadPrivateKeysViewModel: LoadPrivateKeysViewModel by viewModels()
  protected val privateKeysViewModel: PrivateKeysViewModel by viewModels()

  @get:VisibleForTesting
  val idlingForFetchingKeys: SingleIdlingResources = SingleIdlingResources()

  abstract val progressView: View?

  protected var googleSignInAccount: GoogleSignInAccount? = null

  abstract fun onSignSuccess(googleSignInAccount: GoogleSignInAccount?)
  abstract fun onFetchKeysCompleted(keyDetailsList: ArrayList<NodeKeyDetails>?)
  abstract fun onPrivateKeysSaved()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    if (savedInstanceState != null) {
      this.googleSignInAccount = savedInstanceState.getParcelable(KEY_CURRENT_GOOGLE_SIGN_IN_ACCOUNT)
      this.uuid = savedInstanceState.getString(KEY_UUID)
    }

    client = GoogleSignIn.getClient(this, GoogleApiClientHelper.generateGoogleSignInOptions())
    initViews()

    setupEnterpriseViewModel()
    setupLoadPrivateKeysViewModel()
    setupPrivateKeysViewModel()
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

        val account = googleSignInAccount?.account?.name ?: return
        val idToken = googleSignInAccount?.idToken ?: return
        uuid = SecurityUtils.generateRandomUUID()
        if (JavaEmailConstants.EMAIL_PROVIDER_GMAIL.equals(EmailUtil.getDomain(account), true)) {
          domainRules = emptyList()
          onSignSuccess(googleSignInAccount)
        } else {
          uuid?.let { enterpriseDomainRulesViewModel.getDomainRules(account, it, idToken) }
        }
      } else {
        val error = task.exception

        if (error is ApiException) {
          throw error
        }

        UIUtil.showInfoSnackbar(rootView, error?.message ?: error?.javaClass?.simpleName
        ?: getString(R.string.unknown_error))
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
    enterpriseDomainRulesViewModel.domainRulesLiveData.observe(this, Observer<Result<ApiResponse>?> {
      it?.let {
        when (it.status) {
          Result.Status.LOADING -> {
            UIUtil.exchangeViewVisibility(true, progressView, rootView)
          }

          Result.Status.SUCCESS -> {
            val result = it.data as? DomainRulesResponse
            domainRules = result?.domainRules?.flags ?: emptyList()
            onSignSuccess(googleSignInAccount)
          }

          Result.Status.ERROR -> {
            UIUtil.exchangeViewVisibility(false, progressView, rootView)
            Toast.makeText(this, it.data?.apiError?.msg
                ?: getString(R.string.could_not_load_domain_rules), Toast.LENGTH_SHORT).show()
          }

          Result.Status.EXCEPTION -> {
            UIUtil.exchangeViewVisibility(false, progressView, rootView)
            Toast.makeText(this, it.exception?.message
                ?: getString(R.string.could_not_load_domain_rules), Toast.LENGTH_SHORT).show()
          }
        }
      }
    })
  }

  private fun setupLoadPrivateKeysViewModel() {
    loadPrivateKeysViewModel.privateKeysLiveData.observe(this, Observer {
      it?.let {
        when (it.status) {
          Result.Status.LOADING -> {
            idlingForFetchingKeys.setIdleState(false)
            UIUtil.exchangeViewVisibility(true, progressView, rootView)
          }

          Result.Status.SUCCESS -> {
            idlingForFetchingKeys.setIdleState(true)
            onFetchKeysCompleted(it.data)
          }

          Result.Status.ERROR, Result.Status.EXCEPTION -> {
            idlingForFetchingKeys.setIdleState(true)

            UIUtil.exchangeViewVisibility(false, progressView, rootView)

            if (it.exception is UserRecoverableAuthIOException) {
              startActivityForResult(it.exception.intent, REQUEST_CODE_RESOLVE_SIGN_IN_ERROR)
            } else {
              UIUtil.showInfoSnackbar(rootView,
                  it.exception?.message ?: it.exception?.javaClass?.simpleName
                  ?: getString(R.string.unknown_error))
            }
          }
        }
      }
    })
  }

  private fun setupPrivateKeysViewModel() {
    privateKeysViewModel.savePrivateKeysLiveData.observe(this, Observer {
      it?.let {
        when (it.status) {
          Result.Status.LOADING -> {
            UIUtil.exchangeViewVisibility(true, progressView, rootView)
          }

          Result.Status.SUCCESS -> {
            onPrivateKeysSaved()
          }

          Result.Status.ERROR, Result.Status.EXCEPTION -> {
            UIUtil.exchangeViewVisibility(false, progressView, rootView)
            val e = it.exception
            if (e is SavePrivateKeyToDatabaseException) {
              showSnackbar(rootView, e.message ?: e.javaClass.simpleName,
                  getString(R.string.retry), Snackbar.LENGTH_INDEFINITE, View.OnClickListener {
                privateKeysViewModel.encryptAndSaveKeysToDatabase(e.keys, KeyDetails.Type.EMAIL)
              })
            } else {
              showInfoSnackbar(rootView, e?.message ?: e?.javaClass?.simpleName
              ?: getString(R.string.unknown_error))
            }
          }
        }
      }
    })
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
