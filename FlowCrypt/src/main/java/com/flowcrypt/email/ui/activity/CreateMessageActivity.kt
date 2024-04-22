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
import androidx.activity.OnBackPressedCallback
import androidx.navigation.NavHostController
import androidx.navigation.ui.AppBarConfiguration
import com.flowcrypt.email.R
import com.flowcrypt.email.api.email.model.AttachmentInfo
import com.flowcrypt.email.api.email.model.IncomingMessageInfo
import com.flowcrypt.email.api.email.model.ServiceInfo
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.databinding.ActivityCreateMessageBinding
import com.flowcrypt.email.extensions.decrementSafely
import com.flowcrypt.email.extensions.incrementSafely
import com.flowcrypt.email.extensions.toast
import com.flowcrypt.email.model.MessageEncryptionType
import com.flowcrypt.email.model.MessageType
import com.flowcrypt.email.ui.activity.fragment.dialog.ChoosePublicKeyDialogFragment
import com.flowcrypt.email.util.FlavorSettings

/**
 * This activity describes a logic of send encrypted or standard message.
 *
 * @author Denys Bondarenko
 */
class CreateMessageActivity : BaseActivity<ActivityCreateMessageBinding>(),
  ChoosePublicKeyDialogFragment.OnLoadKeysProgressListener {

  override fun inflateBinding(inflater: LayoutInflater): ActivityCreateMessageBinding =
    ActivityCreateMessageBinding.inflate(layoutInflater)

  override fun initAppBarConfiguration(): AppBarConfiguration {
    return AppBarConfiguration(topLevelDestinationIds = emptySet(), fallbackOnNavigateUpListener = {
      if (navController.navigateUp()) {
        true
      } else {
        finish()
        false
      }
    })
  }

  override val onBackPressedCallback = object : OnBackPressedCallback(true) {
    override fun handleOnBackPressed() {
      if (navController.currentDestination?.id == R.id.createMessageFragment) {
        onBackPressed()
      } else {
        navController.navigateUp()
      }
    }

    private fun onBackPressed() {
      isEnabled = false
      onBackPressedDispatcher.onBackPressed()
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    (navController as? NavHostController)?.enableOnBackPressed(true)
    isNavigationArrowDisplayed = true
    val navGraph = navController.navInflater.inflate(R.navigation.create_msg_graph)
    navController.setGraph(navGraph, intent.extras)
  }

  override fun onAccountInfoRefreshed(accountEntity: AccountEntity?) {
    super.onAccountInfoRefreshed(accountEntity)
    //check create a message from extra info when account didn't setup
    if (activeAccount == null) {
      toast(R.string.setup_app, Toast.LENGTH_LONG)
      finish()
      startActivity(Intent(this, MainActivity::class.java))
    }
  }

  override fun onLoadKeysProgress(status: Result.Status) {
    if (status == Result.Status.LOADING) {
      FlavorSettings.getCountingIdlingResource().incrementSafely(this@CreateMessageActivity)
    } else {
      FlavorSettings.getCountingIdlingResource().decrementSafely(this@CreateMessageActivity)
    }
  }

  companion object {
    fun generateIntent(
      context: Context?,
      @MessageType messageType: Int,
      msgEncryptionType: MessageEncryptionType = MessageEncryptionType.ENCRYPTED,
      msgInfo: IncomingMessageInfo? = null,
      attachments: Array<AttachmentInfo>? = null,
      serviceInfo: ServiceInfo? = null
    ): Intent {
      val intent = Intent(context, CreateMessageActivity::class.java)
      intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
      intent.putExtra("incomingMessageInfo", msgInfo)
      intent.putExtra("attachments", attachments)
      intent.putExtra("messageType", messageType)
      intent.putExtra("encryptedByDefault", msgEncryptionType == MessageEncryptionType.ENCRYPTED)
      intent.putExtra("serviceInfo", serviceInfo)
      return intent
    }
  }
}
