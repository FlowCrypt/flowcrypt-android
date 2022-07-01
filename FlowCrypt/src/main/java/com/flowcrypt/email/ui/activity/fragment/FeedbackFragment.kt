/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
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
import androidx.navigation.fragment.navArgs
import com.flowcrypt.email.R
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.databinding.FragmentFeedbackBinding
import com.flowcrypt.email.extensions.gone
import com.flowcrypt.email.extensions.navController
import com.flowcrypt.email.extensions.toast
import com.flowcrypt.email.extensions.visibleOrGone
import com.flowcrypt.email.service.FeedbackJobIntentService
import com.flowcrypt.email.ui.activity.fragment.base.BaseFragment
import com.flowcrypt.email.ui.activity.fragment.dialog.EditScreenshotDialogFragment
import com.flowcrypt.email.util.GeneralUtil
import com.flowcrypt.email.util.UIUtil

/**
 * @author Denis Bondarenko
 *         Date: 3/18/22
 *         Time: 3:55 PM
 *         E-mail: DenBond7@gmail.com
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

              FeedbackJobIntentService.enqueueWork(
                context = requireContext(),
                account = nonNullAccount,
                userComment = binding?.editTextUserMessage?.text.toString(),
                screenShotBytes = screenShotBytes
              )
              toast(getString(R.string.thank_you_for_feedback))
              navController?.navigateUp()
            }
            return true
          }

          else -> return false
        }
      }
    })
  }

  override fun onAccountInfoRefreshed(accountEntity: AccountEntity?) {
    super.onAccountInfoRefreshed(accountEntity)
    accountEntity?.let { binding?.textInputLayoutUserEmail?.gone() }
  }

  private fun initViews() {
    binding?.imageButtonScreenshot?.setOnClickListener {
      navController?.navigate(
        FeedbackFragmentDirections.actionFeedbackFragmentToEditScreenshotDialogFragment(
          Screenshot(bitmapRaw)
        )
      )
    }

    binding?.checkBoxScreenshot?.setOnCheckedChangeListener { _, isChecked ->
      binding?.screenShotGroup?.visibleOrGone(isChecked)
    }

    binding?.imageButtonScreenshot?.setImageBitmap(bitmap)
  }

  private fun subscribeToEditScreenshot() {
    setFragmentResultListener(EditScreenshotDialogFragment.REQUEST_KEY_EDIT_SCREENSHOT) { _, bundle ->
      val byteArray =
        bundle.getByteArray(EditScreenshotDialogFragment.KEY_SCREENSHOT)
      byteArray?.let {
        bitmapRaw = it
        bitmap = BitmapFactory.decodeByteArray(bitmapRaw, 0, bitmapRaw.size)
        binding?.imageButtonScreenshot?.setImageBitmap(bitmap)
      }
    }
  }

  data class Screenshot(val byteArray: ByteArray) : Parcelable {
    constructor(parcel: Parcel) : this(parcel.createByteArray() ?: byteArrayOf())

    override fun equals(other: Any?): Boolean {
      if (this === other) return true
      if (javaClass != other?.javaClass) return false

      other as Screenshot

      if (!byteArray.contentEquals(other.byteArray)) return false

      return true
    }

    override fun hashCode(): Int {
      return byteArray.contentHashCode()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
      parcel.writeByteArray(byteArray)
    }

    override fun describeContents(): Int {
      return 0
    }

    companion object CREATOR : Parcelable.Creator<Screenshot> {
      override fun createFromParcel(parcel: Parcel) = Screenshot(parcel)
      override fun newArray(size: Int): Array<Screenshot?> = arrayOfNulls(size)
    }
  }
}
