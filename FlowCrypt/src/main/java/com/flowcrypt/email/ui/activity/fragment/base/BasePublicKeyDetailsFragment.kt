/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.ui.activity.fragment.base

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewbinding.ViewBinding
import com.flowcrypt.email.Constants
import com.flowcrypt.email.R
import com.flowcrypt.email.databinding.FragmentPublicKeyDetailsBinding
import com.flowcrypt.email.extensions.org.bouncycastle.openpgp.getLastModificationDate
import com.flowcrypt.email.extensions.org.pgpainless.key.info.generateKeyCapabilitiesDrawable
import com.flowcrypt.email.extensions.org.pgpainless.key.info.getColorStateListDependsOnStatus
import com.flowcrypt.email.extensions.org.pgpainless.key.info.getPrimaryKey
import com.flowcrypt.email.extensions.org.pgpainless.key.info.getStatusIcon
import com.flowcrypt.email.extensions.org.pgpainless.key.info.getStatusText
import com.flowcrypt.email.extensions.showInfoDialog
import com.flowcrypt.email.extensions.toast
import com.flowcrypt.email.ui.adapter.SubKeysListAdapter
import com.flowcrypt.email.ui.adapter.UserIdListAdapter
import com.flowcrypt.email.ui.adapter.recyclerview.itemdecoration.MarginItemDecoration
import com.flowcrypt.email.util.DateTimeUtil
import com.flowcrypt.email.util.GeneralUtil
import com.flowcrypt.email.util.exception.ExceptionUtil
import com.google.android.material.snackbar.Snackbar
import org.pgpainless.key.info.KeyRingInfo
import java.io.FileNotFoundException
import java.util.Date

/**
 * @author Denys Bondarenko
 */
abstract class BasePublicKeyDetailsFragment<T : ViewBinding> : BaseFragment<T>(),
  ProgressBehaviour {

  abstract val armoredPublicKey: String?
  abstract val publicKeyFingerprint: String
  abstract val keyOwnerEmail: String
  abstract val isAdditionActionsEnabled: Boolean

  override val progressView: View?
    get() = viewBinding?.progress?.root
  override val contentView: View?
    get() = viewBinding?.layoutContent
  override val statusView: View?
    get() = viewBinding?.status?.root

  private val userIdsAdapter = UserIdListAdapter()
  private val subKeysAdapter = SubKeysListAdapter()

  private val viewBinding: FragmentPublicKeyDetailsBinding?
    get() = binding as? FragmentPublicKeyDetailsBinding

  private val savePubKeyActivityResultLauncher =
    registerForActivityResult(ExportPubKeyCreateDocument()) { uri: Uri? -> uri?.let { saveKey(it) } }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    initUserIdsRecyclerView()
    initSubKeysRecyclerView()
  }

  override fun onSetupActionBarMenu(menuHost: MenuHost) {
    super.onSetupActionBarMenu(menuHost)
    menuHost.addMenuProvider(object : MenuProvider {
      override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.fragment_pub_key_details, menu)
      }

      override fun onPrepareMenu(menu: Menu) {
        super.onPrepareMenu(menu)
        if (isAdditionActionsEnabled) {
          menu.findItem(R.id.menuActionDelete)?.isVisible = true
          menu.findItem(R.id.menuActionEdit)?.isVisible = true
        }
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

          R.id.menuActionShow -> {
            showInfoDialog(dialogTitle = "", dialogMsg = armoredPublicKey)
            true
          }

          else -> handleAdditionMenuActions(menuItem)
        }
      }
    }, viewLifecycleOwner, Lifecycle.State.RESUMED)
  }

  open fun handleAdditionMenuActions(menuItem: MenuItem): Boolean = false

  protected fun updateViews(keyRingInfo: KeyRingInfo) {
    updatePrimaryKeyInfo(keyRingInfo)

    userIdsAdapter.submitList(keyRingInfo.userIds)
    subKeysAdapter.submit(keyRingInfo)
  }

  private fun initUserIdsRecyclerView() {
    viewBinding?.recyclerViewUserIds?.apply {
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
    viewBinding?.recyclerViewSubKeys?.apply {
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

  private fun updatePrimaryKeyInfo(keyRingInfo: KeyRingInfo) {
    val dateFormat = DateTimeUtil.getPgpDateFormat(context)
    viewBinding?.textViewPrimaryKeyFingerprint?.text = GeneralUtil.doSectionsInText(
      originalString = keyRingInfo.fingerprint.toString(), groupSize = 4
    )

    viewBinding?.textViewPrimaryKeyAlgorithm?.apply {
      val bitStrength =
        if (keyRingInfo.publicKey.bitStrength != -1) keyRingInfo.publicKey.bitStrength else null
      val algoWithBits = keyRingInfo.algorithm.name + (bitStrength?.let { "/$it" } ?: "")
      text = algoWithBits
    }

    viewBinding?.textViewPrimaryKeyCreated?.text = getString(
      R.string.template_created,
      dateFormat.format(Date(keyRingInfo.creationDate.time))
    )

    viewBinding?.textViewPrimaryKeyModified?.apply {
      text = keyRingInfo.getPrimaryKey()?.getLastModificationDate()?.let {
        getString(R.string.template_modified, dateFormat.format(it))
      }
    }

    viewBinding?.textViewPrimaryKeyExpiration?.apply {
      text = keyRingInfo.primaryKeyExpirationDate?.time?.let {
        getString(R.string.expires, dateFormat.format(Date(it)))
      } ?: getString(R.string.expires, getString(R.string.never))
    }

    viewBinding?.textViewPrimaryKeyCapabilities?.setCompoundDrawablesWithIntrinsicBounds(
      null,
      null,
      keyRingInfo.generateKeyCapabilitiesDrawable(
        requireContext(), keyRingInfo.getPrimaryKey()?.keyID ?: 0
      ),
      null
    )

    viewBinding?.textViewStatusValue?.apply {
      backgroundTintList = keyRingInfo.getColorStateListDependsOnStatus(requireContext())
      setCompoundDrawablesWithIntrinsicBounds(keyRingInfo.getStatusIcon(), 0, 0, 0)
      text = keyRingInfo.getStatusText(requireContext())
    }
  }

  private fun chooseDest() {
    val sanitizedEmail = keyOwnerEmail.replace("[^a-z0-9]".toRegex(), "")
    val fileName = "0x$publicKeyFingerprint-$sanitizedEmail-public_key.asc"
    savePubKeyActivityResultLauncher.launch(fileName)
  }

  inner class ExportPubKeyCreateDocument :
    ActivityResultContracts.CreateDocument("todo/todo") {
    override fun createIntent(context: Context, input: String): Intent {

      return super.createIntent(context, input)
        .addCategory(Intent.CATEGORY_OPENABLE)
        .setType(Constants.MIME_TYPE_PGP_KEY)
    }
  }
}