/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.extensions

import android.content.Context
import com.flowcrypt.email.api.email.model.OutgoingMessageInfo
import com.flowcrypt.email.database.FlowCryptRoomDatabase

fun OutgoingMessageInfo.replaceWithCachedRecipients(context: Context): OutgoingMessageInfo {
  val recipientDao = FlowCryptRoomDatabase.getDatabase(context).recipientDao()
  val updatedFrom = recipientDao.getRecipientByEmail(from?.address?.lowercase() ?: "")
    ?.toInternetAddress() ?: from
  return copy(
    from = updatedFrom,
    toRecipients = recipientDao.getRecipientsByEmails((toRecipients ?: emptyList())
      .map { it.address.lowercase() })
      .map { it.toInternetAddress() },
    ccRecipients = ccRecipients?.let { list ->
      recipientDao.getRecipientsByEmails(list.map { it.address.lowercase() })
        .map { it.toInternetAddress() }
    },
    bccRecipients = bccRecipients?.let { list ->
      recipientDao.getRecipientsByEmails(list.map { it.address.lowercase() })
        .map { it.toInternetAddress() }
    }
  )
}
