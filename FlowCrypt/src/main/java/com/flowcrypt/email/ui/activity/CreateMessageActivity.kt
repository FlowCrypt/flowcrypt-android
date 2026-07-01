/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.ui.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.navigation.NavHostController
import androidx.navigation.ui.AppBarConfiguration
import com.flowcrypt.email.Constants
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
import com.flowcrypt.email.ui.activity.fragment.CreateMessageFragmentArgs
import com.flowcrypt.email.ui.activity.fragment.dialog.ChoosePublicKeyDialogFragment
import com.flowcrypt.email.util.FileAndDirectoryUtils
import com.flowcrypt.email.util.FlavorSettings
import java.io.File

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
    sanitizeIntentForNavigation(intent)
    enableEdgeToEdge()
    super.onCreate(savedInstanceState)
    (navController as? NavHostController)?.enableOnBackPressed(true)
    isNavigationArrowDisplayed = true
    val navGraph = navController.navInflater.inflate(R.navigation.create_msg_graph)
    navController.setGraph(navGraph, createStartDestinationArgs(intent))
    FileAndDirectoryUtils.cleanDir(File(cacheDir, Constants.DRAFT_CACHE_DIR))
    applyInsetsToSupportEdgeToEdge()
  }

  override fun onNewIntent(intent: Intent) {
    sanitizeIntentForNavigation(intent)
    setIntent(intent)
    super.onNewIntent(intent)
    if (intent.action in PUBLIC_INTENT_ACTIONS) {
      recreate()
    }
  }

  private fun sanitizeIntentForNavigation(intent: Intent) {
    val originalExtras = intent.extras ?: return
    val sanitizedExtras = Bundle(originalExtras).apply {
      NAVIGATION_DEEP_LINK_EXTRA_KEYS.forEach(::remove)
    }
    intent.replaceExtras(sanitizedExtras)
  }

  private fun createStartDestinationArgs(intent: Intent): Bundle? {
    return if (intent.action in PUBLIC_INTENT_ACTIONS) {
      Bundle.EMPTY
    } else {
      intent.extras?.let { CreateMessageFragmentArgs.fromBundle(it).toBundle() }
    }
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

  fun applyInsetsToSupportEdgeToEdge() {
    ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
      val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
      binding.appBarLayout.updatePadding(top = bars.top)
      binding.root.updatePadding(bottom = bars.bottom)
      insets
    }
  }

  companion object {
    private const val EXTRA_KEY_INCOMING_MESSAGE_INFO = "incomingMessageInfo"
    private const val EXTRA_KEY_ATTACHMENTS = "attachments"
    private const val EXTRA_KEY_MESSAGE_TYPE = "messageType"
    private const val EXTRA_KEY_ENCRYPTED_BY_DEFAULT = "encryptedByDefault"
    private const val EXTRA_KEY_SERVICE_INFO = "serviceInfo"
    private val NAVIGATION_DEEP_LINK_EXTRA_KEYS = setOf(
      "android-support-nav:controller:deepLinkIds",
      "android-support-nav:controller:deepLinkArgs",
      "android-support-nav:controller:deepLinkExtras",
      "android-support-nav:controller:deepLinkHandled",
      "android-support-nav:controller:deepLinkIntent",
    )
    private val PUBLIC_INTENT_ACTIONS = setOf(
      Intent.ACTION_VIEW,
      Intent.ACTION_SENDTO,
      Intent.ACTION_SEND,
      Intent.ACTION_SEND_MULTIPLE
    )

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
      intent.putExtra(EXTRA_KEY_INCOMING_MESSAGE_INFO, msgInfo)
      intent.putExtra(EXTRA_KEY_ATTACHMENTS, attachments)
      intent.putExtra(EXTRA_KEY_MESSAGE_TYPE, messageType)
      intent.putExtra(
        EXTRA_KEY_ENCRYPTED_BY_DEFAULT,
        msgEncryptionType == MessageEncryptionType.ENCRYPTED
      )
      intent.putExtra(EXTRA_KEY_SERVICE_INFO, serviceInfo)
      return intent
    }
  }
}
