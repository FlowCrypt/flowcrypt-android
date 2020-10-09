/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import com.flowcrypt.email.R
import com.flowcrypt.email.api.email.model.IncomingMessageInfo
import com.flowcrypt.email.api.email.model.OutgoingMessageInfo
import com.flowcrypt.email.api.email.model.ServiceInfo
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.extensions.decrementSafely
import com.flowcrypt.email.extensions.incrementSafely
import com.flowcrypt.email.model.MessageEncryptionType
import com.flowcrypt.email.model.MessageType
import com.flowcrypt.email.service.PrepareOutgoingMessagesJobIntentService
import com.flowcrypt.email.ui.activity.base.BaseBackStackSyncActivity
import com.flowcrypt.email.ui.activity.fragment.base.CreateMessageFragment
import com.flowcrypt.email.ui.activity.fragment.dialog.ChoosePublicKeyDialogFragment
import com.flowcrypt.email.ui.activity.listeners.OnChangeMessageEncryptionTypeListener
import com.flowcrypt.email.ui.activity.settings.FeedbackActivity
import com.flowcrypt.email.util.GeneralUtil
import com.flowcrypt.email.util.UIUtil

/**
 * This activity describes a logic of send encrypted or standard message.
 *
 * @author DenBond7
 * Date: 10.05.2017
 * Time: 11:43
 * E-mail: DenBond7@gmail.com
 */
class CreateMessageActivity : BaseBackStackSyncActivity(), CreateMessageFragment.OnMessageSendListener,
    OnChangeMessageEncryptionTypeListener, ChoosePublicKeyDialogFragment.OnLoadKeysProgressListener {

  private var nonEncryptedHintView: View? = null
  override lateinit var rootView: View

  override var msgEncryptionType = MessageEncryptionType.ENCRYPTED
    private set
  private var serviceInfo: ServiceInfo? = null

  override val contentViewResourceId: Int
    get() = R.layout.activity_create_message

  override fun onCreate(savedInstanceState: Bundle?) {
    if (intent != null) {
      serviceInfo = intent.getParcelableExtra(EXTRA_KEY_SERVICE_INFO)
      msgEncryptionType = intent.getParcelableExtra(EXTRA_KEY_MESSAGE_ENCRYPTION_TYPE)
          ?: MessageEncryptionType.ENCRYPTED
    }

    super.onCreate(savedInstanceState)

    rootView = findViewById(R.id.createMessageFragment)
    initNonEncryptedHintView()

    prepareActionBarTitle()
    onMsgEncryptionTypeChanged(msgEncryptionType)
  }

  override fun onCreateOptionsMenu(menu: Menu): Boolean {
    menuInflater.inflate(R.menu.activity_send_message, menu)
    return super.onCreateOptionsMenu(menu)
  }

  override fun onPrepareOptionsMenu(menu: Menu): Boolean {
    super.onPrepareOptionsMenu(menu)
    val menuActionSwitchType = menu.findItem(R.id.menuActionSwitchType)
    val titleRes = if (msgEncryptionType === MessageEncryptionType.STANDARD)
      R.string.switch_to_secure_email
    else
      R.string
          .switch_to_standard_email
    menuActionSwitchType.setTitle(titleRes)

    if (serviceInfo?.isMsgTypeSwitchable == false) {
      menu.removeItem(R.id.menuActionSwitchType)
    }

    if (serviceInfo?.hasAbilityToAddNewAtt == false) {
      menu.removeItem(R.id.menuActionAttachFile)
    }

    return true
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    when (item.itemId) {
      R.id.menuActionHelp -> {
        FeedbackActivity.show(this)
        return true
      }

      R.id.menuActionSwitchType -> {
        when (msgEncryptionType) {
          MessageEncryptionType.ENCRYPTED -> onMsgEncryptionTypeChanged(MessageEncryptionType.STANDARD)

          MessageEncryptionType.STANDARD -> onMsgEncryptionTypeChanged(MessageEncryptionType.ENCRYPTED)
        }
        return true
      }

      else -> return super.onOptionsItemSelected(item)
    }
  }

  override fun sendMsg(outgoingMsgInfo: OutgoingMessageInfo) {
    PrepareOutgoingMessagesJobIntentService.enqueueWork(this, outgoingMsgInfo)
    Toast.makeText(this, if (GeneralUtil.isConnected(this))
      R.string.sending
    else
      R.string.no_conn_msg_sent_later, Toast.LENGTH_SHORT).show()
    finish()
  }

  override fun onMsgEncryptionTypeChanged(messageEncryptionType: MessageEncryptionType) {
    this.msgEncryptionType = messageEncryptionType
    when (messageEncryptionType) {
      MessageEncryptionType.ENCRYPTED -> {
        appBarLayout?.setBackgroundColor(UIUtil.getColor(this, R.color.colorPrimary))
        appBarLayout?.removeView(nonEncryptedHintView)
      }

      MessageEncryptionType.STANDARD -> {
        appBarLayout?.setBackgroundColor(UIUtil.getColor(this, R.color.red))
        appBarLayout?.addView(nonEncryptedHintView)
      }
    }

    invalidateOptionsMenu()
    notifyFragmentAboutChangeMsgEncryptionType(messageEncryptionType)
  }

  override fun onAccountInfoRefreshed(accountEntity: AccountEntity?) {
    super.onAccountInfoRefreshed(accountEntity)
    //check create a message from extra info when account didn't setup
    if (activeAccount == null) {
      Toast.makeText(this, R.string.setup_app, Toast.LENGTH_LONG).show()
      finish()
    }
  }

  override fun onLoadKeysProgress(status: Result.Status) {
    if (status == Result.Status.LOADING) {
      countingIdlingResource.incrementSafely()
    } else {
      countingIdlingResource.decrementSafely()
    }
  }

  private fun prepareActionBarTitle() {
    if (supportActionBar != null) {
      if (intent.hasExtra(EXTRA_KEY_MESSAGE_TYPE)) {
        val msgType = intent.getParcelableExtra<MessageType>(EXTRA_KEY_MESSAGE_TYPE)

        msgType?.let {
          when (it) {
            MessageType.NEW -> supportActionBar?.setTitle(R.string.compose)
            MessageType.REPLY -> supportActionBar?.setTitle(R.string.reply)
            MessageType.REPLY_ALL -> supportActionBar?.setTitle(R.string.reply_all)
            MessageType.FORWARD -> supportActionBar?.setTitle(R.string.forward)
          }
        }
      } else {
        if (intent.getParcelableExtra<Parcelable>(EXTRA_KEY_INCOMING_MESSAGE_INFO) != null) {
          supportActionBar!!.setTitle(R.string.reply)
        } else {
          supportActionBar!!.setTitle(R.string.compose)
        }
      }
    }
  }

  private fun notifyFragmentAboutChangeMsgEncryptionType(messageEncryptionType: MessageEncryptionType) {
    val fragment = supportFragmentManager.findFragmentById(R.id
        .createMessageFragment) as CreateMessageFragment?

    fragment?.onMsgEncryptionTypeChange(messageEncryptionType)
  }

  private fun initNonEncryptedHintView() {
    nonEncryptedHintView = layoutInflater.inflate(R.layout.under_toolbar_line_with_text, appBarLayout, false)
    val textView = nonEncryptedHintView!!.findViewById<TextView>(R.id.underToolbarTextTextView)
    textView.setText(R.string.this_message_will_not_be_encrypted)
  }

  companion object {

    val EXTRA_KEY_MESSAGE_ENCRYPTION_TYPE =
        GeneralUtil.generateUniqueExtraKey("EXTRA_KEY_MESSAGE_ENCRYPTION_TYPE", CreateMessageActivity::class.java)

    val EXTRA_KEY_INCOMING_MESSAGE_INFO =
        GeneralUtil.generateUniqueExtraKey("EXTRA_KEY_INCOMING_MESSAGE_INFO", CreateMessageActivity::class.java)

    val EXTRA_KEY_SERVICE_INFO =
        GeneralUtil.generateUniqueExtraKey("EXTRA_KEY_SERVICE_INFO", CreateMessageActivity::class.java)

    val EXTRA_KEY_MESSAGE_TYPE =
        GeneralUtil.generateUniqueExtraKey("EXTRA_KEY_MESSAGE_TYPE", CreateMessageActivity::class.java)

    fun generateIntent(context: Context, msgInfo: IncomingMessageInfo?,
                       msgEncryptionType: MessageEncryptionType): Intent {
      return generateIntent(context, msgInfo, MessageType.NEW, msgEncryptionType)
    }

    fun generateIntent(context: Context?, msgInfo: IncomingMessageInfo?, messageType: MessageType?,
                       msgEncryptionType: MessageEncryptionType?, serviceInfo: ServiceInfo? = null): Intent {

      val intent = Intent(context, CreateMessageActivity::class.java)
      intent.putExtra(EXTRA_KEY_INCOMING_MESSAGE_INFO, msgInfo)
      intent.putExtra(EXTRA_KEY_MESSAGE_TYPE, messageType as Parcelable)
      intent.putExtra(EXTRA_KEY_MESSAGE_ENCRYPTION_TYPE, msgEncryptionType as Parcelable)
      intent.putExtra(EXTRA_KEY_SERVICE_INFO, serviceInfo)
      return intent
    }
  }
}
