/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment.base

import android.app.Activity
import android.os.Bundle
import androidx.lifecycle.Observer
import com.flowcrypt.email.database.entity.AccountEntity

/**
 * @author Denis Bondarenko
 *         Date: 7/21/20
 *         Time: 6:29 PM
 *         E-mail: DenBond7@gmail.com
 */
abstract class BaseSingInFragment : BaseFragment() {
  protected val existedAccounts = mutableListOf<AccountEntity>()

  abstract fun runEmailManagerActivity()

  /**
   * Return the [Activity.RESULT_OK] to the initiator-activity.
   */
  abstract fun returnResultOk()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setupAllAccountsLiveData()
  }

  private fun setupAllAccountsLiveData() {
    accountViewModel.pureAccountsLiveData.observe(this, Observer {
      existedAccounts.clear()
      existedAccounts.addAll(it)
    })
  }
}