/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.api.email.gmail.model

import com.google.api.services.gmail.model.Message
import jakarta.mail.internet.InternetAddress

/**
 * @author Denys Bondarenko
 */
data class GmailThreadInfo(
  val id: String,
  val lastMessage: Message,
  val messagesCount: Int,
  val recipients: List<InternetAddress>,
  val subject: String,
  val labels: Set<String>,
  val hasAttachments: Boolean = false,
  val hasPgpThings: Boolean = false,
  val hasUnreadMessages: Boolean = false,
)