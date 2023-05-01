/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.navArgs
import com.flowcrypt.email.R
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.databinding.FragmentFeedbackBinding
import com.flowcrypt.email.extensions.gone
import com.flowcrypt.email.extensions.navController
import com.flowcrypt.email.extensions.toast
import com.flowcrypt.email.extensions.visibleOrGone
import com.flowcrypt.email.model.Screenshot
import com.flowcrypt.email.ui.activity.fragment.base.BaseFragment
import com.flowcrypt.email.ui.activity.fragment.dialog.EditScreenshotDialogFragment
import com.flowcrypt.email.ui.activity.fragment.dialog.SendFeedbackDialogFragment
import com.flowcrypt.email.util.GeneralUtil
import com.flowcrypt.email.util.UIUtil

/**
 * @author Denys Bondarenko
 */
class FeedbackFragment : BaseFragment<FragmentFeedbackBinding>() {
  override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) =
    FragmentFeedbackBinding.inflate(inflater, container, false)

  private val args by navArgs<FeedbackFragmentArgs>()
  private var bitmapRaw: ByteArray = byteArrayOf()
  private lateinit var bitmap: Bitmap

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    bitmapRaw = args.screenshot.byteArray
    bitmap = BitmapFactory.decodeByteArray(
      args.screenshot.byteArray, 0, args.screenshot.byteArray.size
    )
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    initViews()
    subscribeToEditScreenshot()
    subscribeToSendFeedback()
  }

  override fun onSetupActionBarMenu(menuHost: MenuHost) {
    super.onSetupActionBarMenu(menuHost)
    menuHost.addMenuProvider(object : MenuProvider {
      override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.fragment_feedback, menu)
      }

      override fun onPrepareMenu(menu: Menu) {
        super.onPrepareMenu(menu)
        menu.findItem(R.id.menuActionHelp).isVisible = false
      }

      override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        when (menuItem.itemId) {
          R.id.menuActionSend -> {
            if (account == null) {
              if (binding?.editTextUserEmail?.text?.isEmpty() == true) {
                binding?.editTextUserEmail?.requestFocus()
                toast(R.string.email_must_be_non_empty, Toast.LENGTH_LONG)
                return true
              } else if (!GeneralUtil.isEmailValid(binding?.editTextUserEmail?.text)) {
                binding?.editTextUserEmail?.requestFocus()
                toast(R.string.error_email_is_not_valid, Toast.LENGTH_LONG)
                return true
              }
            }

            if (binding?.editTextUserMessage?.text?.isEmpty() == true) {
              binding?.editTextUserMessage?.requestFocus()
              toast(R.string.your_message_must_be_non_empty, Toast.LENGTH_LONG)
            } else {
              val screenShotBytes = UIUtil.getCompressedByteArrayOfBitmap(
                if (binding?.checkBoxScreenshot?.isChecked == true) {
                  bitmap
                } else {
                  null
                }, 100
              )

              val nonNullAccount =
                account ?: AccountEntity(email = binding?.editTextUserEmail?.text.toString())

              navController?.navigate(
                FeedbackFragmentDirections.actionFeedbackFragmentToSendFeedbackDialogFragment(
                  nonNullAccount,
                  binding?.editTextUserMessage?.text.toString(),
                  screenShotBytes?.let { Screenshot(it) }
                )
              )
            }
            return true
          }

          else -> return false
        }
      }
    }, viewLifecycleOwner, Lifecycle.State.RESUMED)
  }

  override fun onAccountInfoRefreshed(accountEntity: AccountEntity?) {
    super.onAccountInfoRefreshed(accountEntity)
    accountEntity?.let { binding?.textInputLayoutUserEmail?.gone() }
  }

  private fun initViews() {
    binding?.textViewAuthorHint?.text = getString(
      R.string.feedback_thank_you_for_trying_message,
      getString(R.string.app_name)
    )
    binding?.imageButtonScreenshot?.setOnClickListener {
      navController?.navigate(
        FeedbackFragmentDirections.actionFeedbackFragmentToEditScreenshotDialogFragment(
          requestKey = REQUEST_KEY_EDIT_SCREENSHOT,
          screenshot = Screenshot(bitmapRaw)
        )
      )
    }

    binding?.checkBoxScreenshot?.setOnCheckedChangeListener { _, isChecked ->
      binding?.screenShotGroup?.visibleOrGone(isChecked)
    }

    binding?.imageButtonScreenshot?.setImageBitmap(bitmap)
  }

  private fun subscribeToEditScreenshot() {
    setFragmentResultListener(REQUEST_KEY_EDIT_SCREENSHOT) { _, bundle ->
      val byteArray =
        bundle.getByteArray(EditScreenshotDialogFragment.KEY_SCREENSHOT)
      byteArray?.let {
        bitmapRaw = it
        bitmap = BitmapFactory.decodeByteArray(bitmapRaw, 0, bitmapRaw.size)
        binding?.imageButtonScreenshot?.setImageBitmap(bitmap)
      }
    }
  }

  private fun subscribeToSendFeedback() {
    setFragmentResultListener(SendFeedbackDialogFragment.REQUEST_KEY_RESULT) { _, bundle ->
      val isSent = bundle.getBoolean(SendFeedbackDialogFragment.KEY_RESULT)
      if (isSent) {
        toast(getString(R.string.thank_you_for_feedback))
        navController?.navigateUp()
      }
    }
  }

  companion object {
    private val REQUEST_KEY_EDIT_SCREENSHOT = GeneralUtil.generateUniqueExtraKey(
      "REQUEST_KEY_EDIT_SCREENSHOT",
      FeedbackFragment::class.java
    )
  }
}
