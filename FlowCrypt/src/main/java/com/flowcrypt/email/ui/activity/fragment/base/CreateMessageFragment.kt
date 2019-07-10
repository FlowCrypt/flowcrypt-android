/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment.base

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.TextUtils
import android.text.format.Formatter
import android.util.Log
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
import android.widget.EditText
import android.widget.FilterQueryProvider
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.loader.app.LoaderManager
import androidx.loader.content.Loader
import com.flowcrypt.email.Constants
import com.flowcrypt.email.R
import com.flowcrypt.email.api.email.EmailUtil
import com.flowcrypt.email.api.email.FoldersManager
import com.flowcrypt.email.api.email.model.AttachmentInfo
import com.flowcrypt.email.api.email.model.ExtraActionInfo
import com.flowcrypt.email.api.email.model.IncomingMessageInfo
import com.flowcrypt.email.api.email.model.OutgoingMessageInfo
import com.flowcrypt.email.api.email.model.ServiceInfo
import com.flowcrypt.email.database.dao.source.AccountAliasesDao
import com.flowcrypt.email.database.dao.source.AccountAliasesDaoSource
import com.flowcrypt.email.database.dao.source.AccountDao
import com.flowcrypt.email.database.dao.source.AccountDaoSource
import com.flowcrypt.email.database.dao.source.ContactsDaoSource
import com.flowcrypt.email.database.dao.source.UserIdEmailsKeysDaoSource
import com.flowcrypt.email.model.MessageEncryptionType
import com.flowcrypt.email.model.MessageType
import com.flowcrypt.email.model.PgpContact
import com.flowcrypt.email.model.UpdateInfoAboutPgpContactsResult
import com.flowcrypt.email.model.results.LoaderResult
import com.flowcrypt.email.ui.activity.CreateMessageActivity
import com.flowcrypt.email.ui.activity.ImportPublicKeyActivity
import com.flowcrypt.email.ui.activity.SelectContactsActivity
import com.flowcrypt.email.ui.activity.fragment.dialog.NoPgpFoundDialogFragment
import com.flowcrypt.email.ui.activity.listeners.OnChangeMessageEncryptionTypeListener
import com.flowcrypt.email.ui.adapter.FromAddressesAdapter
import com.flowcrypt.email.ui.adapter.PgpContactAdapter
import com.flowcrypt.email.ui.loader.LoadGmailAliasesLoader
import com.flowcrypt.email.ui.loader.UpdateInfoAboutPgpContactsAsyncTaskLoader
import com.flowcrypt.email.ui.widget.CustomChipSpanChipCreator
import com.flowcrypt.email.ui.widget.PGPContactChipSpan
import com.flowcrypt.email.ui.widget.PgpContactsNachoTextView
import com.flowcrypt.email.ui.widget.SingleCharacterSpanChipTokenizer
import com.flowcrypt.email.util.GeneralUtil
import com.flowcrypt.email.util.UIUtil
import com.flowcrypt.email.util.exception.ExceptionUtil
import com.google.android.gms.common.util.CollectionUtils
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputLayout
import com.hootsuite.nachos.NachoTextView
import com.hootsuite.nachos.chip.Chip
import com.hootsuite.nachos.terminator.ChipTerminatorHandler
import com.hootsuite.nachos.validator.ChipifyingNachoValidator
import org.apache.commons.io.FileUtils
import java.io.File
import java.io.IOException
import java.util.*
import java.util.regex.Pattern
import javax.mail.internet.InternetAddress

/**
 * This fragment describe a logic of sent an encrypted or standard message.
 *
 * @author DenBond7
 * Date: 10.05.2017
 * Time: 11:27
 * E-mail: DenBond7@gmail.com
 */

class CreateMessageFragment : BaseSyncFragment(), View.OnFocusChangeListener, AdapterView.OnItemSelectedListener,
    View.OnClickListener, PgpContactsNachoTextView.OnChipLongClickListener {

  private var onMsgSendListener: OnMessageSendListener? = null
  private var listener: OnChangeMessageEncryptionTypeListener? = null
  private var pgpContactsTo: MutableList<PgpContact>? = null
  private var pgpContactsCc: MutableList<PgpContact>? = null
  private var pgpContactsBcc: MutableList<PgpContact>? = null
  private val atts: MutableList<AttachmentInfo>?
  private var folderType: FoldersManager.FolderType? = null
  private var msgInfo: IncomingMessageInfo? = null
  private var serviceInfo: ServiceInfo? = null
  private var account: AccountDao? = null
  private var fromAddrs: FromAddressesAdapter<String>? = null
  private var pgpContactWithNoPublicKey: PgpContact? = null
  private var extraActionInfo: ExtraActionInfo? = null
  private var messageType = MessageType.NEW
  private var draftCacheDir: File? = null

  private var layoutAtts: ViewGroup? = null
  private var editTextFrom: EditText? = null
  private var spinnerFrom: Spinner? = null
  private var recipientsTo: PgpContactsNachoTextView? = null
  private var recipientsCc: PgpContactsNachoTextView? = null
  private var recipientsBcc: PgpContactsNachoTextView? = null
  private var editTextEmailSubject: EditText? = null
  private var editTextEmailMsg: EditText? = null
  private var textInputLayoutMsg: TextInputLayout? = null
  private var layoutContent: ScrollView? = null
  private var progressBarTo: View? = null
  private var progressBarCc: View? = null
  private var progressBarBcc: View? = null
  private var layoutCc: View? = null
  private var layoutBcc: View? = null
  private var progressBarAndButtonLayout: LinearLayout? = null
  private var imageButtonAliases: ImageButton? = null
  private var imageButtonAdditionalRecipientsVisibility: View? = null

  private var isContactsUpdateEnabled = true
  private var isUpdateToCompleted = true
  private var isUpdateCcCompleted = true
  private var isUpdateBccCompleted = true
  private var isIncomingMsgInfoUsed: Boolean = false
  private var isMsgSentToQueue: Boolean = false
  private var originalColor: Int = 0

  override val contentView: View?
    get() = layoutContent

  /**
   * Generate an outgoing message info from entered information by user.
   *
   * @return <tt>OutgoingMessageInfo</tt> Return a created OutgoingMessageInfo object which
   * contains information about an outgoing message.
   */
  private fun getOutgoingMsgInfo(): OutgoingMessageInfo {
    return OutgoingMessageInfo(
        editTextEmailSubject?.text.toString(),
        editTextEmailMsg?.text.toString(),
        recipientsTo?.chipValues,
        recipientsCc?.chipValues,
        recipientsBcc?.chipValues,
        editTextFrom?.text.toString(),
        msgInfo?.origMsgHeaders,
        atts?.minus(forwardedAtts),
        forwardedAtts,
        listener!!.msgEncryptionType,
        messageType === MessageType.FORWARD,
        EmailUtil.genOutboxUID(context!!)
    )
  }

  /**
   * Do a lot of checks to validate an outgoing message info.
   *
   * @return <tt>Boolean</tt> true if all information is correct, false otherwise.
   */
  private val isDataCorrect: Boolean
    get() {
      recipientsTo!!.chipifyAllUnterminatedTokens()
      recipientsCc!!.chipifyAllUnterminatedTokens()
      recipientsBcc!!.chipifyAllUnterminatedTokens()
      if (!fromAddrs!!.isEnabled(spinnerFrom!!.selectedItemPosition)) {
        showInfoSnackbar(recipientsTo!!, getString(R.string.no_key_available))
        return false
      }
      if (recipientsTo!!.text.toString().isEmpty()) {
        showInfoSnackbar(recipientsTo!!, getString(R.string.text_must_not_be_empty,
            getString(R.string.prompt_recipients_to)))
        recipientsTo!!.requestFocus()
        return false
      }
      if (hasInvalidEmail(recipientsTo!!, recipientsCc!!, recipientsBcc!!)) {
        return false
      }
      if (listener!!.msgEncryptionType === MessageEncryptionType.ENCRYPTED) {
        if (recipientsTo!!.text.isNotEmpty() && pgpContactsTo!!.isEmpty()) {
          showUpdateContactsSnackBar(R.id.loader_id_load_info_about_pgp_contacts_to)
          return false
        }
        if (recipientsCc!!.text.isNotEmpty() && pgpContactsCc!!.isEmpty()) {
          showUpdateContactsSnackBar(R.id.loader_id_load_info_about_pgp_contacts_cc)
          return false
        }
        if (recipientsBcc!!.text.isNotEmpty() && pgpContactsBcc!!.isEmpty()) {
          showUpdateContactsSnackBar(R.id.loader_id_load_info_about_pgp_contacts_bcc)
          return false
        }
        if (hasRecipientWithoutPgp(true, pgpContactsTo!! + pgpContactsCc!! + pgpContactsBcc!!)) {
          return false
        }
      }
      if (editTextEmailSubject!!.text.toString().isEmpty()) {
        showInfoSnackbar(editTextEmailSubject!!, getString(R.string.text_must_not_be_empty,
            getString(R.string.prompt_subject)))
        editTextEmailSubject!!.requestFocus()
        return false
      }
      if (atts != null && atts.isEmpty() && editTextEmailMsg!!.text.toString().isEmpty()) {
        showInfoSnackbar(editTextEmailMsg!!, getString(R.string.sending_message_must_not_be_empty))
        editTextEmailMsg!!.requestFocus()
        return false
      }
      if (atts == null || atts.isEmpty() || !hasExternalStorageUris(this.atts)) {
        return true
      }
      if (ContextCompat.checkSelfPermission(context!!, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
        return true
      }
      requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), REQUEST_CODE_REQUEST_READ_EXTERNAL_STORAGE)
      return false
    }


  /**
   * Generate a forwarded attachments list.
   *
   * @return The generated list.
   */
  private val forwardedAtts: MutableList<AttachmentInfo>
    get() {
      val atts = mutableListOf<AttachmentInfo>()

      for (att in this.atts!!) {
        if (att.id != null && att.isForwarded) {
          atts.add(att)
        }
      }

      return atts
    }

  init {
    pgpContactsTo = mutableListOf()
    pgpContactsCc = mutableListOf()
    pgpContactsBcc = mutableListOf()
    atts = ArrayList()
  }

  override fun onAttach(context: Context?) {
    super.onAttach(context)
    if (context is OnMessageSendListener) {
      this.onMsgSendListener = context
    } else
      throw IllegalArgumentException(context!!.toString() + " must implement " +
          OnMessageSendListener::class.java.simpleName)

    if (context is OnChangeMessageEncryptionTypeListener) {
      this.listener = context
    } else
      throw IllegalArgumentException(context.toString() + " must implement " +
          OnChangeMessageEncryptionTypeListener::class.java.simpleName)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setHasOptionsMenu(true)

    initDraftCacheDir()

    account = AccountDaoSource().getActiveAccountInformation(context!!)
    fromAddrs = FromAddressesAdapter(context!!, android.R.layout.simple_list_item_1, android.R.id.text1,
        ArrayList())
    fromAddrs!!.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
    fromAddrs!!.setUseKeysInfo(listener!!.msgEncryptionType === MessageEncryptionType.ENCRYPTED)
    if (account != null) {
      fromAddrs!!.add(account!!.email)
      fromAddrs!!.updateKeyAvailability(account!!.email, !CollectionUtils.isEmpty(
          UserIdEmailsKeysDaoSource().getLongIdsByEmail(context!!, account!!.email)))
    }

    initExtras(activity!!.intent)
  }

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
    return inflater.inflate(R.layout.fragment_create_message, container, false)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    initViews(view)

    if ((msgInfo != null || extraActionInfo != null) && !isIncomingMsgInfoUsed) {
      this.isIncomingMsgInfoUsed = true
      updateViews()
    }

    showAtts()
  }

  override fun onActivityCreated(savedInstanceState: Bundle?) {
    super.onActivityCreated(savedInstanceState)

    if (account != null && AccountDao.ACCOUNT_TYPE_GOOGLE.equals(account!!.accountType!!, ignoreCase = true)) {
      LoaderManager.getInstance(this).restartLoader(R.id.loader_id_load_email_aliases, null, this)
    }

    val isEncryptedMode = listener!!.msgEncryptionType === MessageEncryptionType.ENCRYPTED
    if (msgInfo != null && GeneralUtil.isConnected(context!!) && isEncryptedMode) {
      updateRecipients()
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    if (!isMsgSentToQueue) {
      for ((_, _, _, _, _, _, _, _, _, _, uri) in atts!!) {
        if (uri != null) {
          if (Constants.FILE_PROVIDER_AUTHORITY.equals(uri.authority!!, ignoreCase = true)) {
            context!!.contentResolver.delete(uri, null, null)
          }
        }
      }
    }
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    when (requestCode) {
      REQUEST_CODE_NO_PGP_FOUND_DIALOG -> when (resultCode) {
        NoPgpFoundDialogFragment.RESULT_CODE_SWITCH_TO_STANDARD_EMAIL ->
          listener!!.onMsgEncryptionTypeChanged(MessageEncryptionType.STANDARD)

        NoPgpFoundDialogFragment.RESULT_CODE_IMPORT_THEIR_PUBLIC_KEY -> if (data != null) {
          val pgpContact = data.getParcelableExtra<PgpContact>(NoPgpFoundDialogFragment.EXTRA_KEY_PGP_CONTACT)

          if (pgpContact != null) {
            startActivityForResult(ImportPublicKeyActivity.newIntent(context,
                getString(R.string.import_public_key), pgpContact), REQUEST_CODE_IMPORT_PUBLIC_KEY)
          }
        }

        NoPgpFoundDialogFragment.RESULT_CODE_COPY_FROM_OTHER_CONTACT -> if (data != null) {
          pgpContactWithNoPublicKey = data.getParcelableExtra(NoPgpFoundDialogFragment.EXTRA_KEY_PGP_CONTACT)

          if (pgpContactWithNoPublicKey != null) {
            startActivityForResult(SelectContactsActivity.newIntent(context,
                getString(R.string.use_public_key_from), false), REQUEST_CODE_COPY_PUBLIC_KEY_FROM_OTHER_CONTACT)
          }
        }

        NoPgpFoundDialogFragment.RESULT_CODE_REMOVE_CONTACT -> if (data != null) {
          val pgpContact = data.getParcelableExtra<PgpContact>(NoPgpFoundDialogFragment.EXTRA_KEY_PGP_CONTACT)

          if (pgpContact != null) {
            removePgpContact(pgpContact, recipientsTo!!, pgpContactsTo)
            removePgpContact(pgpContact, recipientsCc!!, pgpContactsCc)
            removePgpContact(pgpContact, recipientsBcc!!, pgpContactsBcc)
          }
        }
      }

      REQUEST_CODE_IMPORT_PUBLIC_KEY -> when (resultCode) {
        Activity.RESULT_OK -> {
          Toast.makeText(context, R.string.the_key_successfully_imported, Toast.LENGTH_SHORT).show()
          updateRecipients()
        }
      }

      REQUEST_CODE_COPY_PUBLIC_KEY_FROM_OTHER_CONTACT -> {
        when (resultCode) {
          Activity.RESULT_OK -> if (data != null) {
            val pgpContact = data.getParcelableExtra<PgpContact>(SelectContactsActivity.KEY_EXTRA_PGP_CONTACT)

            if (pgpContact != null) {
              pgpContactWithNoPublicKey!!.pubkey = pgpContact.pubkey
              ContactsDaoSource().updatePgpContact(context!!, pgpContactWithNoPublicKey)

              Toast.makeText(context, R.string.key_successfully_copied, Toast.LENGTH_LONG).show()
              updateRecipients()
            }
          }
        }

        pgpContactWithNoPublicKey = null
      }

      REQUEST_CODE_GET_CONTENT_FOR_SENDING -> when (resultCode) {
        Activity.RESULT_OK -> if (data != null && data.data != null) {
          val attachmentInfo = EmailUtil.getAttInfoFromUri(context, data.data)
          if (hasAbilityToAddAtt(attachmentInfo)) {
            attachmentInfo?.let { atts!!.add(it) }
            showAtts()
          } else {
            showInfoSnackbar(view!!, getString(R.string.template_warning_max_total_attachments_size,
                FileUtils.byteCountToDisplaySize(Constants.MAX_TOTAL_ATTACHMENT_SIZE_IN_BYTES.toLong())),
                Snackbar.LENGTH_LONG)
          }
        } else {
          showInfoSnackbar(view!!, getString(R.string.can_not_attach_this_file), Snackbar.LENGTH_LONG)
        }
      }

      else -> super.onActivityResult(requestCode, resultCode, data)
    }
  }

  override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
    super.onCreateOptionsMenu(menu, inflater)
    inflater!!.inflate(R.menu.fragment_secure_compose, menu)
  }

  override fun onOptionsItemSelected(item: MenuItem?): Boolean {
    when (item!!.itemId) {
      R.id.menuActionSend -> {
        if (snackBar != null) {
          snackBar!!.dismiss()
        }

        if (isUpdateToCompleted && isUpdateCcCompleted && isUpdateBccCompleted) {
          UIUtil.hideSoftInput(context!!, view)
          if (isDataCorrect) {
            sendMsg()
            this.isMsgSentToQueue = true
          }
        } else {
          Toast.makeText(context, R.string.please_wait_while_information_about_contacts_will_be_updated,
              Toast.LENGTH_SHORT).show()
        }
        return true
      }

      R.id.menuActionAttachFile -> {
        attachFile()
        return true
      }

      else -> return super.onOptionsItemSelected(item)
    }
  }

  override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
    when (requestCode) {
      REQUEST_CODE_REQUEST_READ_EXTERNAL_STORAGE ->
        if (grantResults.size == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
          sendMsg()
        } else {
          Toast.makeText(activity, R.string.cannot_send_attachment_without_read_permission,
              Toast.LENGTH_LONG).show()
        }

      REQUEST_CODE_REQUEST_READ_EXTERNAL_STORAGE_FOR_EXTRA_INFO ->
        if (grantResults.size == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
          addAtts()
          showAtts()
        } else {
          Toast.makeText(activity, R.string.cannot_send_attachment_without_read_permission, Toast.LENGTH_LONG).show()
        }

      else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
  }

  override fun onCreateLoader(id: Int, args: Bundle?): Loader<LoaderResult> {
    when (id) {
      R.id.loader_id_load_info_about_pgp_contacts_to -> {
        pgpContactsTo!!.clear()
        progressBarTo!!.visibility = View.VISIBLE
        isUpdateToCompleted = false
        return UpdateInfoAboutPgpContactsAsyncTaskLoader(context!!, recipientsTo!!.chipAndTokenValues)
      }

      R.id.loader_id_load_info_about_pgp_contacts_cc -> {
        pgpContactsCc!!.clear()
        progressBarCc!!.visibility = View.VISIBLE
        isUpdateCcCompleted = false
        return UpdateInfoAboutPgpContactsAsyncTaskLoader(context!!, recipientsCc!!.chipAndTokenValues)
      }

      R.id.loader_id_load_info_about_pgp_contacts_bcc -> {
        pgpContactsBcc!!.clear()
        progressBarBcc!!.visibility = View.VISIBLE
        isUpdateBccCompleted = false
        return UpdateInfoAboutPgpContactsAsyncTaskLoader(context!!, recipientsBcc!!.chipAndTokenValues)
      }

      R.id.loader_id_load_email_aliases -> return LoadGmailAliasesLoader(context!!, account!!)

      else -> return super.onCreateLoader(id, args)
    }
  }

  @Suppress("UNCHECKED_CAST")
  override fun onSuccess(loaderId: Int, result: Any?) {
    when (loaderId) {
      R.id.loader_id_load_info_about_pgp_contacts_to -> {
        isUpdateToCompleted = true
        pgpContactsTo = getInfoAboutPgpContacts(result as UpdateInfoAboutPgpContactsResult?,
            progressBarTo!!, R.string.to)

        if (pgpContactsTo != null && pgpContactsTo!!.isNotEmpty()) {
          updateChips(recipientsTo!!, pgpContactsTo!!)
        }
      }

      R.id.loader_id_load_info_about_pgp_contacts_cc -> {
        isUpdateCcCompleted = true
        pgpContactsCc = getInfoAboutPgpContacts(result as UpdateInfoAboutPgpContactsResult?,
            progressBarCc!!, R.string.cc)

        if (pgpContactsCc != null && pgpContactsCc!!.isNotEmpty()) {
          updateChips(recipientsCc!!, pgpContactsCc!!)
        }
      }

      R.id.loader_id_load_info_about_pgp_contacts_bcc -> {
        isUpdateBccCompleted = true
        pgpContactsBcc = getInfoAboutPgpContacts(result as UpdateInfoAboutPgpContactsResult?,
            progressBarBcc!!, R.string.bcc)

        if (pgpContactsBcc != null && pgpContactsBcc!!.isNotEmpty()) {
          updateChips(recipientsBcc!!, pgpContactsBcc!!)
        }
      }

      R.id.loader_id_load_email_aliases -> {
        val accountAliasesDaoList = result as List<AccountAliasesDao>?
        val aliases = ArrayList<String>()
        aliases.add(account!!.email)

        for ((_, _, sendAsEmail) in accountAliasesDaoList!!) {
          sendAsEmail?.let { aliases.add(it) }
        }

        fromAddrs!!.clear()
        fromAddrs!!.addAll(aliases)

        for (email in aliases) {
          fromAddrs!!.updateKeyAvailability(email, !CollectionUtils.isEmpty(UserIdEmailsKeysDaoSource()
              .getLongIdsByEmail(context!!, email)))
        }

        if (msgInfo != null) {
          prepareAliasForReplyIfNeeded(aliases)
        } else if (listener!!.msgEncryptionType === MessageEncryptionType.ENCRYPTED) {
          showFirstMatchedAliasWithPrvKey(aliases)
        }

        if (fromAddrs!!.count == 1) {
          if (imageButtonAliases!!.visibility == View.VISIBLE) {
            imageButtonAliases!!.visibility = View.INVISIBLE
          }
        } else {
          if (serviceInfo == null || serviceInfo!!.isFromFieldEditable) {
            imageButtonAliases!!.visibility = View.VISIBLE
          } else {
            imageButtonAliases!!.visibility = View.INVISIBLE
          }
        }

        AccountAliasesDaoSource().updateAliases(context!!, account!!, accountAliasesDaoList)
      }

      else -> super.onSuccess(loaderId, result)
    }
  }

  override fun onError(loaderId: Int, e: Exception?) {
    when (loaderId) {
      R.id.loader_id_load_info_about_pgp_contacts_to -> {
        super.onError(loaderId, e)
        isUpdateToCompleted = true
        progressBarTo!!.visibility = View.INVISIBLE
      }

      R.id.loader_id_load_info_about_pgp_contacts_cc -> {
        super.onError(loaderId, e)
        isUpdateCcCompleted = true
        progressBarCc!!.visibility = View.INVISIBLE
      }

      R.id.loader_id_load_info_about_pgp_contacts_bcc -> {
        super.onError(loaderId, e)
        isUpdateBccCompleted = true
        progressBarBcc!!.visibility = View.INVISIBLE
      }
    }
  }

  override fun onFocusChange(v: View, hasFocus: Boolean) {
    when (v.id) {
      R.id.editTextRecipientTo -> runUpdatePgpContactsAction(pgpContactsTo, progressBarTo,
          R.id.loader_id_load_info_about_pgp_contacts_to, hasFocus)

      R.id.editTextRecipientCc -> runUpdatePgpContactsAction(pgpContactsCc, progressBarCc,
          R.id.loader_id_load_info_about_pgp_contacts_cc, hasFocus)

      R.id.editTextRecipientBcc -> runUpdatePgpContactsAction(pgpContactsBcc, progressBarBcc,
          R.id.loader_id_load_info_about_pgp_contacts_bcc, hasFocus)

      R.id.editTextEmailSubject, R.id.editTextEmailMessage -> if (hasFocus) {
        var isExpandButtonNeeded = false
        if (TextUtils.isEmpty(recipientsCc!!.text)) {
          layoutCc!!.visibility = View.GONE
          isExpandButtonNeeded = true
        }

        if (TextUtils.isEmpty(recipientsBcc!!.text)) {
          layoutBcc!!.visibility = View.GONE
          isExpandButtonNeeded = true
        }

        if (isExpandButtonNeeded) {
          imageButtonAdditionalRecipientsVisibility!!.visibility = View.VISIBLE
          val layoutParams = FrameLayout.LayoutParams(
              FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT)
          layoutParams.gravity = Gravity.TOP or Gravity.END
          progressBarAndButtonLayout!!.layoutParams = layoutParams
        }
      }
    }
  }

  override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
    when (parent?.id) {
      R.id.spinnerFrom -> {
        editTextFrom!!.setText(parent.adapter.getItem(position) as CharSequence)
        if (listener!!.msgEncryptionType === MessageEncryptionType.ENCRYPTED) {
          val adapter = parent.adapter as ArrayAdapter<*>
          val colorGray = UIUtil.getColor(context!!, R.color.gray)
          editTextFrom!!.setTextColor(if (adapter.isEnabled(position)) originalColor else colorGray)
        } else {
          editTextFrom!!.setTextColor(originalColor)
        }
      }
    }
  }

  override fun onNothingSelected(parent: AdapterView<*>) {

  }

  override fun onClick(v: View) {
    when (v.id) {
      R.id.imageButtonAliases -> if (fromAddrs!!.count > 1) {
        spinnerFrom!!.performClick()
      }

      R.id.imageButtonAdditionalRecipientsVisibility -> {
        layoutCc!!.visibility = View.VISIBLE
        layoutBcc!!.visibility = View.VISIBLE
        val layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.MATCH_PARENT)
        layoutParams.gravity = Gravity.TOP or Gravity.END

        progressBarAndButtonLayout!!.layoutParams = layoutParams
        v.visibility = View.GONE
        recipientsCc!!.requestFocus()
      }
    }
  }

  override fun onChipLongClick(nachoTextView: NachoTextView, chip: Chip, event: MotionEvent) {}

  fun onMsgEncryptionTypeChange(messageEncryptionType: MessageEncryptionType?) {
    var emailMassageHint: String? = null
    if (messageEncryptionType != null) {
      when (messageEncryptionType) {
        MessageEncryptionType.ENCRYPTED -> {
          emailMassageHint = getString(R.string.prompt_compose_security_email)
          recipientsTo!!.onFocusChangeListener.onFocusChange(recipientsTo, false)
          recipientsCc!!.onFocusChangeListener.onFocusChange(recipientsCc, false)
          recipientsBcc!!.onFocusChangeListener.onFocusChange(recipientsBcc, false)
          fromAddrs!!.setUseKeysInfo(true)

          val colorGray = UIUtil.getColor(context!!, R.color.gray)
          val isItemEnabled = fromAddrs!!.isEnabled(spinnerFrom!!.selectedItemPosition)
          editTextFrom!!.setTextColor(if (isItemEnabled) originalColor else colorGray)
        }

        MessageEncryptionType.STANDARD -> {
          emailMassageHint = getString(R.string.prompt_compose_standard_email)
          pgpContactsTo!!.clear()
          pgpContactsCc!!.clear()
          pgpContactsBcc!!.clear()
          isUpdateToCompleted = true
          isUpdateCcCompleted = true
          isUpdateBccCompleted = true
          fromAddrs!!.setUseKeysInfo(false)
          editTextFrom!!.setTextColor(originalColor)
        }
      }
    }
    textInputLayoutMsg!!.hint = emailMassageHint
  }

  private fun attachFile() {
    val intent = Intent()
    intent.action = Intent.ACTION_OPEN_DOCUMENT
    intent.addCategory(Intent.CATEGORY_OPENABLE)
    intent.type = "*/*"
    startActivityForResult(Intent.createChooser(intent, getString(R.string.choose_attachment)),
        REQUEST_CODE_GET_CONTENT_FOR_SENDING)
  }

  private fun initExtras(intent: Intent?) {
    if (intent != null) {
      if (intent.hasExtra(CreateMessageActivity.EXTRA_KEY_MESSAGE_TYPE)) {
        this.messageType = intent.getSerializableExtra(CreateMessageActivity.EXTRA_KEY_MESSAGE_TYPE) as MessageType
      }

      if (!TextUtils.isEmpty(intent.action) && intent.action!!.startsWith("android.intent.action")) {
        this.extraActionInfo = ExtraActionInfo.parseExtraActionInfo(context!!, intent)

        if (hasExternalStorageUris(extraActionInfo!!.atts)) {
          val isPermissionGranted = ContextCompat.checkSelfPermission(context!!,
              Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
          if (isPermissionGranted) {
            requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                REQUEST_CODE_REQUEST_READ_EXTERNAL_STORAGE_FOR_EXTRA_INFO)
          } else {
            addAtts()
          }
        } else {
          addAtts()
        }
      } else {
        this.serviceInfo = intent.getParcelableExtra(CreateMessageActivity.EXTRA_KEY_SERVICE_INFO)
        this.msgInfo = intent.getParcelableExtra(CreateMessageActivity.EXTRA_KEY_INCOMING_MESSAGE_INFO)

        if (msgInfo != null && msgInfo!!.localFolder != null) {
          this.folderType = FoldersManager.getFolderType(msgInfo!!.localFolder)
        }

        if (this.serviceInfo != null && this.serviceInfo!!.atts != null) {
          atts!!.addAll(this.serviceInfo!!.atts!!)
        }
      }
    }
  }

  private fun initDraftCacheDir() {
    draftCacheDir = File(context!!.cacheDir, Constants.DRAFT_CACHE_DIR)

    if (!draftCacheDir!!.exists()) {
      if (!draftCacheDir!!.mkdir()) {
        Log.e(TAG, "Create cache directory " + draftCacheDir!!.name + " filed!")
      }
    }
  }

  private fun addAtts() {
    val sizeWarningMsg = getString(R.string.template_warning_max_total_attachments_size,
        FileUtils.byteCountToDisplaySize(Constants.MAX_TOTAL_ATTACHMENT_SIZE_IN_BYTES.toLong()))

    for (attachmentInfo in extraActionInfo!!.atts) {
      if (hasAbilityToAddAtt(attachmentInfo)) {

        if (TextUtils.isEmpty(attachmentInfo.name)) {
          val msg = "attachmentInfo.getName() == null, uri = " + attachmentInfo.uri!!
          ExceptionUtil.handleError(NullPointerException(msg))
          continue
        }

        val draftAtt = File(draftCacheDir, attachmentInfo.name)

        try {
          val inputStream = context!!.contentResolver.openInputStream(attachmentInfo.uri!!)

          if (inputStream != null) {
            FileUtils.copyInputStreamToFile(inputStream, draftAtt)
            val uri = FileProvider.getUriForFile(context!!, Constants.FILE_PROVIDER_AUTHORITY, draftAtt)
            attachmentInfo.uri = uri
            atts!!.add(attachmentInfo)
          }
        } catch (e: IOException) {
          e.printStackTrace()
          ExceptionUtil.handleError(e)

          if (!draftAtt.delete()) {
            Log.e(TAG, "Delete " + draftAtt.name + " filed!")
          }
        }

      } else {
        Toast.makeText(context, sizeWarningMsg, Toast.LENGTH_SHORT).show()
        break
      }
    }
  }

  private fun updateRecipients() {
    LoaderManager.getInstance(this).restartLoader(R.id.loader_id_load_info_about_pgp_contacts_to, null, this)

    if (layoutCc!!.visibility == View.VISIBLE) {
      LoaderManager.getInstance(this).restartLoader(R.id.loader_id_load_info_about_pgp_contacts_cc, null, this)
    } else {
      recipientsCc!!.setText(null as CharSequence?)
      pgpContactsCc!!.clear()
    }

    if (layoutBcc!!.visibility == View.VISIBLE) {
      LoaderManager.getInstance(this).restartLoader(R.id.loader_id_load_info_about_pgp_contacts_bcc, null, this)
    } else {
      recipientsBcc!!.setText(null as CharSequence?)
      pgpContactsBcc!!.clear()
    }
  }

  /**
   * Get information about some [PgpContact]s.
   *
   * @param result                  An API result (lookup API).
   * @param progressBar             A [ProgressBar] which is showing an action progress.
   * @param additionalToastStringId A hint string id.
   */
  private fun getInfoAboutPgpContacts(result: UpdateInfoAboutPgpContactsResult?,
                                      progressBar: View, additionalToastStringId: Int): MutableList<PgpContact>? {
    progressBar.visibility = View.INVISIBLE

    var pgpContacts: MutableList<PgpContact>? = null

    if (result?.updatedPgpContacts != null) {
      pgpContacts = result.updatedPgpContacts.toMutableList()
    }

    if (result == null || !result.isAllInfoReceived) {
      Toast.makeText(context, getString(R.string.info_about_some_contacts_not_received,
          getString(additionalToastStringId)), Toast.LENGTH_SHORT).show()
    }

    return pgpContacts
  }

  /**
   * Run an action to update information about some [PgpContact]s.
   *
   * @param pgpContacts Old [PgpContact]s
   * @param progressBar A [ProgressBar] which is showing an action progress.
   * @param loaderId    A loader id.
   * @param hasFocus    A value which indicates the view focus.
   * @return A modified contacts list.
   */
  private fun runUpdatePgpContactsAction(pgpContacts: MutableList<PgpContact>?, progressBar: View?,
                                         loaderId: Int, hasFocus: Boolean): List<PgpContact>? {
    if (listener!!.msgEncryptionType === MessageEncryptionType.ENCRYPTED) {
      progressBar!!.visibility = if (hasFocus) View.INVISIBLE else View.VISIBLE
      if (hasFocus) {
        pgpContacts!!.clear()
      } else {
        if (isContactsUpdateEnabled) {
          if (isAdded) {
            LoaderManager.getInstance(this).restartLoader(loaderId, null, this)
          }
        } else {
          progressBar.visibility = View.INVISIBLE
        }
      }
    }

    return pgpContacts
  }

  /**
   * Prepare an alias for the reply. Will be used the email address that the email was received. Will be used the
   * first found matched email.
   *
   * @param aliases A list of Gmail aliases.
   */
  private fun prepareAliasForReplyIfNeeded(aliases: List<String>) {
    val messageEncryptionType = listener!!.msgEncryptionType

    val toAddresses: List<InternetAddress>? = if (folderType === FoldersManager.FolderType.SENT) {
      msgInfo!!.getFrom()
    } else {
      msgInfo!!.getTo()
    }

    if (!CollectionUtils.isEmpty(toAddresses)) {
      var firstFoundedAlias: String? = null
      for (toAddress in toAddresses!!) {
        if (firstFoundedAlias == null) {
          for (alias in aliases) {
            if (alias.equals(toAddress.address, ignoreCase = true)) {
              firstFoundedAlias = if (messageEncryptionType === MessageEncryptionType.ENCRYPTED
                  && fromAddrs!!.hasPrvKey(alias)) {
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
        val position = fromAddrs!!.getPosition(firstFoundedAlias)
        if (position != -1) {
          spinnerFrom!!.setSelection(position)
        }
      }
    }
  }

  private fun showFirstMatchedAliasWithPrvKey(aliases: List<String>) {
    var firstFoundedAliasWithPrvKey: String? = null
    for (alias in aliases) {
      if (fromAddrs!!.hasPrvKey(alias)) {
        firstFoundedAliasWithPrvKey = alias
        break
      }
    }

    if (firstFoundedAliasWithPrvKey != null) {
      val position = fromAddrs!!.getPosition(firstFoundedAliasWithPrvKey)
      if (position != -1) {
        spinnerFrom!!.setSelection(position)
      }
    }
  }

  /**
   * Check that all recipients have PGP.
   *
   * @return true if all recipients have PGP, other wise false.
   */
  private fun hasRecipientWithoutPgp(isRemoveActionEnabled: Boolean, pgpContacts: List<PgpContact>): Boolean {
    for (pgpContact in pgpContacts) {
      if (!pgpContact.hasPgp) {
        showNoPgpFoundDialog(pgpContact, isRemoveActionEnabled)
        return true
      }
    }

    return false
  }

  /**
   * This method does update chips in the recipients field.
   *
   * @param view        A view which contains input [PgpContact](s).
   * @param pgpContacts The input [PgpContact](s)
   */
  private fun updateChips(view: PgpContactsNachoTextView, pgpContacts: List<PgpContact>) {
    val builder = SpannableStringBuilder(view.text)

    val pgpContactChipSpans = builder.getSpans(0, view.length(), PGPContactChipSpan::class.java)

    if (pgpContactChipSpans.isNotEmpty()) {
      for ((email, _, _, hasPgp) in pgpContacts) {
        for (pgpContactChipSpan in pgpContactChipSpans) {
          if (email.equals(pgpContactChipSpan.text.toString(), ignoreCase = true)) {
            pgpContactChipSpan.setHasPgp(hasPgp)
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
  private fun initChipsView(pgpContactsNachoTextView: PgpContactsNachoTextView) {
    pgpContactsNachoTextView.setNachoValidator(ChipifyingNachoValidator())
    pgpContactsNachoTextView.setIllegalCharacters(',')
    pgpContactsNachoTextView.addChipTerminator(' ', ChipTerminatorHandler.BEHAVIOR_CHIPIFY_TO_TERMINATOR)
    pgpContactsNachoTextView.chipTokenizer = SingleCharacterSpanChipTokenizer(context!!,
        CustomChipSpanChipCreator(context!!), PGPContactChipSpan::class.java)
    pgpContactsNachoTextView.setAdapter(preparePgpContactAdapter())
    pgpContactsNachoTextView.onFocusChangeListener = this
    pgpContactsNachoTextView.setListener(this)
  }

  private fun showUpdateContactsSnackBar(loaderId: Int) {
    showSnackbar(view!!, getString(R.string.please_update_information_about_contacts),
        getString(R.string.update), Snackbar.LENGTH_LONG, View.OnClickListener {
      if (GeneralUtil.isConnected(context!!)) {
        LoaderManager.getInstance(this@CreateMessageFragment).restartLoader(loaderId, null,
            this@CreateMessageFragment)
      } else {
        showInfoSnackbar(view!!, getString(R.string.internet_connection_is_not_available))
      }
    })
  }

  private fun hasExternalStorageUris(attachmentInfoList: List<AttachmentInfo>): Boolean {
    for ((_, _, _, _, _, _, _, _, _, _, uri) in attachmentInfoList) {
      if (uri != null && ContentResolver.SCHEME_FILE.equals(uri.scheme!!, ignoreCase = true)) {
        return true
      }
    }
    return false
  }

  /**
   * Remove the current [PgpContact] from recipients.
   *
   * @param pgpContact               The [PgpContact] which will be removed.
   * @param pgpContactsNachoTextView The [NachoTextView] which contains the delete candidate.
   * @param pgpContacts              The list which contains the delete candidate.
   */
  private fun removePgpContact(pgpContact: PgpContact, pgpContactsNachoTextView: PgpContactsNachoTextView,
                               pgpContacts: MutableList<PgpContact>?) {
    val chipTokenizer = pgpContactsNachoTextView.chipTokenizer
    for (chip in pgpContactsNachoTextView.allChips) {
      if (pgpContact.email
              .equals(chip.text.toString(), ignoreCase = true) && chipTokenizer != null) {
        chipTokenizer.deleteChip(chip, pgpContactsNachoTextView.text)
      }
    }

    val iterator = pgpContacts!!.iterator()
    while (iterator.hasNext()) {
      val next = iterator.next()
      if (next.email.equals(next.email, ignoreCase = true)) {
        iterator.remove()
      }
    }
  }

  /**
   * Init fragment views
   *
   * @param view The root fragment view.
   */
  private fun initViews(view: View) {
    layoutContent = view.findViewById(R.id.scrollView)
    layoutAtts = view.findViewById(R.id.layoutAtts)
    layoutCc = view.findViewById(R.id.layoutCc)
    layoutBcc = view.findViewById(R.id.layoutBcc)
    progressBarAndButtonLayout = view.findViewById(R.id.progressBarAndButtonLayout)

    recipientsTo = view.findViewById(R.id.editTextRecipientTo)
    recipientsCc = view.findViewById(R.id.editTextRecipientCc)
    recipientsBcc = view.findViewById(R.id.editTextRecipientBcc)

    initChipsView(recipientsTo!!)
    initChipsView(recipientsCc!!)
    initChipsView(recipientsBcc!!)

    spinnerFrom = view.findViewById(R.id.spinnerFrom)
    spinnerFrom!!.onItemSelectedListener = this
    spinnerFrom!!.adapter = fromAddrs

    editTextFrom = view.findViewById(R.id.editTextFrom)
    originalColor = editTextFrom!!.currentTextColor

    imageButtonAliases = view.findViewById(R.id.imageButtonAliases)
    if (imageButtonAliases != null) {
      imageButtonAliases!!.setOnClickListener(this)
    }

    imageButtonAdditionalRecipientsVisibility = view.findViewById(R.id.imageButtonAdditionalRecipientsVisibility)
    if (imageButtonAdditionalRecipientsVisibility != null) {
      imageButtonAdditionalRecipientsVisibility!!.setOnClickListener(this)
    }

    editTextEmailSubject = view.findViewById(R.id.editTextEmailSubject)
    editTextEmailSubject!!.onFocusChangeListener = this
    editTextEmailMsg = view.findViewById(R.id.editTextEmailMessage)
    editTextEmailMsg!!.onFocusChangeListener = this
    textInputLayoutMsg = view.findViewById(R.id.textInputLayoutEmailMessage)

    progressBarTo = view.findViewById(R.id.progressBarTo)
    progressBarCc = view.findViewById(R.id.progressBarCc)
    progressBarBcc = view.findViewById(R.id.progressBarBcc)
  }

  /**
   * Update views on the screen. This method can be called when we need to update the current
   * screen.
   */
  private fun updateViews() {
    onMsgEncryptionTypeChange(listener!!.msgEncryptionType)

    if (extraActionInfo != null) {
      updateViewsFromExtraActionInfo()
    } else {
      if (msgInfo != null) {
        updateViewsFromIncomingMsgInfo()
        recipientsTo!!.chipifyAllUnterminatedTokens()
        recipientsCc!!.chipifyAllUnterminatedTokens()
        editTextEmailSubject!!.setText(prepareReplySubject(msgInfo!!.getSubject() ?: ""))
      }

      if (serviceInfo != null) {
        updateViewsFromServiceInfo()
      }
    }
  }

  private fun updateViewsFromExtraActionInfo() {
    setupPgpFromExtraActionInfo(recipientsTo, extraActionInfo!!.toAddresses.toTypedArray())
    setupPgpFromExtraActionInfo(recipientsCc, extraActionInfo!!.ccAddresses.toTypedArray())
    setupPgpFromExtraActionInfo(recipientsBcc, extraActionInfo!!.bccAddresses.toTypedArray())

    editTextEmailSubject!!.setText(extraActionInfo!!.subject)
    editTextEmailMsg!!.setText(extraActionInfo!!.body)

    if (TextUtils.isEmpty(recipientsTo!!.text)) {
      recipientsTo!!.requestFocus()
      return
    }

    if (TextUtils.isEmpty(editTextEmailSubject!!.text)) {
      editTextEmailSubject!!.requestFocus()
      return
    }

    editTextEmailMsg!!.requestFocus()
  }

  private fun updateViewsFromServiceInfo() {
    recipientsTo!!.isFocusable = serviceInfo!!.isToFieldEditable
    recipientsTo!!.isFocusableInTouchMode = serviceInfo!!.isToFieldEditable
    //todo-denbond7 Need to add a similar option for recipientsCc and recipientsBcc

    editTextEmailSubject!!.isFocusable = serviceInfo!!.isSubjectEditable
    editTextEmailSubject!!.isFocusableInTouchMode = serviceInfo!!.isSubjectEditable

    editTextEmailMsg!!.isFocusable = serviceInfo!!.isMsgEditable
    editTextEmailMsg!!.isFocusableInTouchMode = serviceInfo!!.isMsgEditable

    if (!TextUtils.isEmpty(serviceInfo!!.systemMsg)) {
      editTextEmailMsg!!.setText(serviceInfo!!.systemMsg)
    }
  }

  private fun updateViewsFromIncomingMsgInfo() {
    when (messageType) {
      MessageType.REPLY -> updateViewsIfReplyMode()

      MessageType.REPLY_ALL -> updateViewsIfReplyAllMode()

      MessageType.FORWARD -> updateViewsIfFwdMode()

      else -> {
      }
    }
  }

  private fun updateViewsIfFwdMode() {
    val originalMsgInfo = msgInfo ?: return

    if (!CollectionUtils.isEmpty(originalMsgInfo.atts)) {
      for (att in originalMsgInfo.atts!!) {
        if (hasAbilityToAddAtt(att)) {
          atts!!.add(att)
        } else {
          showInfoSnackbar(view!!, getString(R.string.template_warning_max_total_attachments_size,
              FileUtils.byteCountToDisplaySize(Constants.MAX_TOTAL_ATTACHMENT_SIZE_IN_BYTES.toLong())),
              Snackbar.LENGTH_LONG)
        }
      }
    }

    editTextEmailMsg?.setText(getString(R.string.forward_template, originalMsgInfo.getFrom()!!.first().address,
        EmailUtil.genForwardedMsgDate(originalMsgInfo.getReceiveDate()), originalMsgInfo.getSubject(),
        prepareRecipientsLineForForwarding(originalMsgInfo.getTo())))

    if (!CollectionUtils.isEmpty(originalMsgInfo.getCc())) {
      editTextEmailMsg?.append("Cc: ")
      editTextEmailMsg?.append(prepareRecipientsLineForForwarding(originalMsgInfo.getCc()))
      editTextEmailMsg?.append("\n\n")
    }

    editTextEmailMsg?.append("\n\n" + originalMsgInfo.text)
  }

  private fun updateViewsIfReplyAllMode() {
    if (folderType === FoldersManager.FolderType.SENT || folderType === FoldersManager.FolderType.OUTBOX) {
      recipientsTo!!.setText(prepareRecipients(msgInfo!!.getTo()))

      if (msgInfo!!.getCc() != null && msgInfo!!.getCc()!!.isNotEmpty()) {
        layoutCc!!.visibility = View.VISIBLE
        recipientsCc!!.append(prepareRecipients(msgInfo!!.getCc()))
      }
    } else {
      recipientsTo!!.setText(prepareRecipients(msgInfo!!.getFrom()))

      val ccSet = HashSet<InternetAddress>()

      if (!CollectionUtils.isEmpty(msgInfo!!.getTo())) {
        for (address in msgInfo!!.getTo()!!) {
          if (!account!!.email.equals(address.address, ignoreCase = true)) {
            ccSet.add(address)
          }
        }

        if (AccountDao.ACCOUNT_TYPE_GOOGLE.equals(account!!.accountType!!, ignoreCase = true)) {
          val accountAliases = AccountAliasesDaoSource().getAliases(context!!, account)
          for ((_, _, sendAsEmail) in accountAliases) {
            val iterator = ccSet.iterator()

            while (iterator.hasNext()) {
              if (sendAsEmail!!.equals(iterator.next().address, ignoreCase = true)) {
                iterator.remove()
              }
            }
          }
        }
      }

      if (!CollectionUtils.isEmpty(msgInfo!!.getCc())) {
        for (address in msgInfo!!.getCc()!!) {
          if (!account!!.email.equals(address.address, ignoreCase = true)) {
            ccSet.add(address)
          }
        }
      }

      if (!ccSet.isEmpty()) {
        layoutCc!!.visibility = View.VISIBLE
        recipientsCc!!.append(prepareRecipients(ccSet))
      }
    }

    if (!TextUtils.isEmpty(recipientsTo!!.text) || !TextUtils.isEmpty(recipientsCc!!.text)) {
      editTextEmailMsg!!.requestFocus()
    }
  }

  private fun updateViewsIfReplyMode() {
    if (folderType != null) {
      when (folderType) {
        FoldersManager.FolderType.SENT,
        FoldersManager.FolderType.OUTBOX -> recipientsTo!!.setText(prepareRecipients(msgInfo!!.getTo()))

        else -> recipientsTo!!.setText(prepareRecipients(msgInfo!!.getFrom()))
      }
    } else {
      recipientsTo!!.setText(prepareRecipients(msgInfo!!.getFrom()))
    }

    if (!TextUtils.isEmpty(recipientsTo!!.text)) {
      editTextEmailMsg!!.requestFocus()
    }
  }

  private fun setupPgpFromExtraActionInfo(pgpContactsNachoTextView: PgpContactsNachoTextView?,
                                          addresses: Array<String>?) {
    if (addresses != null && addresses.isNotEmpty()) {
      pgpContactsNachoTextView!!.setText(prepareRecipients(addresses))
      pgpContactsNachoTextView.chipifyAllUnterminatedTokens()
      pgpContactsNachoTextView.onFocusChangeListener.onFocusChange(pgpContactsNachoTextView, false)
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
    val prefix = when (messageType) {
      MessageType.REPLY, MessageType.REPLY_ALL -> "Re"
      MessageType.FORWARD -> "Fwd"
      else -> return subject
    }
    val prefixMatcher = Pattern.compile("^($prefix: )", Pattern.CASE_INSENSITIVE).matcher(subject)
    return if (prefixMatcher.find()) subject else getString(R.string.template_reply_subject, prefix, subject)
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
   * Prepare a [PgpContactAdapter] for the [NachoTextView] object.
   *
   * @return <tt>[PgpContactAdapter]</tt>
   */
  @SuppressLint("Recycle")
  private fun preparePgpContactAdapter(): PgpContactAdapter {
    val pgpContactAdapter = PgpContactAdapter(context!!, null, true)
    //setup a search contacts logic in the database
    pgpContactAdapter.filterQueryProvider = FilterQueryProvider { constraint ->
      val uri = ContactsDaoSource().baseContentUri
      val selection = ContactsDaoSource.COL_EMAIL + " LIKE ?"
      val selectionArgs = arrayOf("%$constraint%")
      val sortOrder = ContactsDaoSource.COL_LAST_USE + " DESC"

      context!!.contentResolver.query(uri, null, selection, selectionArgs, sortOrder)
    }

    return pgpContactAdapter
  }

  /**
   * Check if the given [pgpContactsNachoTextViews] List has an invalid email.
   *
   * @return <tt>boolean</tt> true - if has, otherwise false..
   */
  private fun hasInvalidEmail(vararg pgpContactsNachoTextViews: PgpContactsNachoTextView): Boolean {
    for (textView in pgpContactsNachoTextViews) {
      val emails = textView.chipAndTokenValues
      for (email in emails) {
        if (!GeneralUtil.isEmailValid(email)) {
          showInfoSnackbar(textView, getString(R.string.error_some_email_is_not_valid, email))
          textView.requestFocus()
          return true
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
    return atts!!.map { it.encodedSize.toInt() }.sum() + (newAttInfo?.encodedSize?.toInt()
        ?: 0) < Constants.MAX_TOTAL_ATTACHMENT_SIZE_IN_BYTES
  }


  /**
   * Show a dialog where we can select different actions.
   *
   * @param pgpContact            The [PgpContact] which will be used when we select the remove action.
   * @param isRemoveActionEnabled true if we want to show the remove action, false otherwise.
   */
  private fun showNoPgpFoundDialog(pgpContact: PgpContact, isRemoveActionEnabled: Boolean) {
    val dialogFragment = NoPgpFoundDialogFragment.newInstance(pgpContact, isRemoveActionEnabled)
    dialogFragment.setTargetFragment(this, REQUEST_CODE_NO_PGP_FOUND_DIALOG)
    dialogFragment.show(fragmentManager!!, NoPgpFoundDialogFragment::class.java.simpleName)
  }

  /**
   * Send a message.
   */
  private fun sendMsg() {
    dismissCurrentSnackBar()

    isContactsUpdateEnabled = false
    onMsgSendListener?.sendMsg(getOutgoingMsgInfo())
  }

  /**
   * Show attachments which were added.
   */
  private fun showAtts() {
    if (atts!!.isNotEmpty()) {
      layoutAtts!!.removeAllViews()
      val layoutInflater = LayoutInflater.from(context)
      for (att in atts) {
        val rootView = layoutInflater.inflate(R.layout.attachment_item, layoutAtts, false)

        val textViewAttName = rootView.findViewById<TextView>(R.id.textViewAttchmentName)
        textViewAttName.text = att.name

        val textViewAttSize = rootView.findViewById<TextView>(R.id.textViewAttSize)
        if (att.encodedSize > 0) {
          textViewAttSize.visibility = View.VISIBLE
          textViewAttSize.text = Formatter.formatFileSize(context, att.encodedSize)
        } else {
          textViewAttSize.visibility = View.GONE
        }

        val imageButtonDownloadAtt = rootView.findViewById<View>(R.id.imageButtonDownloadAtt)
        imageButtonDownloadAtt.visibility = View.GONE

        if (!att.isProtected) {
          val imageButtonClearAtt = rootView.findViewById<View>(R.id.imageButtonClearAtt)
          imageButtonClearAtt.visibility = View.VISIBLE
          imageButtonClearAtt.setOnClickListener {
            atts.remove(att)
            layoutAtts!!.removeView(rootView)

            //Remove a temp file which was created by our app
            val uri = att.uri
            if (uri != null && Constants.FILE_PROVIDER_AUTHORITY.equals(uri.authority!!, ignoreCase = true)) {
              context!!.contentResolver.delete(uri, null, null)
            }
          }
        }
        layoutAtts!!.addView(rootView)
      }
    } else {
      layoutAtts!!.removeAllViews()
    }
  }

  /**
   * This interface will be used when we send a message.
   */
  interface OnMessageSendListener {
    fun sendMsg(outgoingMsgInfo: OutgoingMessageInfo)
  }

  companion object {
    private const val REQUEST_CODE_NO_PGP_FOUND_DIALOG = 100
    private const val REQUEST_CODE_IMPORT_PUBLIC_KEY = 101
    private const val REQUEST_CODE_GET_CONTENT_FOR_SENDING = 102
    private const val REQUEST_CODE_COPY_PUBLIC_KEY_FROM_OTHER_CONTACT = 103
    private const val REQUEST_CODE_REQUEST_READ_EXTERNAL_STORAGE = 104
    private const val REQUEST_CODE_REQUEST_READ_EXTERNAL_STORAGE_FOR_EXTRA_INFO = 105
    private val TAG = CreateMessageFragment::class.java.simpleName
  }
}
