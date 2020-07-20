/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.flowcrypt.email.R
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.jetpack.viewmodel.LoadPrivateKeysViewModel
import com.flowcrypt.email.ui.activity.fragment.base.BaseFragment
import com.flowcrypt.email.ui.activity.fragment.base.ProgressBehaviour
import com.flowcrypt.email.util.GeneralUtil

/**
 * @author Denis Bondarenko
 *         Date: 7/17/20
 *         Time: 2:13 PM
 *         E-mail: DenBond7@gmail.com
 */
class AuthorizeAndSearchBackupsFragment : BaseFragment(), ProgressBehaviour {
  private val loadPrivateKeysViewModel: LoadPrivateKeysViewModel by viewModels()

  override val progressView: View?
    get() = view?.findViewById(R.id.progress)
  override val contentView: View?
    get() = view?.findViewById(R.id.layoutContent)
  override val statusView: View?
    get() = view?.findViewById(R.id.status)

  override val contentResourceId: Int = R.layout.fragment_authorize_search_private_key_backups

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    fetchBackups()
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    loadPrivateKeysViewModel.privateKeysLiveData.observe(viewLifecycleOwner, Observer {
      when (it.status) {
        Result.Status.LOADING -> {
          showProgress(it.progressMsg)
        }

        else -> {
          setFragmentResult(REQUEST_KEY_SEARCH_BACKUPS, bundleOf(KEY_PRIVATE_KEY_BACKUPS_RESULT to it))
          parentFragmentManager.popBackStack()
        }
      }
    })
  }

  private fun fetchBackups() {
    val accountEntity: AccountEntity? = arguments?.getParcelable(KEY_ACCOUNT)
    loadPrivateKeysViewModel.fetchAvailableKeys(accountEntity)
  }

  companion object {
    val REQUEST_KEY_SEARCH_BACKUPS = GeneralUtil.generateUniqueExtraKey("REQUEST_KEY_SEARCH_BACKUPS",
        AuthorizeAndSearchBackupsFragment::class.java)
    val KEY_PRIVATE_KEY_BACKUPS_RESULT = GeneralUtil.generateUniqueExtraKey("KEY_PRIVATE_KEY_BACKUPS_RESULT",
        AuthorizeAndSearchBackupsFragment::class.java)

    private val KEY_ACCOUNT = GeneralUtil.generateUniqueExtraKey("KEY_ACCOUNT", AuthorizeAndSearchBackupsFragment::class.java)

    fun newInstance(accountEntity: AccountEntity): AuthorizeAndSearchBackupsFragment {
      val fragment = AuthorizeAndSearchBackupsFragment()
      val args = Bundle()
      args.putParcelable(KEY_ACCOUNT, accountEntity)
      fragment.arguments = args
      return fragment
    }
  }
}