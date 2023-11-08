/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts.CreateDocument
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.flowcrypt.email.Constants
import com.flowcrypt.email.R
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.database.FlowCryptRoomDatabase
import com.flowcrypt.email.database.entity.PublicKeyEntity
import com.flowcrypt.email.databinding.FragmentPublicKeyDetailsBinding
import com.flowcrypt.email.extensions.countingIdlingResource
import com.flowcrypt.email.extensions.decrementSafely
import com.flowcrypt.email.extensions.incrementSafely
import com.flowcrypt.email.extensions.launchAndRepeatWithViewLifecycle
import com.flowcrypt.email.extensions.navController
import com.flowcrypt.email.extensions.org.bouncycastle.openpgp.armor
import com.flowcrypt.email.extensions.org.bouncycastle.openpgp.getLastModificationDate
import com.flowcrypt.email.extensions.org.pgpainless.key.info.generateKeyCapabilitiesDrawable
import com.flowcrypt.email.extensions.org.pgpainless.key.info.getColorStateListDependsOnStatus
import com.flowcrypt.email.extensions.org.pgpainless.key.info.getPrimaryKey
import com.flowcrypt.email.extensions.org.pgpainless.key.info.getPubKeysWithoutPrimary
import com.flowcrypt.email.extensions.org.pgpainless.key.info.getStatusIcon
import com.flowcrypt.email.extensions.org.pgpainless.key.info.getStatusText
import com.flowcrypt.email.extensions.showInfoDialog
import com.flowcrypt.email.extensions.toast
import com.flowcrypt.email.jetpack.lifecycle.CustomAndroidViewModelFactory
import com.flowcrypt.email.jetpack.viewmodel.PublicKeyDetailsViewModel
import com.flowcrypt.email.ui.activity.fragment.base.BaseFragment
import com.flowcrypt.email.ui.activity.fragment.base.ProgressBehaviour
import com.flowcrypt.email.ui.adapter.SubKeysListAdapter
import com.flowcrypt.email.ui.adapter.UserIdListAdapter
import com.flowcrypt.email.ui.adapter.recyclerview.itemdecoration.MarginItemDecoration
import com.flowcrypt.email.util.DateTimeUtil
import com.flowcrypt.email.util.GeneralUtil
import com.flowcrypt.email.util.exception.ExceptionUtil
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import org.pgpainless.key.info.KeyRingInfo
import java.io.FileNotFoundException
import java.util.Date


/**
 * This fragment shows the given public key details
 *
 * @author Denys Bondarenko
 */
@ExperimentalCoroutinesApi
class PublicKeyDetailsFragment : BaseFragment<FragmentPublicKeyDetailsBinding>(),
  ProgressBehaviour {
  override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) =
    FragmentPublicKeyDetailsBinding.inflate(inflater, container, false)

  private val args by navArgs<PublicKeyDetailsFragmentArgs>()
  private val publicKeyDetailsViewModel: PublicKeyDetailsViewModel by viewModels {
    object : CustomAndroidViewModelFactory(requireActivity().application) {
      @Suppress("UNCHECKED_CAST")
      override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return PublicKeyDetailsViewModel(args.publicKeyEntity, requireActivity().application) as T
      }
    }
  }

  private val cachedPublicKeyEntity: PublicKeyEntity?
    get() = publicKeyDetailsViewModel.publicKeyEntityStateFlow.value.data

  private val armoredPublicKey: String?
    get() = publicKeyDetailsViewModel.keyRingInfoStateFlow.value.data?.keys?.armor(
      hideArmorMeta = account?.clientConfiguration?.shouldHideArmorMeta() ?: false
    )

  private val savePubKeyActivityResultLauncher =
    registerForActivityResult(ExportPubKeyCreateDocument()) { uri: Uri? -> uri?.let { saveKey(it) } }

  private val userIdsAdapter = UserIdListAdapter()
  private val subKeysAdapter = SubKeysListAdapter()

  override val progressView: View?
    get() = binding?.progress?.root
  override val contentView: View?
    get() = binding?.layoutContent
  override val statusView: View?
    get() = binding?.status?.root

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    initUserIdsRecyclerView()
    initSubKeysRecyclerView()
    setupPublicKeyDetailsViewModel()
  }

  override fun onSetupActionBarMenu(menuHost: MenuHost) {
    super.onSetupActionBarMenu(menuHost)
    menuHost.addMenuProvider(object : MenuProvider {
      override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.fragment_pub_key_details, menu)
      }

      override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
          R.id.menuActionCopy -> {
            val clipboard =
              requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("pubKey", armoredPublicKey))
            toast(R.string.public_key_copied_to_clipboard)
            true
          }

          R.id.menuActionSave -> {
            chooseDest()
            true
          }

          R.id.menuActionDelete -> {
            lifecycleScope.launch {
              val roomDatabase = FlowCryptRoomDatabase.getDatabase(requireContext())
              roomDatabase.pubKeyDao().deleteSuspend(args.publicKeyEntity)
              navController?.navigateUp()
            }
            true
          }

          R.id.menuActionEdit -> {
            account?.let { accountEntity ->
              cachedPublicKeyEntity?.let { publicKeyEntity ->
                navController?.navigate(
                  PublicKeyDetailsFragmentDirections
                    .actionPublicKeyDetailsFragmentToEditContactFragment(
                      accountEntity,
                      publicKeyEntity
                    )
                )
              }
            }
            true
          }

          else -> false
        }
      }
    }, viewLifecycleOwner, Lifecycle.State.RESUMED)
  }

  private fun setupPublicKeyDetailsViewModel() {
    launchAndRepeatWithViewLifecycle {
      publicKeyDetailsViewModel.publicKeyEntityStateFlow.collect { }
    }

    launchAndRepeatWithViewLifecycle {
      publicKeyDetailsViewModel.keyRingInfoStateFlow.collect {
        when (it.status) {
          Result.Status.LOADING -> {
            countingIdlingResource?.incrementSafely(this@PublicKeyDetailsFragment)
            showProgress()
          }

          Result.Status.SUCCESS -> {
            val keyRingInfo = it.data
            if (keyRingInfo == null) {
              toast(R.string.error_no_keys)
              navController?.navigateUp()
            } else {
              updateViews(keyRingInfo)
              showContent()
            }
            countingIdlingResource?.decrementSafely(this@PublicKeyDetailsFragment)
          }

          Result.Status.EXCEPTION -> {
            showStatus(getString(R.string.could_not_extract_key_details))

            var msg = it.exception?.message ?: it.exception?.javaClass?.simpleName
            ?: getString(R.string.unknown_error)

            if (it.exception is NoSuchElementException) {
              val matchingString = "No suitable signatures found on the key."
              if (matchingString.equals(other = it.exception.message, ignoreCase = true)) {
                msg = getString(R.string.key_sha1_warning_msg)
              }
            }

            showInfoDialog(dialogTitle = "", dialogMsg = msg)
            countingIdlingResource?.decrementSafely(this@PublicKeyDetailsFragment)
          }
          else -> {}
        }
      }
    }
  }

  private fun saveKey(uri: Uri?) {
    uri ?: return
    try {
      val context = this.context ?: return
      val pubKey = armoredPublicKey ?: return
      GeneralUtil.writeFileFromStringToUri(context, uri, pubKey)
      toast(getString(R.string.saved))
    } catch (e: Exception) {
      e.printStackTrace()
      var error = if (e.message.isNullOrEmpty()) e.javaClass.simpleName else e.message

      if (e is IllegalStateException) {
        if (e.message != null && e.message!!.startsWith("Already exists")) {
          error = getString(R.string.not_saved_file_already_exists)
          showInfoSnackbar(requireView(), error, Snackbar.LENGTH_LONG)

          try {
            context?.contentResolver?.let { contentResolver ->
              DocumentsContract.deleteDocument(contentResolver, uri)
            }
          } catch (fileNotFound: FileNotFoundException) {
            fileNotFound.printStackTrace()
          }

          return
        } else {
          ExceptionUtil.handleError(e)
        }
      } else {
        ExceptionUtil.handleError(e)
      }

      showInfoSnackbar(requireView(), error ?: "")
    }
  }

  private fun updateViews(keyRingInfo: KeyRingInfo) {
    updatePrimaryKeyInfo(keyRingInfo)

    userIdsAdapter.submitList(keyRingInfo.userIds)
    subKeysAdapter.submitList(keyRingInfo, keyRingInfo.getPubKeysWithoutPrimary())
  }

  private fun updatePrimaryKeyInfo(keyRingInfo: KeyRingInfo) {
    val dateFormat = DateTimeUtil.getPgpDateFormat(context)
    binding?.textViewPrimaryKeyFingerprint?.text = GeneralUtil.doSectionsInText(
      originalString = keyRingInfo.fingerprint.toString(), groupSize = 4
    )
    val bitStrength =
      if (keyRingInfo.publicKey.bitStrength != -1) keyRingInfo.publicKey.bitStrength else null
    val algoWithBits = keyRingInfo.algorithm.name + (bitStrength?.let { "/$it" } ?: "")
    binding?.textViewPrimaryKeyAlgorithm?.text = algoWithBits
    binding?.textViewPrimaryKeyCreated?.text = getString(
      R.string.template_created,
      dateFormat.format(Date(keyRingInfo.creationDate.time))
    )
    keyRingInfo.getPrimaryKey()?.getLastModificationDate()?.let {
      binding?.textViewPrimaryKeyModified?.text = getString(
        R.string.template_modified, dateFormat.format(it)
      )
    }

    binding?.textViewPrimaryKeyExpiration?.text = keyRingInfo.primaryKeyExpirationDate?.time?.let {
      context?.getString(R.string.expires, dateFormat.format(Date(it)))
    } ?: context?.getString(
      R.string.expires, getString(R.string.never)
    )

    binding?.textViewPrimaryKeyCapabilities?.setCompoundDrawablesWithIntrinsicBounds(
      null,
      null,
      keyRingInfo.generateKeyCapabilitiesDrawable(
        requireContext(), keyRingInfo.getPrimaryKey()?.keyID ?: 0
      ),
      null
    )

    binding?.textViewStatusValue?.backgroundTintList =
      keyRingInfo.getColorStateListDependsOnStatus(requireContext())
    binding?.textViewStatusValue?.setCompoundDrawablesWithIntrinsicBounds(
      keyRingInfo.getStatusIcon(), 0, 0, 0
    )
    binding?.textViewStatusValue?.text = keyRingInfo.getStatusText(requireContext())
  }

  private fun initUserIdsRecyclerView() {
    binding?.recyclerViewUserIds?.apply {
      layoutManager = LinearLayoutManager(context)
      addItemDecoration(
        MarginItemDecoration(
          marginTop = resources.getDimensionPixelSize(R.dimen.default_margin_small)
        )
      )
      adapter = userIdsAdapter
    }
  }

  private fun initSubKeysRecyclerView() {
    binding?.recyclerViewSubKeys?.apply {
      val linearLayoutManager = LinearLayoutManager(context)
      val decoration = DividerItemDecoration(context, linearLayoutManager.orientation)
      layoutManager = linearLayoutManager
      addItemDecoration(decoration)
      addItemDecoration(
        MarginItemDecoration(
          marginTop = resources.getDimensionPixelSize(R.dimen.default_margin_small),
          marginBottom = resources.getDimensionPixelSize(R.dimen.default_margin_small),
        )
      )
      adapter = subKeysAdapter
    }
  }

  private fun chooseDest() {
    val sanitizedEmail = args.recipientEntity.email.replace("[^a-z0-9]".toRegex(), "")
    val fileName = "0x" + cachedPublicKeyEntity?.fingerprint + "-" +
        sanitizedEmail + "-publickey" + ".asc"
    savePubKeyActivityResultLauncher.launch(fileName)
  }

  inner class ExportPubKeyCreateDocument :
    CreateDocument("todo/todo") {
    override fun createIntent(context: Context, input: String): Intent {

      return super.createIntent(context, input)
        .addCategory(Intent.CATEGORY_OPENABLE)
        .setType(Constants.MIME_TYPE_PGP_KEY)
    }
  }
}
