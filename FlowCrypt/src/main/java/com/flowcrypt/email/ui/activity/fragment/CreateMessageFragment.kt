/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.format.Formatter
import android.util.Log
import android.view.ContextMenu
import android.view.Gravity
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.FilterQueryProvider
import android.widget.FrameLayout
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import androidx.core.view.isVisible
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import com.flowcrypt.email.Constants
import com.flowcrypt.email.R
import com.flowcrypt.email.api.email.EmailUtil
import com.flowcrypt.email.api.email.FoldersManager
import com.flowcrypt.email.api.email.model.AttachmentInfo
import com.flowcrypt.email.api.email.model.ExtraActionInfo
import com.flowcrypt.email.api.email.model.OutgoingMessageInfo
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.database.FlowCryptRoomDatabase
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.database.entity.RecipientEntity
import com.flowcrypt.email.database.entity.relation.RecipientWithPubKeys
import com.flowcrypt.email.databinding.FragmentCreateMessageBinding
import com.flowcrypt.email.extensions.appBarLayout
import com.flowcrypt.email.extensions.countingIdlingResource
import com.flowcrypt.email.extensions.decrementSafely
import com.flowcrypt.email.extensions.gone
import com.flowcrypt.email.extensions.incrementSafely
import com.flowcrypt.email.extensions.invisible
import com.flowcrypt.email.extensions.navController
import com.flowcrypt.email.extensions.org.bouncycastle.openpgp.toPgpKeyDetails
import com.flowcrypt.email.extensions.showInfoDialog
import com.flowcrypt.email.extensions.showKeyboard
import com.flowcrypt.email.extensions.showNeedPassphraseDialog
import com.flowcrypt.email.extensions.supportActionBar
import com.flowcrypt.email.extensions.toast
import com.flowcrypt.email.extensions.visible
import com.flowcrypt.email.extensions.visibleOrGone
import com.flowcrypt.email.jetpack.viewmodel.AccountAliasesViewModel
import com.flowcrypt.email.jetpack.viewmodel.ComposeMsgViewModel
import com.flowcrypt.email.jetpack.viewmodel.RecipientsViewModel
import com.flowcrypt.email.model.MessageEncryptionType
import com.flowcrypt.email.model.MessageType
import com.flowcrypt.email.security.KeysStorageImpl
import com.flowcrypt.email.service.PrepareOutgoingMessagesJobIntentService
import com.flowcrypt.email.ui.activity.fragment.base.BaseFragment
import com.flowcrypt.email.ui.activity.fragment.dialog.ChoosePublicKeyDialogFragment
import com.flowcrypt.email.ui.activity.fragment.dialog.FixNeedPassphraseIssueDialogFragment
import com.flowcrypt.email.ui.activity.fragment.dialog.NoPgpFoundDialogFragment
import com.flowcrypt.email.ui.adapter.FromAddressesAdapter
import com.flowcrypt.email.ui.adapter.RecipientAdapter
import com.flowcrypt.email.ui.widget.CustomChipSpanChipCreator
import com.flowcrypt.email.ui.widget.PGPContactChipSpan
import com.flowcrypt.email.ui.widget.PgpContactsNachoTextView
import com.flowcrypt.email.util.FileAndDirectoryUtils
import com.flowcrypt.email.util.GeneralUtil
import com.flowcrypt.email.util.UIUtil
import com.flowcrypt.email.util.exception.ExceptionUtil
import com.google.android.gms.common.util.CollectionUtils
import com.google.android.material.snackbar.Snackbar
import com.hootsuite.nachos.NachoTextView
import com.hootsuite.nachos.chip.Chip
import com.hootsuite.nachos.terminator.ChipTerminatorHandler
import com.hootsuite.nachos.tokenizer.SpanChipTokenizer
import com.hootsuite.nachos.validator.ChipifyingNachoValidator
import org.apache.commons.io.FileUtils
import org.bouncycastle.openpgp.PGPSecretKeyRing
import org.pgpainless.key.OpenPgpV4Fingerprint
import org.pgpainless.util.Passphrase
import java.io.File
import java.io.IOException
import java.util.regex.Pattern
import javax.mail.Message
import javax.mail.internet.InternetAddress

/**
 * This fragment describe a logic of sent an encrypted or standard message.
 *
 * @author DenBond7
 * Date: 10.05.2017
 * Time: 11:27
 * E-mail: DenBond7@gmail.com
 */
class CreateMessageFragment : BaseFragment<FragmentCreateMessageBinding>(),
  View.OnFocusChangeListener,
  AdapterView.OnItemSelectedListener,
  View.OnClickListener, PgpContactsNachoTextView.OnChipLongClickListener {

  override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) =
    FragmentCreateMessageBinding.inflate(inflater, container, false)

  private lateinit var draftCacheDir: File

  private val args by navArgs<CreateMessageFragmentArgs>()
  private val accountAliasesViewModel: AccountAliasesViewModel by viewModels()
  private val recipientsViewModel: RecipientsViewModel by viewModels()
  private val composeMsgViewModel: ComposeMsgViewModel by viewModels {
    object : ViewModelProvider.AndroidViewModelFactory(requireActivity().application) {
      @Suppress("UNCHECKED_CAST")
      override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return ComposeMsgViewModel(args.encryptedByDefault, requireActivity().application) as T
      }
    }
  }

  private val attachments: MutableList<AttachmentInfo> = mutableListOf()
  private var folderType: FoldersManager.FolderType? = null
  private var fromAddressesAdapter: FromAddressesAdapter<String>? = null
  private var cachedRecipientWithoutPubKeys: RecipientWithPubKeys? = null
  private var extraActionInfo: ExtraActionInfo? = null
  private var nonEncryptedHintView: View? = null

  private var isUpdateToCompleted = true
  private var isUpdateCcCompleted = true
  private var isUpdateBccCompleted = true
  private var isIncomingMsgInfoUsed: Boolean = false
  private var isMsgSentToQueue: Boolean = false
  private var originalColor: Int = 0

  override fun onAttach(context: Context) {
    super.onAttach(context)
    fromAddressesAdapter = FromAddressesAdapter(
      context, android.R.layout.simple_list_item_1, android.R.id.text1, ArrayList()
    )
    fromAddressesAdapter?.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
    fromAddressesAdapter?.setUseKeysInfo(composeMsgViewModel.msgEncryptionType === MessageEncryptionType.ENCRYPTED)

    initDraftCacheDir(context)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setHasOptionsMenu(true)
    initExtras(activity?.intent)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    initNonEncryptedHintView()
    updateActionBar()
    initViews()
    setupComposeMsgViewModel()
    setupAccountAliasesViewModel()
    setupPrivateKeysViewModel()
    setupRecipientsViewModel()
    subscribeToSetWebPortalPassword()
    subscribeToSelectRecipients()

    val isEncryptedMode = composeMsgViewModel.msgEncryptionType === MessageEncryptionType.ENCRYPTED
    if (args.incomingMessageInfo != null && GeneralUtil.isConnected(context) && isEncryptedMode) {
      updateRecipients()
    }
  }

  override fun onDestroyView() {
    super.onDestroyView()
    appBarLayout?.removeView(nonEncryptedHintView)
  }

  override fun onDestroy() {
    super.onDestroy()
    if (!isMsgSentToQueue) {
      for (att in attachments) {
        att.uri?.let { uri ->
          if (Constants.FILE_PROVIDER_AUTHORITY.equals(uri.authority, ignoreCase = true)) {
            context?.contentResolver?.delete(uri, null, null)
          }
        }
      }
    }
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    when (requestCode) {
      REQUEST_CODE_NO_PGP_FOUND_DIALOG -> when (resultCode) {
        NoPgpFoundDialogFragment.RESULT_CODE_SWITCH_TO_STANDARD_EMAIL -> {
          composeMsgViewModel.switchMessageEncryptionType(MessageEncryptionType.STANDARD)
        }

        NoPgpFoundDialogFragment.RESULT_CODE_IMPORT_THEIR_PUBLIC_KEY -> if (data != null) {
          val recipientWithPubKeys = data.getParcelableExtra<RecipientWithPubKeys>(
            NoPgpFoundDialogFragment.EXTRA_KEY_PGP_CONTACT
          )

          recipientWithPubKeys?.let {
            navController?.navigate(
              CreateMessageFragmentDirections
                .actionCreateMessageFragmentToImportMissingPublicKeyFragment(it)
            )
          }
        }

        NoPgpFoundDialogFragment.RESULT_CODE_COPY_FROM_OTHER_CONTACT -> if (data != null) {
          cachedRecipientWithoutPubKeys =
            data.getParcelableExtra(NoPgpFoundDialogFragment.EXTRA_KEY_PGP_CONTACT)

          cachedRecipientWithoutPubKeys?.let {
            navController?.navigate(
              CreateMessageFragmentDirections.actionCreateMessageFragmentToSelectRecipientsFragment(
                title = getString(R.string.use_public_key_from)
              )
            )
          }
        }

        NoPgpFoundDialogFragment.RESULT_CODE_REMOVE_CONTACT -> if (data != null) {
          val recipientWithPubKeys = data.getParcelableExtra<RecipientWithPubKeys>(
            NoPgpFoundDialogFragment.EXTRA_KEY_PGP_CONTACT
          )

          if (recipientWithPubKeys != null) {
            removeRecipientWithPubKey(
              recipientWithPubKeys,
              binding?.editTextRecipientTo,
              Message.RecipientType.TO
            )
            removeRecipientWithPubKey(
              recipientWithPubKeys,
              binding?.editTextRecipientCc,
              Message.RecipientType.CC
            )
            removeRecipientWithPubKey(
              recipientWithPubKeys,
              binding?.editTextRecipientBcc,
              Message.RecipientType.BCC
            )
          }
        }

        NoPgpFoundDialogFragment.RESULT_CODE_PROTECT_WITH_PASSWORD -> {
          binding?.btnSetWebPortalPassword?.callOnClick()
        }
      }

      /*REQUEST_CODE_IMPORT_PUBLIC_KEY -> when (resultCode) {
        Activity.RESULT_OK -> {
          Toast.makeText(context, R.string.the_key_successfully_imported, Toast.LENGTH_SHORT).show()
          updateRecipients()
        }
      }*/

      REQUEST_CODE_GET_CONTENT_FOR_SENDING -> when (resultCode) {
        Activity.RESULT_OK -> {
          addAttachmentInfoFromIntent(data)
        }
      }

      REQUEST_CODE_SHOW_PUB_KEY_DIALOG -> when (resultCode) {
        Activity.RESULT_OK -> {
          if (data != null) {
            val keyList: List<AttachmentInfo> =
              data.getParcelableArrayListExtra(ChoosePublicKeyDialogFragment.KEY_ATTACHMENT_INFO_LIST)
                ?: return
            val key = keyList.first()
            if (attachments.none { it.name == key.name && it.encodedSize == key.encodedSize }) {
              attachments.add(key)
              showAtts()
            }
          }
        }
      }

      REQUEST_CODE_SHOW_FIX_EMPTY_PASSPHRASE_DIALOG -> when (resultCode) {
        FixNeedPassphraseIssueDialogFragment.RESULT_OK -> {
          sendMsg()
        }
      }

      else -> super.onActivityResult(requestCode, resultCode, data)
    }
  }

  override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
    super.onCreateOptionsMenu(menu, inflater)
    inflater.inflate(R.menu.fragment_compose, menu)
  }

  override fun onPrepareOptionsMenu(menu: Menu) {
    super.onPrepareOptionsMenu(menu)
    val menuActionSwitchType = menu.findItem(R.id.menuActionSwitchType)
    val titleRes = if (composeMsgViewModel.msgEncryptionType === MessageEncryptionType.STANDARD)
      R.string.switch_to_secure_email
    else
      R.string
        .switch_to_standard_email
    menuActionSwitchType.setTitle(titleRes)

    if (args.serviceInfo?.isMsgTypeSwitchable == false) {
      menu.removeItem(R.id.menuActionSwitchType)
    }

    if (args.serviceInfo?.hasAbilityToAddNewAtt == false) {
      menu.removeItem(R.id.menuActionAttachFile)
    }
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    when (item.itemId) {
      R.id.menuActionSend -> {
        snackBar?.dismiss()

        if (isUpdateToCompleted && isUpdateCcCompleted && isUpdateBccCompleted) {
          UIUtil.hideSoftInput(context, view)
          if (isDataCorrect()) {
            if (composeMsgViewModel.msgEncryptionType == MessageEncryptionType.ENCRYPTED) {
              val keysStorage = KeysStorageImpl.getInstance(requireContext())
              val senderEmail = binding?.editTextFrom?.text.toString()
              val keyRings = keysStorage.getPGPSecretKeyRingsByUserId(senderEmail)
              if (keyRings.isNotEmpty()) {
                val firstMatchedSecretKey = keyRings.first()
                val openPgpV4Fingerprint = OpenPgpV4Fingerprint(firstMatchedSecretKey)
                val fingerprint = openPgpV4Fingerprint.toString()
                val passphrase = keysStorage.getPassphraseByFingerprint(fingerprint)
                if (passphrase?.isEmpty == true) {
                  showNeedPassphraseDialog(
                    listOf(fingerprint),
                    REQUEST_CODE_SHOW_FIX_EMPTY_PASSPHRASE_DIALOG
                  )
                  return true
                }
              }
            }

            sendMsg()
            this.isMsgSentToQueue = true
          }
        } else {
          Toast.makeText(
            context, R.string.please_wait_while_information_about_recipients_will_be_updated,
            Toast.LENGTH_SHORT
          ).show()
        }
        return true
      }

      R.id.menuActionAttachFile -> {
        attachFile()
        return true
      }

      R.id.menuActionIncludePubKey -> {
        showPubKeyDialog()
        return true
      }

      R.id.menuActionSwitchType -> {
        when (composeMsgViewModel.msgEncryptionType) {
          MessageEncryptionType.ENCRYPTED -> composeMsgViewModel.switchMessageEncryptionType(
            MessageEncryptionType.STANDARD
          )
          MessageEncryptionType.STANDARD -> composeMsgViewModel.switchMessageEncryptionType(
            MessageEncryptionType.ENCRYPTED
          )
        }
        return true
      }

      else -> return super.onOptionsItemSelected(item)
    }
  }

  override fun onCreateContextMenu(
    menu: ContextMenu,
    v: View,
    menuInfo: ContextMenu.ContextMenuInfo?
  ) {
    super.onCreateContextMenu(menu, v, menuInfo)
    when (v.id) {
      R.id.iBShowQuotedText -> {
        val inflater: MenuInflater = requireActivity().menuInflater
        inflater.inflate(R.menu.context_menu_delete_quoted_text, menu)
      }
    }
  }

  override fun onContextItemSelected(item: MenuItem): Boolean {
    return when (item.itemId) {
      R.id.menuDeleteQuotedText -> {
        binding?.iBShowQuotedText?.gone()
        args.incomingMessageInfo?.text = ""
        true
      }

      else -> super.onContextItemSelected(item)
    }
  }

  override fun onFocusChange(v: View, hasFocus: Boolean) {
    when (v.id) {
      R.id.editTextRecipientTo -> runUpdateActionForRecipients(
        Message.RecipientType.TO, hasFocus, (v as TextView).text.isEmpty()
      )

      R.id.editTextRecipientCc -> runUpdateActionForRecipients(
        Message.RecipientType.CC, hasFocus, (v as TextView).text.isEmpty()
      )

      R.id.editTextRecipientBcc -> runUpdateActionForRecipients(
        Message.RecipientType.BCC, hasFocus, (v as TextView).text.isEmpty()
      )

      R.id.editTextEmailSubject, R.id.editTextEmailMessage -> if (hasFocus) {
        var isExpandButtonNeeded = false
        if (binding?.editTextRecipientCc?.text?.isEmpty() == true) {
          binding?.layoutCc?.gone()
          isExpandButtonNeeded = true
        }

        if (binding?.editTextRecipientBcc?.text?.isEmpty() == true) {
          binding?.layoutBcc?.gone()
          isExpandButtonNeeded = true
        }

        if (isExpandButtonNeeded) {
          binding?.imageButtonAdditionalRecipientsVisibility?.visible()
          val layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT
          )
          layoutParams.gravity = Gravity.TOP or Gravity.END
          binding?.progressBarAndButtonLayout?.layoutParams = layoutParams
        }
      }
    }
  }

  override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
    when (parent?.id) {
      R.id.spinnerFrom -> {
        binding?.editTextFrom?.setText(parent.adapter.getItem(position) as CharSequence)
        if (composeMsgViewModel.msgEncryptionType === MessageEncryptionType.ENCRYPTED) {
          val adapter = parent.adapter as ArrayAdapter<*>
          val colorGray = UIUtil.getColor(requireContext(), R.color.gray)
          binding?.editTextFrom?.setTextColor(if (adapter.isEnabled(position)) originalColor else colorGray)
        } else {
          binding?.editTextFrom?.setTextColor(originalColor)
        }
      }
    }
  }

  override fun onNothingSelected(parent: AdapterView<*>) {

  }

  override fun onClick(v: View) {
    when (v.id) {
      R.id.imageButtonAliases -> if (fromAddressesAdapter?.count ?: 0 > 1) {
        binding?.spinnerFrom?.performClick()
      }

      R.id.imageButtonAdditionalRecipientsVisibility -> {
        binding?.layoutCc?.visible()
        binding?.layoutBcc?.visible()
        val layoutParams = FrameLayout.LayoutParams(
          FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.MATCH_PARENT
        )
        layoutParams.gravity = Gravity.TOP or Gravity.END

        binding?.progressBarAndButtonLayout?.layoutParams = layoutParams
        v.visibility = View.GONE
        binding?.editTextRecipientCc?.requestFocus()
      }

      R.id.iBShowQuotedText -> {
        val currentCursorPosition = binding?.editTextEmailMessage?.selectionStart ?: 0
        if (binding?.editTextEmailMessage?.text?.isNotEmpty() == true) {
          binding?.editTextEmailMessage?.append("\n" + EmailUtil.genReplyContent(args.incomingMessageInfo))
        } else {
          binding?.editTextEmailMessage?.append(EmailUtil.genReplyContent(args.incomingMessageInfo))
        }
        binding?.editTextEmailMessage?.setSelection(currentCursorPosition)
        v.visibility = View.GONE
      }
    }
  }

  override fun onChipLongClick(nachoTextView: NachoTextView, chip: Chip, event: MotionEvent) {}

  override fun onAccountInfoRefreshed(accountEntity: AccountEntity?) {
    super.onAccountInfoRefreshed(accountEntity)
    accountEntity?.email?.let { email ->
      if (fromAddressesAdapter?.objects?.contains(email) == false) {
        fromAddressesAdapter?.add(email)
      }
    }
  }

  private fun onMsgEncryptionTypeChange(messageEncryptionType: MessageEncryptionType?) {
    var emailMassageHint: String? = null
    if (messageEncryptionType != null) {
      when (messageEncryptionType) {
        MessageEncryptionType.ENCRYPTED -> {
          emailMassageHint = getString(R.string.prompt_compose_security_email)
          binding?.editTextRecipientTo?.onFocusChangeListener?.onFocusChange(
            binding?.editTextRecipientTo,
            false
          )
          binding?.editTextRecipientCc?.onFocusChangeListener?.onFocusChange(
            binding?.editTextRecipientCc,
            false
          )
          binding?.editTextRecipientBcc?.onFocusChangeListener?.onFocusChange(
            binding?.editTextRecipientBcc,
            false
          )
          fromAddressesAdapter?.setUseKeysInfo(true)

          val colorGray = UIUtil.getColor(requireContext(), R.color.gray)
          val selectedItemPosition = binding?.spinnerFrom?.selectedItemPosition
          if (selectedItemPosition != null && selectedItemPosition != AdapterView.INVALID_POSITION
            && binding?.spinnerFrom?.adapter?.count ?: 0 > selectedItemPosition
          ) {
            val isItemEnabled = fromAddressesAdapter?.isEnabled(selectedItemPosition) ?: true
            binding?.editTextFrom?.setTextColor(if (isItemEnabled) originalColor else colorGray)
          }
        }

        MessageEncryptionType.STANDARD -> {
          emailMassageHint = getString(R.string.prompt_compose_standard_email)
          isUpdateToCompleted = true
          isUpdateCcCompleted = true
          isUpdateBccCompleted = true
          fromAddressesAdapter?.setUseKeysInfo(false)
          binding?.editTextFrom?.setTextColor(originalColor)
        }
      }
    }
    binding?.textInputLayoutEmailMessage?.hint = emailMassageHint
  }

  private fun attachFile() {
    val intent = Intent()
    intent.action = Intent.ACTION_OPEN_DOCUMENT
    intent.addCategory(Intent.CATEGORY_OPENABLE)
    intent.type = "*/*"
    startActivityForResult(
      Intent.createChooser(intent, getString(R.string.choose_attachment)),
      REQUEST_CODE_GET_CONTENT_FOR_SENDING
    )
  }

  private fun initExtras(intent: Intent?) {
    if (intent != null) {
      if (intent.action?.startsWith("android.intent.action") == true) {
        this.extraActionInfo = ExtraActionInfo.parseExtraActionInfo(requireContext(), intent)
        addAtts()
      } else {
        args.incomingMessageInfo?.localFolder?.let {
          this.folderType = FoldersManager.getFolderType(it)
        }

        this.args.serviceInfo?.atts?.let { attachments.addAll(it) }
      }
    }
  }

  private fun initDraftCacheDir(context: Context) {
    draftCacheDir = File(context.cacheDir, Constants.DRAFT_CACHE_DIR)

    if (draftCacheDir.exists()) {
      if (!draftCacheDir.mkdir()) {
        Log.e(TAG, "Create cache directory " + draftCacheDir.name + " failed!")
      }
    }
  }

  private fun addAtts() {
    val sizeWarningMsg = getString(
      R.string.template_warning_max_total_attachments_size,
      FileUtils.byteCountToDisplaySize(Constants.MAX_TOTAL_ATTACHMENT_SIZE_IN_BYTES)
    )

    extraActionInfo?.atts?.forEach { attachmentInfo ->
      if (ContentResolver.SCHEME_FILE.equals(attachmentInfo.uri?.scheme, ignoreCase = true)) {
        // we skip attachments that have SCHEME_FILE as deprecated
        return
      }

      if (hasAbilityToAddAtt(attachmentInfo)) {

        if (attachmentInfo.name.isNullOrEmpty()) {
          val msg = "attachmentInfo.getName() == null, uri = " + attachmentInfo.uri!!
          ExceptionUtil.handleError(NullPointerException(msg))
          return
        }

        val fileName = attachmentInfo.getSafeName()
        var draftAtt = File(draftCacheDir, fileName)

        draftAtt = if (draftAtt.exists()) {
          FileAndDirectoryUtils.createFileWithIncreasedIndex(draftCacheDir, fileName)
        } else {
          draftAtt
        }

        try {
          val inputStream = requireContext().contentResolver.openInputStream(attachmentInfo.uri!!)

          if (inputStream != null) {
            FileUtils.copyInputStreamToFile(inputStream, draftAtt)
            val uri = FileProvider.getUriForFile(
              requireContext(),
              Constants.FILE_PROVIDER_AUTHORITY,
              draftAtt
            )
            attachmentInfo.uri = uri
            attachments.add(attachmentInfo)
          }
        } catch (e: IOException) {
          e.printStackTrace()
          ExceptionUtil.handleError(e)

          if (!draftAtt.delete()) {
            Log.e(TAG, "Delete " + draftAtt.name + " failed!")
          }
        }

      } else {
        Toast.makeText(context, sizeWarningMsg, Toast.LENGTH_SHORT).show()
        return@forEach
      }
    }
  }

  private fun updateRecipients() {
    binding?.editTextRecipientTo?.chipAndTokenValues?.let {
      recipientsViewModel.fetchAndUpdateInfoAboutRecipients(
        Message.RecipientType.TO,
        it
      )
    }

    if (binding?.layoutCc?.isVisible == true) {
      binding?.editTextRecipientCc?.chipAndTokenValues?.let {
        recipientsViewModel.fetchAndUpdateInfoAboutRecipients(
          Message.RecipientType.CC,
          it
        )
      }
    } else {
      binding?.editTextRecipientCc?.setText(null as CharSequence?)
      composeMsgViewModel.replaceRecipients(Message.RecipientType.CC, emptyList())
    }

    if (binding?.layoutBcc?.isVisible == true) {
      binding?.editTextRecipientBcc?.chipAndTokenValues?.let {
        recipientsViewModel.fetchAndUpdateInfoAboutRecipients(
          Message.RecipientType.BCC,
          it
        )
      }
    } else {
      binding?.editTextRecipientBcc?.setText(null as CharSequence?)
      composeMsgViewModel.replaceRecipients(Message.RecipientType.BCC, emptyList())
    }
  }

  /**
   * Run an action to update information about some [RecipientWithPubKeys]s.
   *
   * @param type        A type of recipients
   * @param hasFocus    A value which indicates the view focus.
   * @return A modified recipients list.
   */
  private fun runUpdateActionForRecipients(
    type: Message.RecipientType,
    hasFocus: Boolean,
    isEmpty: Boolean
  ) {
    if (composeMsgViewModel.msgEncryptionType === MessageEncryptionType.ENCRYPTED) {
      if (!hasFocus && isAdded) {
        fetchDetailsAboutRecipients(type)
      }
    }

    if (isEmpty) {
      composeMsgViewModel.replaceRecipients(type, emptyList())
    }
  }

  private fun fetchDetailsAboutRecipients(type: Message.RecipientType) {
    when (type) {
      Message.RecipientType.TO -> {
        binding?.editTextRecipientTo?.chipAndTokenValues?.let {
          recipientsViewModel.fetchAndUpdateInfoAboutRecipients(Message.RecipientType.TO, it)
        }
      }

      Message.RecipientType.CC -> {
        binding?.editTextRecipientCc?.chipAndTokenValues?.let {
          recipientsViewModel.fetchAndUpdateInfoAboutRecipients(Message.RecipientType.CC, it)
        }
      }

      Message.RecipientType.BCC -> {
        binding?.editTextRecipientBcc?.chipAndTokenValues?.let {
          recipientsViewModel.fetchAndUpdateInfoAboutRecipients(Message.RecipientType.BCC, it)
        }
      }
    }
  }

  /**
   * Prepare an alias for the reply. Will be used the email address that the email was received. Will be used the
   * first found matched email.
   *
   * @param aliases A list of Gmail aliases.
   */
  private fun prepareAliasForReplyIfNeeded(aliases: List<String>) {
    val messageEncryptionType = composeMsgViewModel.msgEncryptionType

    val toAddresses: List<InternetAddress>? = if (folderType === FoldersManager.FolderType.SENT) {
      args.incomingMessageInfo?.getFrom()
    } else {
      args.incomingMessageInfo?.getTo()
    }

    if (!CollectionUtils.isEmpty(toAddresses)) {
      var firstFoundedAlias: String? = null
      for (toAddress in toAddresses!!) {
        if (firstFoundedAlias == null) {
          for (alias in aliases) {
            if (alias.equals(toAddress.address, ignoreCase = true)) {
              firstFoundedAlias = if (messageEncryptionType === MessageEncryptionType.ENCRYPTED
                && fromAddressesAdapter?.hasPrvKey(alias) == true
              ) {
                alias
              } else {
                alias
              }
              break
            }
          }
        } else {
          break
        }
      }

      if (firstFoundedAlias != null) {
        val position =
          fromAddressesAdapter?.getPosition(firstFoundedAlias) ?: Spinner.INVALID_POSITION
        if (position != Spinner.INVALID_POSITION) {
          binding?.spinnerFrom?.setSelection(position)
        }
      }
    }
  }

  private fun showFirstMatchedAliasWithPrvKey(aliases: List<String>) {
    var firstFoundedAliasWithPrvKey: String? = null
    for (alias in aliases) {
      if (fromAddressesAdapter?.hasPrvKey(alias) == true) {
        firstFoundedAliasWithPrvKey = alias
        break
      }
    }

    if (firstFoundedAliasWithPrvKey != null) {
      val position =
        fromAddressesAdapter?.getPosition(firstFoundedAliasWithPrvKey) ?: Spinner.INVALID_POSITION
      if (position != Spinner.INVALID_POSITION) {
        binding?.spinnerFrom?.setSelection(position)
      }
    }
  }

  /**
   * Check that all recipients are usable.
   */
  private fun hasUnusableRecipient(): Boolean {
    for (recipient in composeMsgViewModel.recipientWithPubKeys) {
      val recipientWithPubKeys = recipient.recipientWithPubKeys
      if (!recipientWithPubKeys.hasAtLeastOnePubKey()) {
        return if (isPasswordProtectedFunctionalityEnabled()) {
          if (composeMsgViewModel.webPortalPasswordStateFlow.value.isEmpty()) {
            showNoPgpFoundDialog(recipientWithPubKeys)
            true
          } else continue
        } else {
          showNoPgpFoundDialog(recipientWithPubKeys)
          true
        }
      }

      if (!recipientWithPubKeys.hasNotExpiredPubKey()) {
        showInfoDialog(dialogMsg = getString(R.string.warning_one_of_recipients_has_expired_pub_key))
        return true
      }

      if (!recipientWithPubKeys.hasNotRevokedPubKey()) {
        showInfoDialog(dialogMsg = getString(R.string.warning_one_of_recipients_has_revoked_pub_key))
        return true
      }

      if (!recipientWithPubKeys.hasUsablePubKey()) {
        showInfoDialog(dialogMsg = getString(R.string.warning_one_of_recipients_has_not_usable_pub_key))
        return true
      }
    }

    return false
  }

  /**
   * This method does update chips in the recipients field.
   *
   * @param view        A view which contains input [RecipientWithPubKeys](s).
   * @param list The input [RecipientWithPubKeys](s)
   */
  private fun updateChips(
    view: PgpContactsNachoTextView?,
    list: List<RecipientWithPubKeys>?
  ) {
    view ?: return
    val pgpContactChipSpans = view.text.getSpans(0, view.length(), PGPContactChipSpan::class.java)

    if (pgpContactChipSpans.isNotEmpty()) {
      for (recipientWithPubKeys in list ?: emptyList()) {
        for (pgpContactChipSpan in pgpContactChipSpans) {
          if (recipientWithPubKeys.recipient.email.equals(
              pgpContactChipSpan.text.toString(), ignoreCase = true
            )
          ) {
            pgpContactChipSpan.hasAtLeastOnePubKey = recipientWithPubKeys.hasAtLeastOnePubKey()
            pgpContactChipSpan.hasNotExpiredPubKey = recipientWithPubKeys.hasNotExpiredPubKey()
            pgpContactChipSpan.hasUsablePubKey = recipientWithPubKeys.hasUsablePubKey()
            pgpContactChipSpan.hasNotRevokedPubKey = recipientWithPubKeys.hasNotRevokedPubKey()
            break
          }
        }
      }
      view.invalidateChips()
    }
  }

  /**
   * Init an input [NachoTextView] using custom settings.
   *
   * @param pgpContactsNachoTextView An input [NachoTextView]
   */
  private fun initChipsView(pgpContactsNachoTextView: PgpContactsNachoTextView?) {
    pgpContactsNachoTextView?.setNachoValidator(ChipifyingNachoValidator())
    pgpContactsNachoTextView?.setIllegalCharacterIdentifier { character -> character == ',' }
    pgpContactsNachoTextView?.addChipTerminator(
      ' ', ChipTerminatorHandler
        .BEHAVIOR_CHIPIFY_TO_TERMINATOR
    )
    pgpContactsNachoTextView?.chipTokenizer = SpanChipTokenizer(
      requireContext(),
      CustomChipSpanChipCreator(requireContext()), PGPContactChipSpan::class.java
    )
    pgpContactsNachoTextView?.setAdapter(prepareRecipientsAdapter())
    pgpContactsNachoTextView?.onFocusChangeListener = this
    pgpContactsNachoTextView?.setListener(this)
  }

  private fun hasExternalStorageUris(attachmentInfoList: List<AttachmentInfo>?): Boolean {
    attachmentInfoList?.let {
      for (att in it) {
        if (ContentResolver.SCHEME_FILE.equals(att.uri?.scheme, ignoreCase = true)) {
          return true
        }
      }
    }
    return false
  }

  /**
   * Remove the current [RecipientWithPubKeys] from recipients.
   *
   * @param recipientWithPubKeys     The [RecipientWithPubKeys] which will be removed.
   * @param pgpContactsNachoTextView The [NachoTextView] which contains the delete candidate.
   */
  private fun removeRecipientWithPubKey(
    recipientWithPubKeys: RecipientWithPubKeys, pgpContactsNachoTextView: PgpContactsNachoTextView?,
    recipientType: Message.RecipientType
  ) {
    val chipTokenizer = pgpContactsNachoTextView?.chipTokenizer
    pgpContactsNachoTextView?.allChips?.let {
      for (chip in it) {
        if (recipientWithPubKeys.recipient.email.equals(
            chip.text.toString(),
            ignoreCase = true
          ) && chipTokenizer != null
        ) {
          chipTokenizer.deleteChip(chip, pgpContactsNachoTextView.text)
        }
      }
    }

    composeMsgViewModel.removeRecipient(recipientType, recipientWithPubKeys)
  }

  /**
   * Init fragment views
   */
  private fun initViews() {
    initChipsView(binding?.editTextRecipientTo)
    initChipsView(binding?.editTextRecipientCc)
    initChipsView(binding?.editTextRecipientBcc)

    binding?.spinnerFrom?.onItemSelectedListener = this
    binding?.spinnerFrom?.adapter = fromAddressesAdapter

    originalColor = requireNotNull(binding?.editTextFrom?.currentTextColor)

    binding?.imageButtonAliases?.setOnClickListener(this)

    binding?.imageButtonAdditionalRecipientsVisibility?.setOnClickListener(this)

    binding?.editTextEmailSubject?.onFocusChangeListener = this
    binding?.editTextEmailMessage?.onFocusChangeListener = this
    binding?.iBShowQuotedText?.setOnClickListener(this)
    binding?.btnSetWebPortalPassword?.setOnClickListener {
      navController?.navigate(
        CreateMessageFragmentDirections
          .actionCreateMessageFragmentToProvidePasswordToProtectMsgFragment(
            composeMsgViewModel.webPortalPasswordStateFlow.value.toString()
          )
      )
    }
  }

  private fun showContent() {
    if ((args.incomingMessageInfo != null || extraActionInfo != null) && !isIncomingMsgInfoUsed) {
      this.isIncomingMsgInfoUsed = true
      updateViews()
    }

    showAtts()
  }

  /**
   * Update views on the screen. This method can be called when we need to update the current
   * screen.
   */
  private fun updateViews() {
    onMsgEncryptionTypeChange(composeMsgViewModel.msgEncryptionType)

    if (extraActionInfo != null) {
      updateViewsFromExtraActionInfo()
    } else {
      if (args.incomingMessageInfo != null) {
        updateViewsFromIncomingMsgInfo()
        binding?.editTextRecipientTo?.chipifyAllUnterminatedTokens()
        binding?.editTextRecipientCc?.chipifyAllUnterminatedTokens()
        binding?.editTextEmailSubject?.setText(
          prepareReplySubject(
            args.incomingMessageInfo?.getSubject() ?: ""
          )
        )
      }

      if (args.serviceInfo != null) {
        updateViewsFromServiceInfo()
      }
    }
  }

  private fun updateViewsFromExtraActionInfo() {
    setupPgpFromExtraActionInfo(
      binding?.editTextRecipientTo,
      extraActionInfo?.toAddresses?.toTypedArray()
    )
    setupPgpFromExtraActionInfo(
      binding?.editTextRecipientCc,
      extraActionInfo?.ccAddresses?.toTypedArray()
    )
    setupPgpFromExtraActionInfo(
      binding?.editTextRecipientBcc,
      extraActionInfo?.bccAddresses?.toTypedArray()
    )

    binding?.editTextEmailSubject?.setText(extraActionInfo?.subject)
    binding?.editTextEmailMessage?.setText(extraActionInfo?.body)

    if (binding?.editTextRecipientTo?.text?.isEmpty() == true) {
      binding?.editTextRecipientTo?.requestFocus()
      return
    }

    if (binding?.editTextEmailSubject?.text?.isEmpty() == true) {
      binding?.editTextEmailSubject?.requestFocus()
      return
    }

    binding?.editTextEmailMessage?.requestFocus()
    binding?.editTextEmailMessage?.showKeyboard()
  }

  private fun updateViewsFromServiceInfo() {
    binding?.editTextRecipientTo?.isFocusable = args.serviceInfo?.isToFieldEditable ?: false
    binding?.editTextRecipientTo?.isFocusableInTouchMode =
      args.serviceInfo?.isToFieldEditable ?: false
    //todo-denbond7 Need to add a similar option for editTextRecipientCc and editTextRecipientBcc

    binding?.editTextEmailSubject?.isFocusable = args.serviceInfo?.isSubjectEditable ?: false
    binding?.editTextEmailSubject?.isFocusableInTouchMode =
      args.serviceInfo?.isSubjectEditable ?: false

    binding?.editTextEmailMessage?.isFocusable = args.serviceInfo?.isMsgEditable ?: false
    binding?.editTextEmailMessage?.isFocusableInTouchMode = args.serviceInfo?.isMsgEditable ?: false

    if (args.serviceInfo?.systemMsg?.isNotEmpty() == true) {
      binding?.editTextEmailMessage?.setText(args.serviceInfo?.systemMsg)
    }
  }

  private fun updateViewsFromIncomingMsgInfo() {
    binding?.iBShowQuotedText?.visible()
    binding?.iBShowQuotedText?.let { registerForContextMenu(it) }

    when (args.messageType) {
      MessageType.REPLY -> updateViewsIfReplyMode()

      MessageType.REPLY_ALL -> updateViewsIfReplyAllMode()

      MessageType.FORWARD -> updateViewsIfFwdMode()
    }

    updateRecipients()
  }

  private fun updateViewsIfFwdMode() {
    val originalMsgInfo = args.incomingMessageInfo ?: return

    if (!CollectionUtils.isEmpty(originalMsgInfo.atts)) {
      for (att in originalMsgInfo.atts!!) {
        if (hasAbilityToAddAtt(att)) {
          attachments.add(att)
        } else {
          showInfoSnackbar(
            requireView(), getString(
              R.string.template_warning_max_total_attachments_size,
              FileUtils.byteCountToDisplaySize(Constants.MAX_TOTAL_ATTACHMENT_SIZE_IN_BYTES)
            ),
            Snackbar.LENGTH_LONG
          )
        }
      }
    }

    binding?.editTextEmailMessage?.setText(
      getString(
        R.string.forward_template,
        originalMsgInfo.getFrom().first().address ?: "",
        EmailUtil.genForwardedMsgDate(originalMsgInfo.getReceiveDate()),
        originalMsgInfo.getSubject(),
        prepareRecipientsLineForForwarding(originalMsgInfo.getTo())
      )
    )

    if (!CollectionUtils.isEmpty(originalMsgInfo.getCc())) {
      binding?.editTextEmailMessage?.append("Cc: ")
      binding?.editTextEmailMessage?.append(prepareRecipientsLineForForwarding(originalMsgInfo.getCc()))
      binding?.editTextEmailMessage?.append("\n\n")
    }

    binding?.editTextEmailMessage?.append("\n\n" + originalMsgInfo.text)
  }

  private fun updateViewsIfReplyAllMode() {
    when (folderType) {
      FoldersManager.FolderType.SENT, FoldersManager.FolderType.OUTBOX -> {
        binding?.editTextRecipientTo?.setText(prepareRecipients(args.incomingMessageInfo?.getTo()))

        if (args.incomingMessageInfo?.getCc()?.isNotEmpty() == true) {
          binding?.layoutCc?.visibility = View.VISIBLE
          binding?.editTextRecipientCc?.append(prepareRecipients(args.incomingMessageInfo?.getCc()))
        }
      }

      else -> {
        val toRecipients =
          if (args.incomingMessageInfo?.getReplyToWithoutOwnerAddress().isNullOrEmpty()) {
            args.incomingMessageInfo?.getTo() ?: emptyList()
          } else {
            args.incomingMessageInfo?.getReplyToWithoutOwnerAddress() ?: emptyList()
          }

        binding?.editTextRecipientTo?.setText(prepareRecipients(toRecipients))

        val ccSet = HashSet<InternetAddress>()

        if (args.incomingMessageInfo?.getTo()?.isNotEmpty() == true) {
          for (address in args.incomingMessageInfo?.getTo() ?: emptyList()) {
            if (!account?.email.equals(address.address, ignoreCase = true)) {
              ccSet.add(address)
            }
          }

          accountAliasesViewModel.accountAliasesLiveData.value?.let {
            for (alias in it) {
              val iterator = ccSet.iterator()

              while (iterator.hasNext()) {
                if (iterator.next().address.equals(alias.sendAsEmail, ignoreCase = true)) {
                  iterator.remove()
                }
              }
            }
          }
        }

        ccSet.removeAll(toRecipients.toSet())

        if (args.incomingMessageInfo?.getCc()?.isNotEmpty() == true) {
          for (address in args.incomingMessageInfo?.getCc() ?: emptyList()) {
            if (!account?.email.equals(address.address, ignoreCase = true)) {
              ccSet.add(address)
            }
          }
        }

        //here we remove the owner address
        val fromAddress = args.incomingMessageInfo?.msgEntity?.email
        val finalCcSet = ccSet.filter { fromAddress?.equals(it.address, true) != true }

        if (finalCcSet.isNotEmpty()) {
          binding?.layoutCc?.visible()
          val ccRecipients = prepareRecipients(finalCcSet)
          binding?.editTextRecipientCc?.append(ccRecipients)
        }
      }
    }

    if (binding?.editTextRecipientTo?.text?.isNotEmpty() == true
      || binding?.editTextRecipientCc?.text?.isNotEmpty() == true
    ) {
      binding?.editTextEmailMessage?.requestFocus()
      binding?.editTextEmailMessage?.showKeyboard()
    }
  }

  private fun updateViewsIfReplyMode() {
    when (folderType) {
      FoldersManager.FolderType.SENT,
      FoldersManager.FolderType.OUTBOX -> {
        binding?.editTextRecipientTo?.setText(prepareRecipients(args.incomingMessageInfo?.getTo()))
      }

      else -> binding?.editTextRecipientTo?.setText(
        prepareRecipients(
          if (args.incomingMessageInfo?.getReplyToWithoutOwnerAddress().isNullOrEmpty()) {
            args.incomingMessageInfo?.getTo()
          } else {
            args.incomingMessageInfo?.getReplyToWithoutOwnerAddress()
          }
        )
      )
    }

    if (binding?.editTextRecipientTo?.text?.isNotEmpty() == true) {
      binding?.editTextEmailMessage?.requestFocus()
      binding?.editTextEmailMessage?.showKeyboard()
    }
  }

  private fun setupPgpFromExtraActionInfo(
    pgpContactsNachoTextView: PgpContactsNachoTextView?,
    addresses: Array<String>?
  ) {
    if (addresses?.isNotEmpty() == true) {
      pgpContactsNachoTextView?.setText(prepareRecipients(addresses))
      pgpContactsNachoTextView?.chipifyAllUnterminatedTokens()
      pgpContactsNachoTextView?.onFocusChangeListener?.onFocusChange(
        pgpContactsNachoTextView,
        false
      )
    }
  }

  private fun prepareRecipientsLineForForwarding(recipients: List<InternetAddress>?): String {
    val stringBuilder = StringBuilder()
    return if (!CollectionUtils.isEmpty(recipients)) {
      stringBuilder.append(recipients!![0])

      if (recipients.size > 1) {
        for (i in 1 until recipients.size) {
          val recipient = recipients[0].address
          stringBuilder.append(", ")
          stringBuilder.append(recipient)
        }
      }

      stringBuilder.toString()
    } else
      ""
  }

  private fun prepareReplySubject(subject: String): String {
    val prefix = when (args.messageType) {
      MessageType.REPLY, MessageType.REPLY_ALL -> "Re"
      MessageType.FORWARD -> "Fwd"
      else -> return subject
    }
    val prefixMatcher = Pattern.compile("^($prefix: )", Pattern.CASE_INSENSITIVE).matcher(subject)
    return if (prefixMatcher.find()) subject else getString(
      R.string.template_reply_subject,
      prefix,
      subject
    )
  }

  private fun prepareRecipients(recipients: Array<String>?): String {
    val stringBuilder = StringBuilder()
    if (recipients != null && recipients.isNotEmpty()) {
      for (s in recipients) {
        stringBuilder.append(s).append(" ")
      }
    }

    return stringBuilder.toString()
  }

  private fun prepareRecipients(recipients: Collection<InternetAddress>?): String {
    val stringBuilder = StringBuilder()
    if (!CollectionUtils.isEmpty(recipients)) {
      for (s in recipients!!) {
        stringBuilder.append(s.address).append(" ")
      }
    }

    return stringBuilder.toString()
  }

  /**
   * Prepare a [RecipientAdapter] for the [NachoTextView] object.
   *
   * @return <tt>[RecipientAdapter]</tt>
   */
  @SuppressLint("Recycle")
  private fun prepareRecipientsAdapter(): RecipientAdapter {
    val pgpContactAdapter = RecipientAdapter(requireContext(), null, true)
    //setup a search contacts logic in the database
    pgpContactAdapter.filterQueryProvider = FilterQueryProvider { constraint ->
      val dao = FlowCryptRoomDatabase.getDatabase(requireContext()).recipientDao()
      dao.getFilteredCursor("%$constraint%")
    }

    return pgpContactAdapter
  }

  /**
   * Check if the given [pgpContactsNachoTextViews] List has an invalid email.
   *
   * @return <tt>boolean</tt> true - if has, otherwise false..
   */
  private fun hasInvalidEmail(vararg pgpContactsNachoTextViews: PgpContactsNachoTextView?): Boolean {
    for (textView in pgpContactsNachoTextViews) {
      val emails = textView?.chipAndTokenValues
      if (emails != null) {
        for (email in emails) {
          if (!GeneralUtil.isEmailValid(email)) {
            showInfoSnackbar(textView, getString(R.string.error_some_email_is_not_valid, email))
            textView.requestFocus()
            return true
          }
        }
      }
    }
    return false
  }

  /**
   * Check is attachment can be added to the current message.
   *
   * @param newAttInfo The new attachment which will be maybe added.
   * @return true if the attachment can be added, otherwise false.
   */
  private fun hasAbilityToAddAtt(newAttInfo: AttachmentInfo?): Boolean {
    val existedAttsSize = (attachments.map { it.encodedSize.toInt() }.sum())
    val newAttSize = newAttInfo?.encodedSize?.toInt() ?: 0
    return (existedAttsSize + newAttSize) < Constants.MAX_TOTAL_ATTACHMENT_SIZE_IN_BYTES
  }


  /**
   * Show a dialog where we can select different actions.
   *
   * @param recipient The [RecipientWithPubKeys] which will be used when we select the remove action.
   */
  private fun showNoPgpFoundDialog(recipient: RecipientWithPubKeys) {
    val dialogFragment = NoPgpFoundDialogFragment.newInstance(
      RecipientWithPubKeys = recipient,
      isRemoveActionEnabled = true,
      isProtectingWithPasswordEnabled = isPasswordProtectedFunctionalityEnabled()
    )
    dialogFragment.setTargetFragment(this, REQUEST_CODE_NO_PGP_FOUND_DIALOG)
    dialogFragment.show(parentFragmentManager, NoPgpFoundDialogFragment::class.java.simpleName)
  }

  /**
   * Send a message.
   */
  private fun sendMsg() {
    dismissCurrentSnackBar()
    val outgoingMessageInfo = getOutgoingMsgInfo()
    PrepareOutgoingMessagesJobIntentService.enqueueWork(requireContext(), outgoingMessageInfo)
    toast(
      if (GeneralUtil.isConnected(requireContext()))
        R.string.sending
      else
        R.string.no_conn_msg_sent_later
    )
    activity?.finish()
  }

  /**
   * Show attachments which were added.
   */
  private fun showAtts() {
    if (attachments.isNotEmpty()) {
      binding?.layoutAtts?.removeAllViews()
      val layoutInflater = LayoutInflater.from(context)
      for (att in attachments) {
        val rootView = layoutInflater.inflate(R.layout.attachment_item, binding?.layoutAtts, false)

        val textViewAttName = rootView.findViewById<TextView>(R.id.textViewAttachmentName)
        textViewAttName.text = att.name

        val textViewAttSize = rootView.findViewById<TextView>(R.id.textViewAttSize)
        if (att.encodedSize > 0) {
          textViewAttSize.visibility = View.VISIBLE
          textViewAttSize.text = Formatter.formatFileSize(context, att.encodedSize)
        } else {
          textViewAttSize.visibility = View.GONE
        }

        val imageButtonDownloadAtt = rootView.findViewById<View>(R.id.imageButtonDownloadAtt)
        rootView.findViewById<View>(R.id.imageButtonPreviewAtt)?.visibility = View.GONE

        if (!att.isProtected) {
          imageButtonDownloadAtt.visibility = View.GONE
          val imageButtonClearAtt = rootView.findViewById<View>(R.id.imageButtonClearAtt)
          imageButtonClearAtt.visibility = View.VISIBLE
          imageButtonClearAtt.setOnClickListener {
            attachments.remove(att)
            binding?.layoutAtts?.removeView(rootView)

            //Remove a temp file which was created by our app
            val uri = att.uri
            if (uri != null && Constants.FILE_PROVIDER_AUTHORITY.equals(
                uri.authority!!,
                ignoreCase = true
              )
            ) {
              context?.contentResolver?.delete(uri, null, null)
            }
          }
        } else {
          imageButtonDownloadAtt.visibility = View.INVISIBLE
        }
        binding?.layoutAtts?.addView(rootView)
      }
    } else {
      binding?.layoutAtts?.removeAllViews()
    }
  }

  /**
   * Show a dialog where the user can select a public key which will be attached to a message.
   */
  private fun showPubKeyDialog() {
    if (!account?.email.isNullOrEmpty()) {
      val fragment = ChoosePublicKeyDialogFragment.newInstance(
        account?.email!!, ListView.CHOICE_MODE_SINGLE, R.plurals.choose_pub_key, true
      )
      fragment.setTargetFragment(this@CreateMessageFragment, REQUEST_CODE_SHOW_PUB_KEY_DIALOG)
      fragment.show(parentFragmentManager, ChoosePublicKeyDialogFragment::class.java.simpleName)
    }
  }

  private fun setupAccountAliasesViewModel() {
    accountAliasesViewModel.fetchUpdates(viewLifecycleOwner)
    accountAliasesViewModel.accountAliasesLiveData.observe(viewLifecycleOwner) {
      val aliases = ArrayList<String>()
      accountAliasesViewModel.activeAccountLiveData.value?.let { accountEntity ->
        aliases.add(accountEntity.email)
      }

      for (accountAlias in it) {
        aliases.add(accountAlias.sendAsEmail)
      }

      fromAddressesAdapter?.clear()
      fromAddressesAdapter?.addAll(aliases)

      updateFromAddressAdapter(KeysStorageImpl.getInstance(requireContext()).getPGPSecretKeyRings())

      if (args.incomingMessageInfo != null) {
        prepareAliasForReplyIfNeeded(aliases)
      } else if (composeMsgViewModel.msgEncryptionType === MessageEncryptionType.ENCRYPTED) {
        showFirstMatchedAliasWithPrvKey(aliases)
      }

      if (args.serviceInfo != null) {
        args.serviceInfo?.let { serviceInfo ->
          if (serviceInfo.isFromFieldEditable) {
            binding?.imageButtonAliases?.visibility = View.VISIBLE
          } else {
            binding?.imageButtonAliases?.visibility = View.INVISIBLE
          }
        }
      } else {
        fromAddressesAdapter?.count?.let { count: Int ->
          if (count in 0..1) {
            if (binding?.imageButtonAliases?.visibility == View.VISIBLE) {
              binding?.imageButtonAliases?.visibility = View.INVISIBLE
            }
          } else {
            binding?.imageButtonAliases?.visibility = View.VISIBLE
          }
        }
      }

      showContent()
    }
  }

  private fun setupPrivateKeysViewModel() {
    KeysStorageImpl.getInstance(requireContext()).secretKeyRingsLiveData
      .observe(viewLifecycleOwner, { updateFromAddressAdapter(it) })
  }

  private fun updateFromAddressAdapter(list: List<PGPSecretKeyRing>) {
    val setOfUsers = list.map { keyRing -> keyRing.toPgpKeyDetails().mimeAddresses }
      .flatten()
      .map { mimeAddress -> mimeAddress.address }

    fromAddressesAdapter?.let { adapter ->
      for (email in adapter.objects) {
        adapter.updateKeyAvailability(email, setOfUsers.contains(email))
      }
    }
  }

  private fun setupRecipientsViewModel() {
    handleUpdatingRecipients(
      recipientsViewModel.recipientsToLiveData,
      Message.RecipientType.TO,
      binding?.progressBarTo,
      binding?.editTextRecipientTo
    ) {
      isUpdateToCompleted = it
    }

    handleUpdatingRecipients(
      recipientsViewModel.recipientsCcLiveData,
      Message.RecipientType.CC,
      binding?.progressBarCc,
      binding?.editTextRecipientCc
    ) {
      isUpdateCcCompleted = it
    }

    handleUpdatingRecipients(
      recipientsViewModel.recipientsBccLiveData,
      Message.RecipientType.BCC,
      binding?.progressBarBcc,
      binding?.editTextRecipientBcc
    ) {
      isUpdateBccCompleted = it
    }
  }

  private fun handleUpdatingRecipients(
    liveData: LiveData<Result<List<RecipientWithPubKeys>>>,
    recipientType: Message.RecipientType,
    progressBar: ProgressBar?,
    nachoTextView: PgpContactsNachoTextView?,
    updateState: (state: Boolean) -> Unit
  ) {
    liveData.observe(viewLifecycleOwner) {
      when (it.status) {
        Result.Status.LOADING -> {
          updateState.invoke(false)
          countingIdlingResource?.incrementSafely()
          progressBar?.visible()
        }

        Result.Status.SUCCESS -> {
          updateState.invoke(true)
          progressBar?.invisible()
          it.data?.let { list ->
            composeMsgViewModel.replaceRecipients(recipientType, list)
          }
          countingIdlingResource?.decrementSafely()
        }

        Result.Status.ERROR, Result.Status.EXCEPTION -> {
          updateState.invoke(true)
          progressBar?.invisible()
          showInfoSnackbar(view, it.exception?.message ?: getString(R.string.unknown_error))
          countingIdlingResource?.decrementSafely()
        }

        Result.Status.NONE -> {}
      }
    }
  }

  /**
   * Add [AttachmentInfo] that was created from the given [Uri]
   *
   * Due to accessing to the [Uri] out of the current [Activity] we should use
   * [ContentResolver.takePersistableUriPermission] in order to persist the
   * permission.
   *
   * Because access to the uri is granted until the receiving Activity is
   * finished. We can extend the lifetime of the permission grant by passing
   * it along to another Android component. This is done by including the uri
   * in the data field or the ClipData object of the Intent used to launch that
   * component. Additionally, we need to add [Intent.FLAG_GRANT_READ_URI_PERMISSION] to the Intent.
   */
  private fun addAttachmentInfoFromIntent(intent: Intent?) {
    val uri = intent?.data
    if (uri != null) {
      val attachmentInfo = EmailUtil.getAttInfoFromUri(context, uri)
      if (hasAbilityToAddAtt(attachmentInfo)) {
        try {
          context?.contentResolver?.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION
          )
        } catch (e: Exception) {
          showInfoSnackbar(view, getString(R.string.can_not_attach_this_file), Snackbar.LENGTH_LONG)
          return
        }
        attachmentInfo?.let { attachments.add(it) }
        showAtts()
      } else {
        showInfoSnackbar(
          view, getString(
            R.string.template_warning_max_total_attachments_size,
            FileUtils.byteCountToDisplaySize(Constants.MAX_TOTAL_ATTACHMENT_SIZE_IN_BYTES)
          ),
          Snackbar.LENGTH_LONG
        )
      }
    } else {
      showInfoSnackbar(view, getString(R.string.can_not_attach_this_file), Snackbar.LENGTH_LONG)
    }
  }

  private fun setupComposeMsgViewModel() {
    lifecycleScope.launchWhenStarted {
      composeMsgViewModel.messageEncryptionTypeStateFlow.collect {
        when (it) {
          MessageEncryptionType.ENCRYPTED -> {
            appBarLayout?.setBackgroundColor(
              UIUtil.getColor(requireContext(), R.color.colorPrimary)
            )
            nonEncryptedHintView?.gone()
          }

          MessageEncryptionType.STANDARD -> {
            appBarLayout?.setBackgroundColor(UIUtil.getColor(requireContext(), R.color.red))
            nonEncryptedHintView?.visible()
            binding?.btnSetWebPortalPassword?.gone()
            composeMsgViewModel.setWebPortalPassword()
          }
        }

        activity?.invalidateOptionsMenu()
        onMsgEncryptionTypeChange(it)
      }
    }

    lifecycleScope.launchWhenStarted {
      composeMsgViewModel.recipientsStateFlow.collect { recipients ->
        if (isPasswordProtectedFunctionalityEnabled()) {
          val hasRecipientsWithoutPgp =
            recipients.any { recipient -> !recipient.recipientWithPubKeys.hasAtLeastOnePubKey() }
          if (hasRecipientsWithoutPgp) {
            binding?.btnSetWebPortalPassword?.visible()
          } else {
            binding?.btnSetWebPortalPassword?.gone()
            composeMsgViewModel.setWebPortalPassword()
          }
        }
      }
    }

    lifecycleScope.launchWhenStarted {
      composeMsgViewModel.recipientsToStateFlow.collect { recipients ->
        updateChips(binding?.editTextRecipientTo, recipients.map { it.recipientWithPubKeys })
      }
    }

    lifecycleScope.launchWhenStarted {
      composeMsgViewModel.recipientsCcStateFlow.collect { recipients ->
        binding?.layoutCc?.visibleOrGone(recipients.isNotEmpty())
        updateChips(binding?.editTextRecipientCc, recipients.map { it.recipientWithPubKeys })
      }
    }

    lifecycleScope.launchWhenStarted {
      composeMsgViewModel.recipientsBccStateFlow.collect { recipients ->
        binding?.layoutBcc?.visibleOrGone(recipients.isNotEmpty())
        updateChips(binding?.editTextRecipientBcc, recipients.map { it.recipientWithPubKeys })
      }
    }

    lifecycleScope.launchWhenStarted {
      composeMsgViewModel.webPortalPasswordStateFlow.collect { webPortalPassword ->
        if (isPasswordProtectedFunctionalityEnabled()) {
          binding?.btnSetWebPortalPassword?.apply {
            if (webPortalPassword.isEmpty()) {
              setCompoundDrawablesWithIntrinsicBounds(
                R.drawable.ic_password_not_protected_white_24,
                0,
                0,
                0
              )
              setText(R.string.tap_to_protect_with_web_portal_password)
              background?.colorFilter = BlendModeColorFilterCompat.createBlendModeColorFilterCompat(
                ContextCompat.getColor(context, R.color.orange), BlendModeCompat.MODULATE
              )
            } else {
              setCompoundDrawablesWithIntrinsicBounds(
                R.drawable.ic_password_protected_white_24,
                0,
                0,
                0
              )
              setText(R.string.web_portal_password_added)
              background?.colorFilter = BlendModeColorFilterCompat.createBlendModeColorFilterCompat(
                ContextCompat.getColor(context, R.color.colorPrimary), BlendModeCompat.MODULATE
              )
            }
          }
        }
      }
    }
  }

  private fun initNonEncryptedHintView() {
    nonEncryptedHintView =
      layoutInflater.inflate(R.layout.under_toolbar_line_with_text, appBarLayout, false)
    val textView = nonEncryptedHintView?.findViewById<TextView>(R.id.underToolbarTextTextView)
    textView?.setText(R.string.this_message_will_not_be_encrypted)
  }

  private fun updateActionBar() {
    when (args.messageType) {
      MessageType.NEW -> supportActionBar?.setTitle(R.string.compose)
      MessageType.REPLY -> supportActionBar?.setTitle(R.string.reply)
      MessageType.REPLY_ALL -> supportActionBar?.setTitle(R.string.reply_all)
      MessageType.FORWARD -> supportActionBar?.setTitle(R.string.forward)
    }

    appBarLayout?.addView(nonEncryptedHintView)
  }

  /**
   * Do a lot of checks to validate an outgoing message info.
   *
   * @return true if all information is correct, false otherwise.
   */
  private fun isDataCorrect(): Boolean {
    binding?.editTextRecipientTo?.chipifyAllUnterminatedTokens()
    binding?.editTextRecipientCc?.chipifyAllUnterminatedTokens()
    binding?.editTextRecipientBcc?.chipifyAllUnterminatedTokens()
    if (fromAddressesAdapter?.isEnabled(
        binding?.spinnerFrom?.selectedItemPosition ?: Spinner.INVALID_POSITION
      ) == false
    ) {
      showInfoSnackbar(binding?.editTextRecipientTo, getString(R.string.no_key_available))
      return false
    }
    if (binding?.editTextRecipientTo?.text?.isEmpty() == true) {
      showInfoSnackbar(
        binding?.editTextRecipientTo, getString(
          R.string.text_must_not_be_empty,
          getString(R.string.prompt_recipients_to)
        )
      )
      binding?.editTextRecipientTo?.requestFocus()
      return false
    }
    if (hasInvalidEmail(
        binding?.editTextRecipientTo,
        binding?.editTextRecipientCc,
        binding?.editTextRecipientBcc
      )
    ) {
      return false
    }
    if (composeMsgViewModel.msgEncryptionType === MessageEncryptionType.ENCRYPTED) {
      if (binding?.editTextRecipientTo?.text?.isNotEmpty() == true
        && composeMsgViewModel.recipientWithPubKeysTo.isEmpty()
      ) {
        fetchDetailsAboutRecipients(Message.RecipientType.TO)
        return false
      }
      if (binding?.editTextRecipientCc?.text?.isNotEmpty() == true
        && composeMsgViewModel.recipientWithPubKeysCc.isEmpty()
      ) {
        fetchDetailsAboutRecipients(Message.RecipientType.CC)
        return false
      }
      if (binding?.editTextRecipientBcc?.text?.isNotEmpty() == true
        && composeMsgViewModel.recipientWithPubKeysBcc.isEmpty()
      ) {
        fetchDetailsAboutRecipients(Message.RecipientType.BCC)
        return false
      }
      if (hasUnusableRecipient()) {
        return false
      }

      if (isPasswordProtectedFunctionalityEnabled()) {
        val password = composeMsgViewModel.webPortalPasswordStateFlow.value
        if (password.isNotEmpty()) {
          val keysStorage = KeysStorageImpl.getInstance(requireContext())
          if (keysStorage.hasPassphrase(Passphrase(password.toString().toCharArray()))) {
            showInfoDialog(
              dialogTitle = getString(R.string.warning),
              dialogMsg = getString(R.string.warning_use_private_key_pass_phrase_as_password)
            )
            return false
          }

          if (binding?.editTextEmailSubject?.text.toString() == password.toString()) {
            showInfoDialog(
              dialogTitle = getString(R.string.warning),
              dialogMsg = getString(
                R.string.warning_use_subject_as_password,
                getString(R.string.app_name)
              )
            )
            return false
          }
        }
      }
    }
    if (binding?.editTextEmailSubject?.text?.isEmpty() == true) {
      showInfoSnackbar(
        binding?.editTextEmailSubject, getString(
          R.string.text_must_not_be_empty,
          getString(R.string.prompt_subject)
        )
      )
      binding?.editTextEmailSubject?.requestFocus()
      return false
    }
    if (attachments.isEmpty() && binding?.editTextEmailMessage?.text?.isEmpty() == true) {
      showInfoSnackbar(
        binding?.editTextEmailMessage,
        getString(R.string.sending_message_must_not_be_empty)
      )
      binding?.editTextEmailMessage?.requestFocus()
      return false
    }
    if (attachments.isEmpty() || !hasExternalStorageUris(this.attachments)) {
      return true
    }
    return false
  }

  private fun getOutgoingMsgInfo(): OutgoingMessageInfo {
    var msg = binding?.editTextEmailMessage?.text.toString()
    if (args.messageType == MessageType.REPLY || args.messageType == MessageType.REPLY_ALL) {
      if (binding?.iBShowQuotedText?.visibility == View.VISIBLE) {
        msg += EmailUtil.genReplyContent(args.incomingMessageInfo)
      }
    }

    val attachments = attachments.minus(getForwardedAttachments().toSet())
    attachments.forEachIndexed { index, attachmentInfo -> attachmentInfo.path = index.toString() }

    return OutgoingMessageInfo(
      account = accountViewModel.activeAccountLiveData.value?.email ?: "",
      subject = binding?.editTextEmailSubject?.text.toString(),
      msg = msg,
      toRecipients = binding?.editTextRecipientTo?.chipValues?.map { InternetAddress(it) }
        ?: emptyList(),
      ccRecipients = binding?.editTextRecipientCc?.chipValues?.map { InternetAddress(it) },
      bccRecipients = binding?.editTextRecipientBcc?.chipValues?.map { InternetAddress(it) },
      from = InternetAddress(binding?.editTextFrom?.text.toString()),
      atts = attachments,
      forwardedAtts = getForwardedAttachments(),
      encryptionType = composeMsgViewModel.msgEncryptionType,
      messageType = args.messageType,
      replyToMsgEntity = args.incomingMessageInfo?.msgEntity,
      uid = EmailUtil.genOutboxUID(requireContext()),
      password = usePasswordIfNeeded()
    )
  }

  private fun usePasswordIfNeeded(): CharArray? {
    return if (isPasswordProtectedFunctionalityEnabled()) {
      for (recipient in composeMsgViewModel.recipientWithPubKeys) {
        val recipientWithPubKeys = recipient.recipientWithPubKeys
        if (!recipientWithPubKeys.hasAtLeastOnePubKey()) {
          return composeMsgViewModel.webPortalPasswordStateFlow.value.toString().toCharArray()
        }
      }
      null
    } else null
  }

  private fun isPasswordProtectedFunctionalityEnabled(): Boolean {
    return accountViewModel.activeAccountLiveData.value?.useFES ?: return false
  }

  private fun getForwardedAttachments(): List<AttachmentInfo> {
    return attachments.filter { it.id != null && it.isForwarded }
  }

  private fun subscribeToSetWebPortalPassword() {
    setFragmentResultListener(
      ProvidePasswordToProtectMsgFragment.REQUEST_KEY_PASSWORD
    ) { _, bundle ->
      val password =
        bundle.getCharSequence(ProvidePasswordToProtectMsgFragment.KEY_PASSWORD) ?: ""
      composeMsgViewModel.setWebPortalPassword(password)
    }
  }

  private fun subscribeToSelectRecipients() {
    setFragmentResultListener(
      SelectRecipientsFragment.REQUEST_KEY_SELECT_RECIPIENTS
    ) { _, bundle ->
      val list =
        bundle.getParcelableArrayList<RecipientEntity>(SelectRecipientsFragment.KEY_RECIPIENTS)
      list?.let { recipients ->
        val recipientEntity = recipients.firstOrNull() ?: return@let
        recipientsViewModel.copyPubKeysBetweenRecipients(
          recipientEntity,
          cachedRecipientWithoutPubKeys?.recipient
        )

        updateRecipients()
        updateChips(binding?.editTextRecipientTo,
          composeMsgViewModel.recipientWithPubKeysTo.map { it.recipientWithPubKeys })
        updateChips(
          binding?.editTextRecipientCc,
          composeMsgViewModel.recipientWithPubKeysCc.map { it.recipientWithPubKeys })
        updateChips(
          binding?.editTextRecipientBcc,
          composeMsgViewModel.recipientWithPubKeysBcc.map { it.recipientWithPubKeys })

        Toast.makeText(context, R.string.key_successfully_copied, Toast.LENGTH_LONG).show()

        cachedRecipientWithoutPubKeys = null
      }
    }
  }

  companion object {
    private const val REQUEST_CODE_NO_PGP_FOUND_DIALOG = 100
    private const val REQUEST_CODE_GET_CONTENT_FOR_SENDING = 102
    private const val REQUEST_CODE_SHOW_PUB_KEY_DIALOG = 106
    private const val REQUEST_CODE_SHOW_FIX_EMPTY_PASSPHRASE_DIALOG = 107
    private val TAG = CreateMessageFragment::class.java.simpleName
  }
}
