/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.service.attachment

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.text.TextUtils
import android.text.format.DateUtils
import androidx.core.app.NotificationCompat
import com.flowcrypt.email.BuildConfig
import com.flowcrypt.email.Constants
import com.flowcrypt.email.R
import com.flowcrypt.email.api.email.model.AttachmentInfo
import com.flowcrypt.email.security.pgp.PgpDecryptAndOrVerify
import com.flowcrypt.email.ui.notifications.CustomNotificationManager
import com.flowcrypt.email.ui.notifications.NotificationChannelManager
import com.flowcrypt.email.util.GeneralUtil
import com.flowcrypt.email.util.exception.DecryptionException
import java.util.Random

/**
 * This manager is responsible for displaying attachment notifications.
 *
 * @author Denys Bondarenko
 */
class AttachmentNotificationManager(context: Context) : CustomNotificationManager(context) {

  override val groupName: String = GROUP_NAME_ATTACHMENTS
  override val groupId: Int = NOTIFICATIONS_GROUP_ATTACHMENTS

  /**
   * Show a [android.app.Notification] which notify that a new attachment was added to the loading queue.
   *
   * @param context Interface to global information about an application environment.
   * @param attInfo [AttachmentInfo] object which contains a detail information about an attachment.
   */
  fun attachmentAddedToLoadQueue(context: Context, attInfo: AttachmentInfo) {
    val builder = genDefBuilder(context, attInfo)
    builder.setProgress(0, 0, true)
      .addAction(genCancelDownloadAction(context, attInfo))
      .setOnlyAlertOnce(true)
      .setGroup(GROUP_NAME_ATTACHMENTS)
      .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_SUMMARY)

    notify(attInfo.id, attInfo.uid.toInt(), builder.build())
    prepareAndShowNotificationsGroup(context, attInfo, true)
  }

  /**
   * Update a [android.app.Notification] download progress.
   *
   * @param context               Interface to global information about an application environment.
   * @param attInfo               [AttachmentInfo] object which contains a detail information about an
   * attachment.
   * @param progress              The attachment loading progress in percentage.
   * @param timeLeftInMillisecond The time left in millisecond (approximately).
   */
  fun updateLoadingProgress(
    context: Context,
    attInfo: AttachmentInfo,
    progress: Int,
    timeLeftInMillisecond: Long
  ) {
    val builder = genDefBuilder(context, attInfo)

    builder.setProgress(MAX_FILE_SIZE_IN_PERCENTAGE, progress, timeLeftInMillisecond == 0L)
      .addAction(genCancelDownloadAction(context, attInfo))
      .setCategory(NotificationCompat.CATEGORY_PROGRESS)
      .setSmallIcon(android.R.drawable.stat_sys_download)
      .setContentText(DateUtils.formatElapsedTime(timeLeftInMillisecond / DateUtils.SECOND_IN_MILLIS))
      .setOnlyAlertOnce(true)
      .setGroup(GROUP_NAME_ATTACHMENTS)
      .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_SUMMARY)

    notify(attInfo.id, attInfo.uid.toInt(), builder.build())
    prepareAndShowNotificationsGroup(context, attInfo, true)
  }

  /**
   * Show a [android.app.Notification] which notify that an attachment was downloaded.
   *
   * @param context Interface to global information about an application environment.
   * @param attInfo [AttachmentInfo] object which contains a detail information about an attachment.
   * @param uri     The [Uri] of the downloaded attachment.
   * @param useContentApp     A flag that indicates should we use the Content app as a viewer.
   */
  fun downloadCompleted(
    context: Context,
    attInfo: AttachmentInfo,
    uri: Uri,
    useContentApp: Boolean = false
  ) {
    val intent = if (useContentApp) {
      GeneralUtil.genViewAttachmentIntent(uri, attInfo)
        .setPackage(Constants.APP_PACKAGE_CONTENT_LOCKER)
    } else {
      GeneralUtil.genViewAttachmentIntent(uri, attInfo)
    }
    val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
    val builder = genDefBuilder(context, attInfo)

    builder.setProgress(0, 0, false)
      .setAutoCancel(true)
      .setOngoing(false)
      .setCategory(NotificationCompat.CATEGORY_STATUS)
      .setSmallIcon(android.R.drawable.stat_sys_download_done)
      .setContentText(context.getString(R.string.download_complete))
      .setContentIntent(pendingIntent)
      .setGroup(GROUP_NAME_ATTACHMENTS)
      .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_CHILDREN)

    notify(attInfo.id, attInfo.uid.toInt(), builder.build())

    prepareAndShowNotificationsGroup(context, attInfo, false)
  }

  /**
   * Show a [android.app.Notification] which notify that an error happened while we loading an attachment.
   * The user can cancel current loading or retry loading again.
   *
   * @param context Interface to global information about an application environment.
   * @param attInfo [AttachmentInfo] object which contains a detail information about an attachment.
   * @param e       The [Exception] which contains a detail information about happened error..
   */
  fun errorHappened(context: Context, attInfo: AttachmentInfo, e: Exception) {
    val builder = genDefBuilder(context, attInfo)

    val bigText = StringBuilder()
    val contentText = StringBuilder()
    when {
      TextUtils.isEmpty(e.message) -> {
        contentText.append(context.getString(R.string.error_occurred_please_try_again))
        bigText.append(e.javaClass.simpleName)
      }

      e.cause != null -> {
        contentText.append(e.cause?.message ?: "")
        bigText.append(e.javaClass.simpleName + ": " + e.message)
      }

      else -> {
        contentText.append(e.message)
        bigText.append(e.javaClass.simpleName + ": " + e.message)
      }
    }

    if (e is DecryptionException) {
      if (e.decryptionErrorType == PgpDecryptAndOrVerify.DecryptionErrorType.NEED_PASSPHRASE) {
        contentText.clear()
        contentText.append(context.getString(R.string.provide_passphrase_to_decrypt_file))
        val fingerprint = e.fingerprints.firstOrNull()
        fingerprint?.let {
          val additionalText = context.resources.getQuantityString(
            R.plurals.please_provide_passphrase_for_following_keys,
            1
          ) + "\n" + GeneralUtil.doSectionsInText(" ", it, 4) + "\n\n"
          bigText.insert(0, additionalText)
        }
      }
    }

    builder.setProgress(0, 0, false)
      .setAutoCancel(true)
      .setOngoing(false)
      .addAction(genCancelDownloadAction(context, attInfo))
      .addAction(genRetryDownloadingAction(context, attInfo))
      .setCategory(NotificationCompat.CATEGORY_SERVICE)
      .setSmallIcon(android.R.drawable.stat_sys_download_done)
      .setContentText(contentText)
      .setGroup(GROUP_NAME_ATTACHMENTS)
      .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_CHILDREN)

    if (bigText.isNotEmpty()) {
      builder.setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
    }

    notify(attInfo.id, attInfo.uid.toInt(), builder.build())
    prepareAndShowNotificationsGroup(context, attInfo, false)
  }

  /**
   * Cancel a [android.app.Notification] when user clicked on the "Cancel" button.
   *
   * @param attInfo The [AttachmentInfo] object.
   */
  fun loadingCanceledByUser(attInfo: AttachmentInfo) {
    cancel(attInfo.id, attInfo.uid.toInt())
  }

  /**
   * Prepare a content title for a notification.
   *
   * @param attachmentInfo The [AttachmentInfo] object.
   * @return The prepared title.
   */
  private fun prepareContentTitle(attachmentInfo: AttachmentInfo): String {
    var contentTitle = attachmentInfo.getSafeName()
    if (!TextUtils.isEmpty(contentTitle) && contentTitle.length > MAX_CONTENT_TITLE_LENGTH) {
      contentTitle = contentTitle.substring(0, MAX_CONTENT_TITLE_LENGTH) + "..."
    }
    return contentTitle
  }

  /**
   * Generate a cancel download an attachment [NotificationCompat.Action].
   *
   * @param context Interface to global information about an application environment.
   * @return The created [NotificationCompat.Action].
   */
  private fun genCancelDownloadAction(
    context: Context,
    attInfo: AttachmentInfo
  ): NotificationCompat.Action {
    val intent = Intent(context, AttachmentDownloadManagerService::class.java)
    intent.action = AttachmentDownloadManagerService.ACTION_CANCEL_DOWNLOAD_ATTACHMENT
    intent.putExtra(AttachmentDownloadManagerService.EXTRA_KEY_ATTACHMENT_INFO, attInfo)

    val pendingIntent =
      PendingIntent.getService(context, Random().nextInt(), intent, PendingIntent.FLAG_IMMUTABLE)
    return NotificationCompat.Action.Builder(0, context.getString(R.string.cancel), pendingIntent)
      .build()
  }

  /**
   * Generate a retry download an attachment [NotificationCompat.Action].
   *
   * @param context Interface to global information about an application environment.
   * @param attInfo [AttachmentInfo] object which contains a detail information about an attachment.
   * @return The created [NotificationCompat.Action].
   */
  private fun genRetryDownloadingAction(
    context: Context,
    attInfo: AttachmentInfo
  ): NotificationCompat.Action {
    val intent = Intent(context, AttachmentDownloadManagerService::class.java)
    intent.action = AttachmentDownloadManagerService.ACTION_RETRY_DOWNLOAD_ATTACHMENT
    intent.putExtra(AttachmentDownloadManagerService.EXTRA_KEY_ATTACHMENT_INFO, attInfo)

    val pendingIntent =
      PendingIntent.getService(context, Random().nextInt(), intent, PendingIntent.FLAG_IMMUTABLE)
    return NotificationCompat.Action.Builder(0, context.getString(R.string.retry), pendingIntent)
      .build()
  }

  /**
   * Generate [NotificationCompat.Builder] with common values.
   *
   * @param context Interface to global information about an application environment.
   * @param attInfo [AttachmentInfo] object which contains a detail information about an attachment.
   * @return Generated [NotificationCompat.Builder].
   */
  private fun genDefBuilder(context: Context, attInfo: AttachmentInfo): NotificationCompat.Builder {
    return NotificationCompat.Builder(
      context,
      NotificationChannelManager.CHANNEL_ID_ATTACHMENTS
    )
      .setAutoCancel(false)
      .setOngoing(true)
      .setSmallIcon(android.R.drawable.stat_sys_download)
      .setContentTitle(prepareContentTitle(attInfo))
      .setContentText(context.getString(R.string.waiting_to_load))
      .setSubText(attInfo.email)
      .setDefaults(Notification.DEFAULT_ALL)
  }

  /**
   * Prepare and show the notifications group.
   *
   * @param context           Interface to global information about an application environment.
   * @param attInfo           [AttachmentInfo] object which contains a detail information about an attachment.
   * @param isProgressEnabled true if need to use the progress icon as default, otherwise false.
   */
  private fun prepareAndShowNotificationsGroup(
    context: Context,
    attInfo: AttachmentInfo,
    isProgressEnabled: Boolean
  ) {
    var groupResourceId = if (isProgressEnabled)
      android.R.drawable.stat_sys_download
    else
      android.R.drawable.stat_sys_download_done

    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    for (stBarNotification in manager.activeNotifications) {
      if (!TextUtils.isEmpty(stBarNotification.tag) && stBarNotification.tag != attInfo.id
        && stBarNotification.id != attInfo.uid.toInt()
      ) {
        val notification = stBarNotification.notification
        if (GROUP_NAME_ATTACHMENTS == notification.group) {
          val extras = notification.extras
          if (extras != null && extras.getInt(Notification.EXTRA_PROGRESS_MAX) == MAX_FILE_SIZE_IN_PERCENTAGE
            && extras.getInt(Notification.EXTRA_PROGRESS) > 0
          ) {
            groupResourceId = android.R.drawable.stat_sys_download
            break
          }
        }
      }
    }

    val groupBuilder = NotificationCompat.Builder(
      context,
      NotificationChannelManager.CHANNEL_ID_ATTACHMENTS
    )
      .setSmallIcon(groupResourceId)
      .setGroup(GROUP_NAME_ATTACHMENTS)
      .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_CHILDREN)
      .setDefaults(Notification.DEFAULT_ALL)
      .setAutoCancel(true)
      .setGroupSummary(true)
    notify(groupName, groupId, groupBuilder.build())
  }

  companion object {
    const val GROUP_NAME_ATTACHMENTS = BuildConfig.APPLICATION_ID + ".ATTACHMENTS"
    const val NOTIFICATIONS_GROUP_ATTACHMENTS = -2
    private const val MAX_CONTENT_TITLE_LENGTH = 30
    private const val MAX_FILE_SIZE_IN_PERCENTAGE = 100
  }
}
