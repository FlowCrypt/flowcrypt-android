/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.ui.activity.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.lifecycle.Lifecycle
import com.flowcrypt.email.R
import com.flowcrypt.email.databinding.FragmentSignatureSettingsBinding
import com.flowcrypt.email.extensions.hideKeyboard
import com.flowcrypt.email.extensions.showKeyboard
import com.flowcrypt.email.ui.activity.fragment.base.BaseFragment
import com.flowcrypt.email.ui.activity.fragment.base.ProgressBehaviour

/**
 * @author Denys Bondarenko
 */
class SignatureSettingsFragment : BaseFragment<FragmentSignatureSettingsBinding>(),
  ProgressBehaviour {
  override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) =
    FragmentSignatureSettingsBinding.inflate(inflater, container, false)

  override val progressView: View?
    get() = binding?.progressBar
  override val contentView: View?
    get() = binding?.textViewSignatureExplanation
  override val statusView: View? = null

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    initViews()
  }

  override fun onSetupActionBarMenu(menuHost: MenuHost) {
    super.onSetupActionBarMenu(menuHost)
    menuHost.addMenuProvider(object : MenuProvider {
      private var menuActionEdit: MenuItem? = null
      private var menuActionSave: MenuItem? = null

      override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.fragment_signature_settings, menu)
      }

      override fun onPrepareMenu(menu: Menu) {
        super.onPrepareMenu(menu)
        menuActionEdit = menu.findItem(R.id.menuActionEdit)
        menuActionSave = menu.findItem(R.id.menuActionSave)
        if (binding?.editTextSignature?.text?.isEmpty() == true) {
          menuActionEdit?.let { onMenuItemSelected(it) }
        }
      }

      override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
          R.id.menuActionEdit -> {
            binding?.editTextSignature?.apply {
              isEnabled = true
              requestFocus()
              showKeyboard()
            }

            menuItem.isVisible = false
            menuActionSave?.isVisible = true
            true
          }

          R.id.menuActionSave -> {
            binding?.editTextSignature?.apply {
              isEnabled = false
              hideKeyboard()
            }

            menuItem.isVisible = false
            menuActionEdit?.isVisible = true
            true
          }

          else -> false
        }
      }
    }, viewLifecycleOwner, Lifecycle.State.RESUMED)
  }


  private fun initViews() {

  }
}
