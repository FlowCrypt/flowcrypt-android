/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.TaskStackBuilder
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.preference.PreferenceManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.flowcrypt.email.BuildConfig
import com.flowcrypt.email.Constants
import com.flowcrypt.email.R
import com.flowcrypt.email.api.email.EmailUtil
import com.flowcrypt.email.api.email.FoldersManager
import com.flowcrypt.email.api.email.model.LocalFolder
import com.flowcrypt.email.broadcastreceivers.MarkMessagesAsOldBroadcastReceiver
import com.flowcrypt.email.database.FlowCryptRoomDatabase
import com.flowcrypt.email.database.dao.source.AccountDao
import com.flowcrypt.email.database.entity.MessageEntity
import com.flowcrypt.email.ui.activity.EmailManagerActivity
import com.flowcrypt.email.ui.activity.MessageDetailsActivity
import com.flowcrypt.email.ui.activity.fragment.preferences.NotificationsSettingsFragment
import com.flowcrypt.email.ui.notifications.CustomNotificationManager
import com.flowcrypt.email.ui.notifications.NotificationChannelManager
import com.flowcrypt.email.util.GeneralUtil
import com.flowcrypt.email.util.SharedPreferencesHelper
import com.google.android.gms.common.util.CollectionUtils
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.*

/**
 * This manager is responsible for displaying messages notifications.
 *
 * @author Denis Bondarenko
 * Date: 23.06.2018
 * Time: 12:10
 * E-mail: DenBond7@gmail.com
 */
class MessagesNotificationManager(context: Context) : CustomNotificationManager(context) {

  override val groupName: String = GROUP_NAME_FLOWCRYPT_MESSAGES
  override val groupId: Int = NOTIFICATIONS_GROUP_MESSAGES

  /**
   * Show a [Notification] of an incoming message.
   *
   * @param context               Interface to global information about an application environment.
   * @param account               An [AccountDao] object which contains information about an email account.
   * @param localFolder           A local implementation of a remote folder.
   * @param msgs                  A list of models which consists information about some messages.
   * @param uidListOfUnseenMsgs   A list of UID of unseen messages.
   * @param isSilent              true if we don't need sound and vibration for Android 7.0 and below.
   */
  fun notify(context: Context, account: AccountDao?, localFolder: LocalFolder,
             msgs: List<MessageEntity>?, uidListOfUnseenMsgs: List<Long>,
             isSilent: Boolean) {

    if (account == null || msgs == null || msgs.isEmpty()) {
      notificationManagerCompat.cancel(NOTIFICATIONS_GROUP_MESSAGES)
      return
    }

    val isNotificationDisabled = NotificationsSettingsFragment.NOTIFICATION_LEVEL_NEVER ==
        SharedPreferencesHelper.getString(PreferenceManager.getDefaultSharedPreferences(context),
            Constants.PREF_KEY_MESSAGES_NOTIFICATION_FILTER, "")

    if (isNotificationDisabled) {
      return
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
      notifyWithGroupSupport(context, account, localFolder, msgs)
    } else {
      notifyWithSingleNotification(context, account, localFolder, msgs, uidListOfUnseenMsgs, isSilent)
    }
  }

  fun cancelAll(context: Context, account: AccountDao) {
    cancel(NOTIFICATIONS_GROUP_MESSAGES)

    val foldersManager = FoldersManager.fromDatabase(context, account.email)
    val localFolder = foldersManager.findInboxFolder()

    if (localFolder != null) {
      GlobalScope.launch {
        val roomDatabase = FlowCryptRoomDatabase.getDatabase(context)
        roomDatabase.msgDao().markMsgsAsOld(account.email, localFolder.fullName)
      }
    }
  }

  private fun notifyWithSingleNotification(context: Context, account: AccountDao,
                                           folder: LocalFolder, msgs: List<MessageEntity>,
                                           uidOfUnseenMsgs: List<Long>, isSilent: Boolean) {
    val onlyEncrypted = NotificationsSettingsFragment.NOTIFICATION_LEVEL_ENCRYPTED_MESSAGES_ONLY ==
        SharedPreferencesHelper.getString(PreferenceManager.getDefaultSharedPreferences(context),
            Constants.PREF_KEY_MESSAGES_NOTIFICATION_FILTER, "")

    val builder = NotificationCompat.Builder(context, NotificationChannelManager.CHANNEL_ID_MESSAGES)
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setCategory(NotificationCompat.CATEGORY_EMAIL)
        .setSmallIcon(R.drawable.ic_email_encrypted)
        .setLargeIcon(BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher))
        .setColor(ContextCompat.getColor(context, R.color.colorPrimary))
        .setAutoCancel(true)
        .setGroup(GROUP_NAME_FLOWCRYPT_MESSAGES)
        .setSubText(account.email)

    if (!isSilent) {
      builder.setDefaults(Notification.DEFAULT_ALL)
    }

    if (uidOfUnseenMsgs.size > 1) {
      builder.setNumber(uidOfUnseenMsgs.size)
    }

    if (msgs.size > 1) {
      var hasAllowedNotifications = false

      val inboxStyle = NotificationCompat.InboxStyle()
      for (msgEntity in msgs) {
        if (onlyEncrypted && msgEntity.isEncrypted == false) {
          continue
        }

        hasAllowedNotifications = true
        inboxStyle.addLine(formatInboxStyleLine(context, EmailUtil.getFirstAddressString
        (msgEntity.from), msgEntity.subject))
      }

      if (!hasAllowedNotifications) {
        return
      }

      builder.setStyle(inboxStyle)
          .setSmallIcon(R.drawable.ic_email_multiply_encrypted)
          .setContentIntent(getInboxPendingIntent(context))
          .setDeleteIntent(genDeletePendingIntent(context, NOTIFICATIONS_GROUP_MESSAGES, account, folder, msgs))
          .setContentTitle(context.getString(R.string.incoming_message, msgs.size))
    } else {
      val msgDetails = msgs[0]

      if (onlyEncrypted && msgDetails.isEncrypted == false) {
        return
      }

      val style = NotificationCompat.BigTextStyle().bigText(formatText(msgDetails.subject,
          ContextCompat.getColor(context, android.R.color.black)))

      val contentText = formatText(msgDetails.subject,
          ContextCompat.getColor(context, android.R.color.black))

      builder.setContentText(contentText)
          .setContentIntent(getMsgDetailsPendingIntent(context, NOTIFICATIONS_GROUP_MESSAGES, folder, msgDetails))
          .setContentTitle(EmailUtil.getFirstAddressString(msgDetails.from))
          .setStyle(style)
          .setDeleteIntent(genDeletePendingIntent(context, NOTIFICATIONS_GROUP_MESSAGES, account, folder, msgs))
          .setColor(ContextCompat.getColor(context, if (msgDetails.isEncrypted == true)
            R.color.colorPrimary else R.color.red))
          .setSmallIcon(R.drawable.ic_email_encrypted)
    }

    notificationManagerCompat.notify(groupName, NOTIFICATIONS_GROUP_MESSAGES, builder.build())
  }

  private fun notifyWithGroupSupport(context: Context, account: AccountDao,
                                     localFolder: LocalFolder, msgs: List<MessageEntity>) {

    val isEncryptedModeEnabled =
        NotificationsSettingsFragment.NOTIFICATION_LEVEL_ENCRYPTED_MESSAGES_ONLY ==
            SharedPreferencesHelper.getString(PreferenceManager.getDefaultSharedPreferences(context),
                Constants.PREF_KEY_MESSAGES_NOTIFICATION_FILTER, "")

    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    prepareAndShowMsgGroup(context, account, localFolder, manager, msgs)

    for (msg in msgs) {
      if (isEncryptedModeEnabled && msg.isEncrypted == false) {
        continue
      }

      val builder = NotificationCompat.Builder(context, NotificationChannelManager
          .CHANNEL_ID_MESSAGES)
          .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_SUMMARY)
          .setPriority(NotificationCompat.PRIORITY_DEFAULT)
          .setCategory(NotificationCompat.CATEGORY_EMAIL)
          .setSmallIcon(R.drawable.ic_email_encrypted)
          .setLargeIcon(generateLargeIcon(context))
          .setColor(ContextCompat.getColor(context, if (msg.isEncrypted == true)
            R.color.colorPrimary
          else
            R.color.red))
          .setDeleteIntent(genDeletePendingIntent(context, msg.uid.toInt(), account, localFolder, msg))
          .setAutoCancel(true)
          .setContentTitle(EmailUtil.getFirstAddressString(msg.from))
          .setStyle(NotificationCompat.BigTextStyle().bigText(msg.subject))
          .setGroup(GROUP_NAME_FLOWCRYPT_MESSAGES)
          .setContentText(msg.subject)
          .setContentIntent(getMsgDetailsPendingIntent(context, msg.uid.toInt(),
              localFolder, msg))
          .setDefaults(Notification.DEFAULT_ALL)
          .setSubText(account.email)

      notificationManagerCompat.notify(groupName, msg.uid.toInt(), builder.build())
    }
  }

  private fun prepareAndShowMsgGroup(context: Context, account: AccountDao, localFolder: LocalFolder,
                                     notificationManager: NotificationManager,
                                     msgs: List<MessageEntity>) {
    val isEncryptedModeEnabled = NotificationsSettingsFragment.NOTIFICATION_LEVEL_ENCRYPTED_MESSAGES_ONLY ==
        SharedPreferencesHelper.getString(PreferenceManager.getDefaultSharedPreferences(context),
            Constants.PREF_KEY_MESSAGES_NOTIFICATION_FILTER, "")

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

    val groupBuilder = NotificationCompat.Builder(context, NotificationChannelManager.CHANNEL_ID_MESSAGES)
        .setSmallIcon(groupResourceId)
        .setContentInfo(account.email)
        .setColor(ContextCompat.getColor(context, R.color.colorPrimary))
        .setSubText(account.email)
        .setGroup(GROUP_NAME_FLOWCRYPT_MESSAGES)
        .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_SUMMARY)
        .setContentIntent(getInboxPendingIntent(context))
        .setDeleteIntent(genDeletePendingIntent(context, NOTIFICATIONS_GROUP_MESSAGES,
            account, localFolder, msgs))
        .setDefaults(Notification.DEFAULT_ALL)
        .setAutoCancel(true)
        .setGroupSummary(true)
    notificationManager.notify(groupName, groupId, groupBuilder.build())
  }

  private fun getInboxPendingIntent(context: Context): PendingIntent {
    val inboxIntent = Intent(context, EmailManagerActivity::class.java)
    inboxIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    return PendingIntent.getActivity(context, 0, inboxIntent, PendingIntent.FLAG_UPDATE_CURRENT)
  }

  private fun genDeletePendingIntent(context: Context, requestCode: Int,
                                     account: AccountDao, localFolder: LocalFolder,
                                     messageEntity: MessageEntity): PendingIntent {
    val msgs = ArrayList<MessageEntity>()
    msgs.add(messageEntity)
    return genDeletePendingIntent(context, requestCode, account, localFolder, msgs)
  }

  private fun genDeletePendingIntent(context: Context, requestCode: Int,
                                     account: AccountDao, localFolder: LocalFolder,
                                     msgs: List<MessageEntity>): PendingIntent {
    val intent = Intent(context, MarkMessagesAsOldBroadcastReceiver::class.java)
    intent.action = MarkMessagesAsOldBroadcastReceiver.ACTION_MARK_MESSAGES_AS_OLD
    intent.putExtra(MarkMessagesAsOldBroadcastReceiver.EXTRA_KEY_EMAIL, account.email)
    intent.putExtra(MarkMessagesAsOldBroadcastReceiver.EXTRA_KEY_LABEL, localFolder.fullName)

    if (!CollectionUtils.isEmpty(msgs)) {
      val uidList = ArrayList<String>()
      for (msg in msgs) {
        uidList.add(msg.uid.toString())
      }
      intent.putStringArrayListExtra(MarkMessagesAsOldBroadcastReceiver.EXTRA_KEY_UID_LIST, uidList)
    }

    return PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT)
  }

  private fun generateLargeIcon(context: Context): Bitmap {
    return GeneralUtil.drawableToBitmap(context.resources.getDrawable(R.mipmap.ic_launcher, context.theme))
  }

  private fun getMsgDetailsPendingIntent(context: Context, requestCode: Int, localFolder: LocalFolder,
                                         msg: MessageEntity): PendingIntent {
    val intent = MessageDetailsActivity.getIntent(context, localFolder, msg)

    val stackBuilder = TaskStackBuilder.create(context)
    stackBuilder.addParentStack(MessageDetailsActivity::class.java)
    stackBuilder.addNextIntent(intent)

    return stackBuilder.getPendingIntent(requestCode, PendingIntent.FLAG_UPDATE_CURRENT)
  }

  companion object {
    const val GROUP_NAME_FLOWCRYPT_MESSAGES = BuildConfig.APPLICATION_ID + ".MESSAGES"
    const val NOTIFICATIONS_GROUP_MESSAGES = -1
  }
}
