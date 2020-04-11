/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.settings

import android.os.Bundle
import android.view.View
import com.flowcrypt.email.R
import com.flowcrypt.email.ui.activity.fragment.ContactsListFragment

/**
 * This Activity show information about contacts where has_pgp == true.
 *
 *
 * Clicking the delete button will remove a contact from the db. This is useful if the contact
 * now has a new public key attested: next time the user writes them, it will pull a new public key.
 *
 * @author DenBond7
 * Date: 26.05.2017
 * Time: 17:35
 * E-mail: DenBond7@gmail.com
 */

class ContactsSettingsActivity : BaseSettingsActivity() {
  private var contactsListFragment: ContactsListFragment? = null

  override val contentViewResourceId: Int
    get() = R.layout.activity_contacts_settings

  override val rootView: View
    get() = View(this)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    if (savedInstanceState == null) {
      contactsListFragment = ContactsListFragment.newInstance()
      contactsListFragment?.let {
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.layoutContent, it)
            .commitNow()
      }
    }
  }
}
