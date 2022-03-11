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
class CreateOrImportPrivateKeyDuringSetupFragment : BaseFragment() {
  private var binding: FragmentCreateOrImportPrivateKeyDuringSetupBinding? = null
  private val args by navArgs<CreateOrImportPrivateKeyDuringSetupFragmentArgs>()

  override val contentResourceId = R.layout.fragment_create_or_import_private_key_during_setup
  override val isDisplayHomeAsUpEnabled = false
  override val isToolbarVisible: Boolean = false

  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
  ): View? {
    binding = FragmentCreateOrImportPrivateKeyDuringSetupBinding
      .inflate(inflater, container, false)
    return binding?.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    initViews()
    subscribeToCheckPrivateKeysFromImport()
    subscribeToCreatePrivateKey()
  }

  private fun initViews() {
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
      returnResult(USE_ANOTHER_ACCOUNT)
    }
  }

  private fun returnResult(@CreateOrImportPrivateKeyDuringSetupFragment.Result state: Int) {
    setFragmentResult(
      CheckKeysFragment.REQUEST_KEY_CHECK_PRIVATE_KEYS,
      bundleOf(
        KEY_STATE to state
      )
    )
    navController?.navigateUp()
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

  private fun subscribeToCheckPrivateKeysFromImport() {
    setFragmentResultListener(ImportPrivateKeysDuringSetupFragment.REQUEST_KEY_PRIVATE_KEYS) { _, bundle ->
      val keys =
        bundle.getParcelableArrayList<PgpKeyDetails>(ImportPrivateKeysDuringSetupFragment.KEY_UNLOCKED_PRIVATE_KEYS)
      setFragmentResult(
        REQUEST_KEY_PRIVATE_KEYS,
        bundleOf(KEY_UNLOCKED_PRIVATE_KEYS to keys)
      )
      navController?.navigateUp()
    }
  }

  private fun subscribeToCreatePrivateKey() {
    setFragmentResultListener(CreatePrivateKeyFirstFragment.REQUEST_KEY_CREATE_KEY) { _, bundle ->
      val pgpKeyDetails =
        bundle.getParcelable<PgpKeyDetails>(CreatePrivateKeyFirstFragment.KEY_CREATED_KEY)
      setFragmentResult(
        REQUEST_KEY_CREATE_KEY,
        bundleOf(KEY_CREATED_KEY to pgpKeyDetails)
      )
      navController?.navigateUp()
    }
  }

  companion object {
    val KEY_STATE = GeneralUtil.generateUniqueExtraKey(
      "KEY_STATE", CreateOrImportPrivateKeyDuringSetupFragment::class.java
    )

    val REQUEST_KEY_PRIVATE_KEYS = GeneralUtil.generateUniqueExtraKey(
      "REQUEST_KEY_PRIVATE_KEYS",
      CreateOrImportPrivateKeyDuringSetupFragment::class.java
    )

    val KEY_UNLOCKED_PRIVATE_KEYS = GeneralUtil.generateUniqueExtraKey(
      "KEY_UNLOCKED_PRIVATE_KEYS", CreateOrImportPrivateKeyDuringSetupFragment::class.java
    )

    val REQUEST_KEY_CREATE_KEY = GeneralUtil.generateUniqueExtraKey(
      "REQUEST_KEY_PARSED_KEYS", CreateOrImportPrivateKeyDuringSetupFragment::class.java
    )

    val KEY_CREATED_KEY = GeneralUtil.generateUniqueExtraKey(
      "KEY_PARSED_KEYS", CreateOrImportPrivateKeyDuringSetupFragment::class.java
    )
  }
}
