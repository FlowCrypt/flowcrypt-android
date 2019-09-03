/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.settings

import android.os.Bundle
import android.os.PersistableBundle
import android.text.TextUtils
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import androidx.loader.app.LoaderManager
import androidx.loader.content.Loader
import com.flowcrypt.email.BuildConfig
import com.flowcrypt.email.R
import com.flowcrypt.email.api.retrofit.ApiName
import com.flowcrypt.email.api.retrofit.BaseResponse
import com.flowcrypt.email.api.retrofit.request.api.PostHelpFeedbackRequest
import com.flowcrypt.email.api.retrofit.request.model.PostHelpFeedbackModel
import com.flowcrypt.email.api.retrofit.response.api.PostHelpFeedbackResponse
import com.flowcrypt.email.database.dao.source.AccountDao
import com.flowcrypt.email.database.dao.source.AccountDaoSource
import com.flowcrypt.email.model.results.LoaderResult
import com.flowcrypt.email.ui.activity.base.BaseBackStackSyncActivity
import com.flowcrypt.email.ui.loader.ApiServiceAsyncTaskLoader
import com.flowcrypt.email.util.GeneralUtil
import com.flowcrypt.email.util.UIUtil

/**
 * The feedback activity. Anywhere there is a question mark, it should take the user to this
 * screen.
 *
 * @author DenBond7
 * Date: 30.05.2017
 * Time: 9:56
 * E-mail: DenBond7@gmail.com
 */

class FeedbackActivity : BaseBackStackSyncActivity(), LoaderManager.LoaderCallbacks<LoaderResult> {

  private lateinit var progressBar: View
  private lateinit var layoutInput: View
  override lateinit var rootView: View
  private lateinit var editTextUserMsg: EditText

  private var account: AccountDao? = null
  private var isMsgSent: Boolean = false

  override val contentViewResourceId: Int
    get() = R.layout.activity_feedback

  private val isInformationValid: Boolean
    get() {
      return if (TextUtils.isEmpty(editTextUserMsg.text.toString())) {
        UIUtil.showInfoSnackbar(editTextUserMsg, getString(R.string.your_message_must_be_non_empty))
        false
      } else {
        true
      }
    }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    if (savedInstanceState != null) {
      this.isMsgSent = savedInstanceState.getBoolean(KEY_IS_MESSAGE_SENT)
    }

    initViews()

    account = AccountDaoSource().getActiveAccountInformation(this)
  }

  override fun onSaveInstanceState(outState: Bundle, outPersistentState: PersistableBundle) {
    super.onSaveInstanceState(outState, outPersistentState)
    outState.putBoolean(KEY_IS_MESSAGE_SENT, isMsgSent)
  }

  override fun onCreateOptionsMenu(menu: Menu): Boolean {
    menuInflater.inflate(R.menu.activity_feedback, menu)
    return true
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    when (item.itemId) {
      R.id.menuActionSend -> {
        if (!isMsgSent) {
          if (isInformationValid) {
            if (GeneralUtil.isConnected(this)) {
              UIUtil.hideSoftInput(this, editTextUserMsg)
              LoaderManager.getInstance(this).restartLoader(R.id.loader_id_post_help_feedback, null, this)
            } else {
              UIUtil.showInfoSnackbar(rootView, getString(R.string.internet_connection_is_not_available))
            }
          }
        } else {
          UIUtil.showInfoSnackbar(rootView, getString(R.string
              .you_already_sent_this_message))
        }
        return true
      }

      else -> return super.onOptionsItemSelected(item)
    }
  }

  override fun onCreateLoader(id: Int, args: Bundle?): Loader<LoaderResult> {
    when (id) {
      R.id.loader_id_post_help_feedback -> {
        UIUtil.exchangeViewVisibility(this, true, progressBar, layoutInput)
        val text = editTextUserMsg.text.toString() + "\n\n" + "Android v" + BuildConfig.VERSION_CODE

        return ApiServiceAsyncTaskLoader(applicationContext, PostHelpFeedbackRequest(ApiName.POST_HELP_FEEDBACK,
            PostHelpFeedbackModel(email = account!!.email, msg = text)))
      }
      else -> return Loader(this)
    }
  }

  override fun onLoadFinished(loader: Loader<LoaderResult>, loaderResult: LoaderResult?) {
    when (loader.id) {
      R.id.loader_id_post_help_feedback -> {
        UIUtil.exchangeViewVisibility(this, false, progressBar, layoutInput)
        if (loaderResult != null) {
          if (loaderResult.result != null) {
            val baseResponse = loaderResult.result as BaseResponse<*>?
            val response = baseResponse!!.responseModel as PostHelpFeedbackResponse?
            if (response!!.isSent) {
              this.isMsgSent = true
              showBackAction(response)
            } else if (response.apiError != null) {
              UIUtil.showInfoSnackbar(rootView, response.apiError.msg!!)
            } else {
              UIUtil.showInfoSnackbar(rootView, getString(R.string.unknown_error))
            }
          } else if (loaderResult.exception != null) {
            UIUtil.showInfoSnackbar(rootView, loaderResult.exception!!.message ?: "")
          } else {
            UIUtil.showInfoSnackbar(rootView, getString(R.string.unknown_error))
          }
        } else {
          UIUtil.showInfoSnackbar(rootView, getString(R.string.unknown_error))
        }
      }
    }
  }

  override fun onLoaderReset(loader: Loader<LoaderResult>) {

  }

  private fun showBackAction(response: PostHelpFeedbackResponse) {
    UIUtil.showSnackbar(rootView, response.text!!, getString(R.string.back), View.OnClickListener { finish() })
  }

  private fun initViews() {
    editTextUserMsg = findViewById(R.id.editTextUserMessage)
    progressBar = findViewById(R.id.progressBar)
    layoutInput = findViewById(R.id.layoutInput)
    rootView = findViewById(R.id.layoutContent)
  }

  companion object {
    private const val KEY_IS_MESSAGE_SENT = BuildConfig.APPLICATION_ID + ".KEY_IS_MESSAGE_SENT"
  }
}
