/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.notifications

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import androidx.core.app.NotificationCompat
import androidx.navigation.NavDeepLinkBuilder
import com.flowcrypt.email.BuildConfig
import com.flowcrypt.email.R
import com.flowcrypt.email.database.FlowCryptRoomDatabase
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.ui.activity.MainActivity

/**
 * It's a manager which helps to show errors notifications with a high priority.
 *
 * @author Denis Bondarenko
 *         Date: 6/26/20
 *         Time: 5:28 PM
 *         E-mail: DenBond7@gmail.com
 */
class ErrorNotificationManager(context: Context) : CustomNotificationManager(context) {
  override val groupName: String = GROUP_NAME_ERRORS
  override val groupId: Int = NOTIFICATIONS_GROUP_ERROR

  fun notifyUserAboutProblemWithOutgoingMsgs(account: AccountEntity, messageCount: Int) {
    val pendingIntent = NavDeepLinkBuilder(context)
      .setGraph(R.navigation.nav_graph)
      .setDestination(R.id.messagesListFragment)
      .setComponentName(MainActivity::class.java)
      .createTaskStackBuilder()
      .getPendingIntent(
        System.currentTimeMillis().toInt(),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
      )

    val contentText = context.resources.getQuantityString(
      R.plurals.has_failed_outgoing_msgs,
      messageCount, messageCount
    )
    val builder = NotificationCompat.Builder(context, NotificationChannelManager.CHANNEL_ID_ERRORS)
      .setSmallIcon(R.drawable.ic_create_message_failed_grey_24dp)
      .setLargeIcon(BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher))
      .setContentText(contentText)
      .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
      .setContentIntent(pendingIntent)
      .setPriority(NotificationCompat.PRIORITY_HIGH)
      .setCategory(NotificationCompat.CATEGORY_ERROR)
      .setAutoCancel(true)
      .setGroup(GROUP_NAME_ERRORS)
      .setSubText(account.email)
      .setOngoing(true)
      .setDefaults(Notification.DEFAULT_ALL)
      .setGroup(GROUP_NAME_ERRORS)
      .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_SUMMARY)

    prepareAndShowNotificationsGroup(context)

    notify(
      groupName,
      R.id.notification_id_has_failed_outgoing_msgs,
      builder.build()
    )
  }

  fun notifyUserAboutAuthFailure(account: AccountEntity, recoverableIntent: Intent? = null) {
    if (!isShowingAuthErrorEnabled) return

    val refreshedAccount =
      FlowCryptRoomDatabase.getDatabase(context).accountDao().getAccountById(account.id)
    if (refreshedAccount?.isActive == false) return

    val intent = getFixAuthIssueIntent(context, account, recoverableIntent)

    val pendingIntent =
      PendingIntent.getActivity(
        context,
        System.currentTimeMillis().toInt(),
        intent,
        PendingIntent.FLAG_IMMUTABLE
      )

    val contentText = when (account.accountType) {
      AccountEntity.ACCOUNT_TYPE_GOOGLE, AccountEntity.ACCOUNT_TYPE_OUTLOOK -> context.getString(
        R.string.auth_failure_hint,
        context.getString(R.string.app_name)
      )
      else -> context.getString(R.string.auth_failure_hint_regular_accounts)
    }

    val builder = NotificationCompat.Builder(context, NotificationChannelManager.CHANNEL_ID_ERRORS)
      .setSmallIcon(android.R.drawable.stat_sys_warning)
      .setContentTitle(context.getString(R.string.auth_failure))
      .setContentText(contentText)
      .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
      .setContentIntent(pendingIntent)
      .setPriority(NotificationCompat.PRIORITY_HIGH)
      .setCategory(NotificationCompat.CATEGORY_ERROR)
      .setAutoCancel(true)
      .setGroup(GROUP_NAME_ERRORS)
      .setSubText(account.email)
      .setOngoing(false)
      .setOnlyAlertOnce(true)
      .setDefaults(Notification.DEFAULT_ALL)
      .setGroup(GROUP_NAME_ERRORS)

    prepareAndShowNotificationsGroup(context)

    notify(groupName, R.id.notification_id_auth_failure, builder.build())
  }

  /**
   * Prepare and show the notifications group.
   *
   * @param context Interface to global information about an application environment.
   */
  private fun prepareAndShowNotificationsGroup(context: Context) {
    val groupBuilder = NotificationCompat.Builder(
      context, NotificationChannelManager.CHANNEL_ID_ERRORS
    )
      .setSmallIcon(android.R.drawable.stat_sys_warning)
      .setGroup(GROUP_NAME_ERRORS)
      .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_SUMMARY)
      .setDefaults(Notification.DEFAULT_ALL)
      .setAutoCancel(true)
      .setGroupSummary(true)
      .setOnlyAlertOnce(true)
    notify(groupName, groupId, groupBuilder.build())
  }

  companion object {
    var isShowingAuthErrorEnabled: Boolean = true

    const val GROUP_NAME_ERRORS = BuildConfig.APPLICATION_ID + ".ERRORS"
    const val NOTIFICATIONS_GROUP_ERROR = -4

    fun getFixAuthIssueIntent(
      context: Context,
      account: AccountEntity?,
      recoverableIntent: Intent?
    ): Intent {
      return when (account?.accountType) {
        AccountEntity.ACCOUNT_TYPE_GOOGLE, AccountEntity.ACCOUNT_TYPE_OUTLOOK -> {
          val uri = Uri.parse("flowcrypt://email.flowcrypt.com/sign-in/recover_auth")
          Intent(null, uri).apply {
            recoverableIntent?.let {
              val extras = Bundle().apply {
                putParcelable("recoverableIntent", recoverableIntent)
              }
              putExtra("android-support-nav:controller:deepLinkExtras", extras)
            }
          }
        }

        else -> Intent(null, Uri.parse(context.getString(R.string.deep_link_server_settings)))
      }
    }
  }
}
