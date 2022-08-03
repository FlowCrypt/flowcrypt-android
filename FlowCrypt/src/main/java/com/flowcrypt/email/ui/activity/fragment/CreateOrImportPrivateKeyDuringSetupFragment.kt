/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.IntDef
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.setFragmentResultListener
import androidx.navigation.fragment.navArgs
import com.flowcrypt.email.R
import com.flowcrypt.email.api.retrofit.response.model.OrgRules
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.databinding.FragmentCreateOrImportPrivateKeyDuringSetupBinding
import com.flowcrypt.email.extensions.gone
import com.flowcrypt.email.extensions.navController
import com.flowcrypt.email.extensions.visibleOrGone
import com.flowcrypt.email.security.model.PgpKeyDetails
import com.flowcrypt.email.ui.activity.fragment.CreateOrImportPrivateKeyDuringSetupFragment.Result.Companion.HANDLE_CREATED_KEY
import com.flowcrypt.email.ui.activity.fragment.CreateOrImportPrivateKeyDuringSetupFragment.Result.Companion.HANDLE_RESOLVED_KEYS
import com.flowcrypt.email.ui.activity.fragment.CreateOrImportPrivateKeyDuringSetupFragment.Result.Companion.USE_ANOTHER_ACCOUNT
import com.flowcrypt.email.ui.activity.fragment.base.BaseFragment
import com.flowcrypt.email.util.GeneralUtil

/**
 * @author Denis Bondarenko
 *         Date: 3/10/22
 *         Time: 12:16 PM
 *         E-mail: DenBond7@gmail.com
 */
class CreateOrImportPrivateKeyDuringSetupFragment :
  BaseFragment<FragmentCreateOrImportPrivateKeyDuringSetupBinding>() {
  override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) =
    FragmentCreateOrImportPrivateKeyDuringSetupBinding.inflate(inflater, container, false)

  private val args by navArgs<CreateOrImportPrivateKeyDuringSetupFragmentArgs>()

  override val isDisplayHomeAsUpEnabled = false
  override val isToolbarVisible: Boolean = false

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    initViews()
    subscribeToCheckPrivateKeysFromImport()
    subscribeToCreatePrivateKey()
  }

  private fun initViews() {
    binding?.textViewTitle?.text =
      getString(R.string.set_up_flow_crypt, getString(R.string.app_name))
    binding?.buttonImportMyKey?.setOnClickListener {
      navController?.navigate(
        CreateOrImportPrivateKeyDuringSetupFragmentDirections
          .actionCreateOrImportPrivateKeyDuringSetupFragmentToImportPrivateKeysDuringSetupFragment(
            args.accountEntity
          )
      )
    }

    if (args.accountEntity.isRuleExist(OrgRules.DomainRule.NO_PRV_CREATE)) {
      binding?.buttonCreateNewKey?.gone()
    } else {
      binding?.buttonCreateNewKey?.setOnClickListener {
        navController?.navigate(
          CreateOrImportPrivateKeyDuringSetupFragmentDirections
            .actionCreateOrImportPrivateKeyDuringSetupFragmentToCreatePrivateKeyFirstFragment(
              args.accountEntity
            )
        )
      }
    }

    binding?.buttonSelectAnotherAccount?.visibleOrGone(args.isShowAnotherAccountBtnEnabled)
    binding?.buttonSelectAnotherAccount?.setOnClickListener {
      setResult(USE_ANOTHER_ACCOUNT, emptyList(), args.accountEntity)
    }
  }

  private fun setResult(
    @Result result: Int,
    keys: List<PgpKeyDetails>,
    account: AccountEntity
  ) {
    navController?.navigateUp()
    setFragmentResult(
      REQUEST_KEY_PRIVATE_KEYS,
      bundleOf(
        KEY_STATE to result,
        KEY_PRIVATE_KEYS to ArrayList(keys),
        KEY_ACCOUNT to account
      )
    )
  }

  private fun subscribeToCheckPrivateKeysFromImport() {
    setFragmentResultListener(ImportPrivateKeysDuringSetupFragment.REQUEST_KEY_PRIVATE_KEYS) { _, bundle ->
      val keys =
        bundle.getParcelableArrayList<PgpKeyDetails>(ImportPrivateKeysDuringSetupFragment.KEY_UNLOCKED_PRIVATE_KEYS)
      keys?.let { setResult(HANDLE_RESOLVED_KEYS, it, args.accountEntity) }
    }
  }

  private fun subscribeToCreatePrivateKey() {
    setFragmentResultListener(CreatePrivateKeyFirstFragment.REQUEST_KEY_CREATE_KEY) { _, bundle ->
      val pgpKeyDetails = bundle.getParcelable<PgpKeyDetails>(
        CreatePrivateKeyFirstFragment.KEY_CREATED_KEY
      ) ?: return@setFragmentResultListener
      val account = bundle.getParcelable<AccountEntity>(CreatePrivateKeyFirstFragment.KEY_ACCOUNT)
        ?: return@setFragmentResultListener
      setResult(HANDLE_CREATED_KEY, listOf(pgpKeyDetails), account)
    }
  }

  @Retention(AnnotationRetention.SOURCE)
  @IntDef(USE_ANOTHER_ACCOUNT, HANDLE_RESOLVED_KEYS, HANDLE_CREATED_KEY)
  annotation class Result {
    companion object {
      const val USE_ANOTHER_ACCOUNT = 0
      const val HANDLE_RESOLVED_KEYS = 1
      const val HANDLE_CREATED_KEY = 2
    }
  }

  companion object {
    val REQUEST_KEY_PRIVATE_KEYS = GeneralUtil.generateUniqueExtraKey(
      "REQUEST_KEY_PRIVATE_KEYS",
      CreateOrImportPrivateKeyDuringSetupFragment::class.java
    )

    val KEY_STATE = GeneralUtil.generateUniqueExtraKey(
      "KEY_STATE", CreateOrImportPrivateKeyDuringSetupFragment::class.java
    )

    val KEY_PRIVATE_KEYS = GeneralUtil.generateUniqueExtraKey(
      "KEY_PRIVATE_KEYS", CreateOrImportPrivateKeyDuringSetupFragment::class.java
    )

    val KEY_ACCOUNT = GeneralUtil.generateUniqueExtraKey(
      "KEY_ACCOUNT", CreateOrImportPrivateKeyDuringSetupFragment::class.java
    )
  }
}
