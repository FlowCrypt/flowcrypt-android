/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment

import android.os.Bundle
import com.flowcrypt.email.ui.activity.fragment.base.BaseFragment
import com.flowcrypt.email.util.GeneralUtil

/**
 * This fragment shows the given public key details
 *
 * @author Denis Bondarenko
 *         Date: 9/20/19
 *         Time: 8:54 AM
 *         E-mail: DenBond7@gmail.com
 */
class PublicKeyDetailsFragment : BaseFragment() {

  companion object {
    private val KEY_PUBLIC_KEY = GeneralUtil.generateUniqueExtraKey("KEY_PUBLIC_KEY",
        PublicKeyDetailsFragment::class.java)

    fun newInstance(publicKey: String): PublicKeyDetailsFragment {
      val fragment = PublicKeyDetailsFragment()
      val args = Bundle()
      args.putString(KEY_PUBLIC_KEY, publicKey)
      fragment.arguments = args
      return fragment
    }
  }
}
