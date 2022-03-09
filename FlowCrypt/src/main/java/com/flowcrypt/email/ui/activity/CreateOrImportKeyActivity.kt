/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import com.flowcrypt.email.R
import com.flowcrypt.email.api.retrofit.response.model.OrgRules
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.model.KeyImportModel
import com.flowcrypt.email.ui.activity.base.BaseCheckClipboardBackStackActivity
import com.flowcrypt.email.util.GeneralUtil

/**
 * This activity describes a logic for create ot import private keys.
 *
 * @author DenBond7
 * Date: 23.05.2017.
 * Time: 16:15.
 * E-mail: DenBond7@gmail.com
 */
class CreateOrImportKeyActivity : BaseCheckClipboardBackStackActivity(), View.OnClickListener {
  private var isShowAnotherAccountBtnEnabled = true
  private lateinit var tempAccount: AccountEntity

  override val rootView: View
    get() = findViewById(R.id.layoutContent)

  override val isDisplayHomeAsUpEnabled: Boolean
    get() = false

  override val contentViewResourceId: Int
    get() = R.layout.activity_create_or_import_key

  override val isPrivateKeyChecking: Boolean
    get() = true

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    this.isShowAnotherAccountBtnEnabled =
      intent?.getBooleanExtra(KEY_IS_SHOW_ANOTHER_ACCOUNT_BUTTON_ENABLED, true) ?: true
    val accountEntity: AccountEntity? = intent.getParcelableExtra(EXTRA_KEY_ACCOUNT)

    if (accountEntity == null) {
      finish()
    } else {
      this.tempAccount = accountEntity
      initViews()
    }
  }

  override fun onClick(v: View) {
    when (v.id) {
      R.id.buttonCreateNewKey -> startActivityForResult(
        CreatePrivateKeyActivity.newIntent(this, tempAccount),
        REQUEST_CODE_CREATE_KEY_ACTIVITY
      )

      R.id.buttonImportMyKey -> {
        var keyImportModel: KeyImportModel? = null
        if (isBound) {
          keyImportModel = service.keyImportModel
        }

        startActivityForResult(
          ImportPrivateKeyActivity.getIntent(
            context = this,
            accountEntity = tempAccount,
            isSyncEnabled = false,
            title = getString(R.string.import_private_key),
            model = keyImportModel,
            throwErrorIfDuplicateFoundEnabled = true,
            cls = ImportPrivateKeyActivity::class.java,
            addAccountIfNotExist = true
          ),
          REQUEST_CODE_IMPORT_ACTIVITY
        )
      }

      R.id.buttonSelectAnotherAccount -> {
        val intent = Intent()
        intent.putExtra(EXTRA_KEY_ACCOUNT, tempAccount)
        setResult(RESULT_CODE_USE_ANOTHER_ACCOUNT, intent)
        finish()
      }
    }
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    when (requestCode) {
      REQUEST_CODE_CREATE_KEY_ACTIVITY -> when (resultCode) {
        Activity.RESULT_OK -> {
          setResult(Activity.RESULT_OK)
          finish()
        }
      }

      REQUEST_CODE_IMPORT_ACTIVITY -> when (resultCode) {
        Activity.RESULT_OK -> {
          setResult(Activity.RESULT_OK)
          finish()
        }

        /*CheckKeysActivity.RESULT_SKIP_REMAINING_KEYS -> {
          setResult(RESULT_CODE_HANDLE_RESOLVED_KEYS, data)
          finish()
        }*/
      }

      else -> super.onActivityResult(requestCode, resultCode, data)
    }
  }

  private fun initViews() {
    findViewById<View>(R.id.buttonImportMyKey)?.setOnClickListener(this)

    if (tempAccount.isRuleExist(OrgRules.DomainRule.NO_PRV_CREATE)) {
      findViewById<View>(R.id.buttonCreateNewKey)?.visibility = View.GONE
    } else {
      findViewById<View>(R.id.buttonCreateNewKey)?.setOnClickListener(this)
    }

    val buttonSelectAnotherAccount = findViewById<View>(R.id.buttonSelectAnotherAccount)
    if (isShowAnotherAccountBtnEnabled) {
      buttonSelectAnotherAccount?.visibility = View.VISIBLE
      buttonSelectAnotherAccount?.setOnClickListener(this)
    } else {
      buttonSelectAnotherAccount?.visibility = View.GONE
    }
  }

  companion object {
    const val RESULT_CODE_USE_ANOTHER_ACCOUNT = 10
    const val RESULT_CODE_HANDLE_RESOLVED_KEYS = 11

    val EXTRA_KEY_ACCOUNT =
      GeneralUtil.generateUniqueExtraKey("EXTRA_KEY_ACCOUNT", CreateOrImportKeyActivity::class.java)

    private const val REQUEST_CODE_IMPORT_ACTIVITY = 11
    private const val REQUEST_CODE_CREATE_KEY_ACTIVITY = 12
    private val KEY_IS_SHOW_ANOTHER_ACCOUNT_BUTTON_ENABLED =
      GeneralUtil.generateUniqueExtraKey(
        "KEY_IS_SHOW_ANOTHER_ACCOUNT_BUTTON_ENABLED",
        CreateOrImportKeyActivity::class.java
      )

    fun newIntent(
      context: Context,
      accountEntity: AccountEntity,
      isShowAnotherAccountBtnEnabled: Boolean
    ): Intent {
      val intent = Intent(context, CreateOrImportKeyActivity::class.java)
      intent.putExtra(EXTRA_KEY_ACCOUNT, accountEntity)
      intent.putExtra(KEY_IS_SHOW_ANOTHER_ACCOUNT_BUTTON_ENABLED, isShowAnotherAccountBtnEnabled)
      return intent
    }
  }
}
