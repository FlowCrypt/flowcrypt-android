/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.extensions

import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentTransaction

/**
 * This class describes extension function for [FragmentActivity]
 *
 * @author Denis Bondarenko
 *         Date: 11/22/19
 *         Time: 3:37 PM
 *         E-mail: DenBond7@gmail.com
 */

fun FragmentActivity.showDialogFragment(dialog: DialogFragment) {
  val fragmentTransaction: FragmentTransaction = supportFragmentManager.beginTransaction()
  supportFragmentManager.findFragmentByTag("dialog")?.let { fragmentTransaction.remove(it) }
  fragmentTransaction.addToBackStack(null)
  dialog.show(fragmentTransaction, "dialog")
}