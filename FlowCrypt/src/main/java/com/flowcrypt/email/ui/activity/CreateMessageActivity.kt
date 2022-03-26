/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Toast
import com.flowcrypt.email.R
import com.flowcrypt.email.api.email.model.IncomingMessageInfo
import com.flowcrypt.email.api.email.model.ServiceInfo
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.databinding.ActivityCreateMessageBinding
import com.flowcrypt.email.extensions.decrementSafely
import com.flowcrypt.email.extensions.incrementSafely
import com.flowcrypt.email.model.MessageEncryptionType
import com.flowcrypt.email.model.MessageType
import com.flowcrypt.email.ui.activity.fragment.dialog.ChoosePublicKeyDialogFragment

/**
 * This activity describes a logic of send encrypted or standard message.
 *
 * @author DenBond7
 * Date: 10.05.2017
 * Time: 11:43
 * E-mail: DenBond7@gmail.com
 */
class CreateMessageActivity : BaseActivity<ActivityCreateMessageBinding>(),
  ChoosePublicKeyDialogFragment.OnLoadKeysProgressListener {

  override fun inflateBinding(inflater: LayoutInflater): ActivityCreateMessageBinding =
    ActivityCreateMessageBinding.inflate(layoutInflater)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val navGraph = navController.navInflater.inflate(R.navigation.nav_graph)
    navGraph.startDestination = R.id.createMessageFragment
    navController.setGraph(navGraph, intent.extras)
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

  companion object {
    fun generateIntent(
      context: Context,
      msgInfo: IncomingMessageInfo?,
      msgEncryptionType: MessageEncryptionType
    ): Intent {
      return generateIntent(context, msgInfo, MessageType.NEW, msgEncryptionType)
    }

    fun generateIntent(
      context: Context?,
      msgInfo: IncomingMessageInfo?,
      @MessageType messageType: Int,
      msgEncryptionType: MessageEncryptionType?,
      serviceInfo: ServiceInfo? = null
    ): Intent {
      val intent = Intent(context, CreateMessageActivity::class.java)
      intent.putExtra("incomingMessageInfo", msgInfo)
      intent.putExtra("messageType", messageType)
      intent.putExtra("encryptedByDefault", msgEncryptionType == MessageEncryptionType.ENCRYPTED)
      intent.putExtra("serviceInfo", serviceInfo)
      return intent
    }
  }
}
