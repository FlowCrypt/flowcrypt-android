/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.navigation.NavDeepLinkBuilder
import androidx.preference.PreferenceManager
import com.flowcrypt.email.BuildConfig
import com.flowcrypt.email.Constants
import com.flowcrypt.email.R
import com.flowcrypt.email.api.email.EmailUtil
import com.flowcrypt.email.api.email.FoldersManager
import com.flowcrypt.email.api.email.model.LocalFolder
import com.flowcrypt.email.broadcastreceivers.MarkMessagesAsOldBroadcastReceiver
import com.flowcrypt.email.database.FlowCryptRoomDatabase
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.database.entity.MessageEntity
import com.flowcrypt.email.ui.activity.MainActivity
import com.flowcrypt.email.ui.activity.fragment.MessageDetailsFragmentArgs
import com.flowcrypt.email.ui.activity.fragment.ThreadDetailsFragmentArgs
import com.flowcrypt.email.ui.activity.fragment.preferences.NotificationsSettingsFragment
import com.flowcrypt.email.ui.notifications.CustomNotificationManager
import com.flowcrypt.email.ui.notifications.NotificationChannelManager
import com.flowcrypt.email.util.GeneralUtil
import com.flowcrypt.email.util.SharedPreferencesHelper
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

/**
 * This manager is responsible for displaying messages notifications.
 *
 * @author Denys Bondarenko
 */
class MessagesNotificationManager(context: Context) : CustomNotificationManager(context) {

  override val groupName: String = GROUP_NAME_FLOWCRYPT_MESSAGES
  override val groupId: Int = NOTIFICATIONS_GROUP_MESSAGES

  /**
   * Show a [Notification] of an incoming message.
   *
   * @param context               Interface to global information about an application environment.
   * @param account               An [AccountEntity] object which contains information about an email account.
   * @param localFolder           A local implementation of a remote folder.
   * @param msgs                  A list of models which consists information about some messages.
   */
  fun notify(
    context: Context,
    account: AccountEntity?,
    localFolder: LocalFolder,
    msgs: List<MessageEntity>?
  ) {

    if (account == null || msgs.isNullOrEmpty()) {
      notificationManagerCompat.cancel(NOTIFICATIONS_GROUP_MESSAGES)
      return
    }

    val isNotificationDisabled = NotificationsSettingsFragment.NOTIFICATION_LEVEL_NEVER ==
        SharedPreferencesHelper.getString(
          PreferenceManager.getDefaultSharedPreferences(context),
          Constants.PREF_KEY_MESSAGES_NOTIFICATION_FILTER, ""
        )

    if (isNotificationDisabled) {
      return
    }

    notifyWithGroupSupport(context, account, localFolder, msgs)
  }

  @OptIn(DelicateCoroutinesApi::class)
  fun cancelAll(account: AccountEntity, foldersManager: FoldersManager) {
    cancel(NOTIFICATIONS_GROUP_MESSAGES)
    val localFolder = foldersManager.findInboxFolder()

    if (localFolder != null) {
      GlobalScope.launch {
        val roomDatabase = FlowCryptRoomDatabase.getDatabase(context)
        roomDatabase.msgDao().markMsgsAsOld(account.email, localFolder.fullName)
      }
    }
  }

  private fun notifyWithGroupSupport(
    context: Context, account: AccountEntity,
    localFolder: LocalFolder, msgs: List<MessageEntity>
  ) {

    val isEncryptedModeEnabled =
      NotificationsSettingsFragment.NOTIFICATION_LEVEL_ENCRYPTED_MESSAGES_ONLY ==
          SharedPreferencesHelper.getString(
            PreferenceManager.getDefaultSharedPreferences(context),
            Constants.PREF_KEY_MESSAGES_NOTIFICATION_FILTER, ""
          )

    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    prepareAndShowMsgGroup(context, account, localFolder, manager, msgs)

    for (msg in msgs) {
      if (isEncryptedModeEnabled && msg.isEncrypted == false) {
        continue
      }

      val builder = NotificationCompat.Builder(
        context, NotificationChannelManager.CHANNEL_ID_MESSAGES
      ).setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_SUMMARY)
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setCategory(NotificationCompat.CATEGORY_EMAIL)
        .setSmallIcon(R.drawable.ic_email_encrypted)
        .setLargeIcon(generateLargeIcon(context))
        .setColor(
          ContextCompat.getColor(
            context, if (msg.isEncrypted == true)
              R.color.colorPrimary
            else
              R.color.red
          )
        )
        .setDeleteIntent(
          genDeletePendingIntent(
            context = context,
            requestCode = msg.uid.toInt(),
            account = account,
            localFolder = localFolder,
            msgs = listOf(msg)
          )
        )
        .setAutoCancel(true)
        .setContentTitle(EmailUtil.getFirstAddressString(msg.from))
        .setStyle(NotificationCompat.BigTextStyle().bigText(msg.subject))
        .setGroup(GROUP_NAME_FLOWCRYPT_MESSAGES)
        .setContentText(msg.subject)
        .setContentIntent(
          getMsgDetailsPendingIntent(
            context = context,
            accountEntity = account,
            requestCode = msg.uid.toInt(),
            localFolder = localFolder,
            messageEntity = msg
          )
        )
        .setDefaults(Notification.DEFAULT_ALL)
        .setSubText(account.email)

      notify(msg.uidAsHEX, -1, builder.build())
    }
  }

  private fun prepareAndShowMsgGroup(
    context: Context, account: AccountEntity, localFolder: LocalFolder,
    notificationManager: NotificationManager,
    msgs: List<MessageEntity>
  ) {
    val isEncryptedModeEnabled =
      NotificationsSettingsFragment.NOTIFICATION_LEVEL_ENCRYPTED_MESSAGES_ONLY ==
          SharedPreferencesHelper.getString(
            PreferenceManager.getDefaultSharedPreferences(context),
            Constants.PREF_KEY_MESSAGES_NOTIFICATION_FILTER, ""
          )

    if (isEncryptedModeEnabled) {
      var isEncryptedMsgFound = false
      for (msg in msgs) {
        if (msg.isEncrypted == true) {
          isEncryptedMsgFound = true
          break
        }
      }

      if (!isEncryptedMsgFound) {
        return
      }
    }

    var groupResourceId = R.drawable.ic_email_encrypted

    if (msgs.size > 1) {
      groupResourceId = R.drawable.ic_email_multiply_encrypted
    } else {
      for (statusBarNotification in notificationManager.activeNotifications) {
        if (GROUP_NAME_FLOWCRYPT_MESSAGES == statusBarNotification.notification.group) {
          groupResourceId = R.drawable.ic_email_multiply_encrypted
          break
        }
      }
    }

    val groupBuilder =
      NotificationCompat.Builder(context, NotificationChannelManager.CHANNEL_ID_MESSAGES)
        .setSmallIcon(groupResourceId)
        .setContentInfo(account.email)
        .setColor(ContextCompat.getColor(context, R.color.colorPrimary))
        .setSubText(account.email)
        .setGroup(GROUP_NAME_FLOWCRYPT_MESSAGES)
        .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_SUMMARY)
        .setContentIntent(getInboxPendingIntent(context))
        .setDeleteIntent(
          genDeletePendingIntent(
            context = context,
            requestCode = NOTIFICATIONS_GROUP_MESSAGES,
            account = account,
            localFolder = localFolder,
            msgs = msgs
          )
        )
        .setDefaults(Notification.DEFAULT_ALL)
        .setAutoCancel(true)
        .setGroupSummary(true)
    notificationManager.notify(groupName, groupId, groupBuilder.build())
  }

  private fun getInboxPendingIntent(context: Context): PendingIntent {
    val inboxIntent = Intent(context, MainActivity::class.java)
    inboxIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    return PendingIntent.getActivity(
      context,
      0,
      inboxIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
  }

  private fun genDeletePendingIntent(
    context: Context,
    requestCode: Int,
    account: AccountEntity,
    localFolder: LocalFolder,
    msgs: List<MessageEntity>
  ): PendingIntent {
    val intent = Intent(context, MarkMessagesAsOldBroadcastReceiver::class.java)
    intent.action = MarkMessagesAsOldBroadcastReceiver.ACTION_MARK_MESSAGES_AS_OLD
    intent.putExtra(MarkMessagesAsOldBroadcastReceiver.EXTRA_KEY_EMAIL, account.email)
    intent.putExtra(MarkMessagesAsOldBroadcastReceiver.EXTRA_KEY_LABEL, localFolder.fullName)

    if (msgs.isNotEmpty()) {
      intent.putStringArrayListExtra(
        MarkMessagesAsOldBroadcastReceiver.EXTRA_KEY_UID_LIST,
        ArrayList(msgs.map { it.uid.toString() })
      )
    }

    return PendingIntent.getBroadcast(
      context,
      requestCode,
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
  }

  private fun generateLargeIcon(context: Context): Bitmap {
    val drawable = ResourcesCompat.getDrawable(
      context.resources, R.mipmap.ic_launcher,
      context.theme
    ) ?: return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
    return GeneralUtil.drawableToBitmap(drawable)
  }

  private fun getMsgDetailsPendingIntent(
    context: Context,
    accountEntity: AccountEntity,
    requestCode: Int,
    localFolder: LocalFolder,
    messageEntity: MessageEntity
  ): PendingIntent {
    val navDeepLinkBuilder = NavDeepLinkBuilder(context)
      .setGraph(R.navigation.nav_graph)
      .setComponentName(MainActivity::class.java)

    if (accountEntity.isGoogleSignInAccount
      && accountEntity.useAPI
      && accountEntity.useConversationMode
      && messageEntity.id != null
    ) {
      navDeepLinkBuilder
        .setDestination(R.id.threadDetailsFragment)
        .setArguments(
          ThreadDetailsFragmentArgs(
            messageEntityId = messageEntity.id
          ).toBundle()
        )
    } else {
      navDeepLinkBuilder
        .setDestination(R.id.messageDetailsFragment)
        .setArguments(
          MessageDetailsFragmentArgs(
            messageEntity = messageEntity,
            localFolder = localFolder
          ).toBundle()
        )
    }

    return requireNotNull(
      navDeepLinkBuilder
        .createTaskStackBuilder()
        .getPendingIntent(
          requestCode, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    )
  }

  companion object {
    const val GROUP_NAME_FLOWCRYPT_MESSAGES = BuildConfig.APPLICATION_ID + ".MESSAGES"
    const val NOTIFICATIONS_GROUP_MESSAGES = -1
  }
}
