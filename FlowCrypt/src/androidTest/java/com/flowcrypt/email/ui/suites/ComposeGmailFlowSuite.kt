/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.ui.suites

import com.flowcrypt.email.junit.annotations.NotReadyForCI
import com.flowcrypt.email.ui.gmailapi.EncryptedComposeGmailApiFlow
import com.flowcrypt.email.ui.gmailapi.EncryptedReplyAllComposeGmailApiFlow
import com.flowcrypt.email.ui.gmailapi.EncryptedWithAttachmentsComposeGmailApiFlow
import com.flowcrypt.email.ui.gmailapi.EncryptedWithAttachmentsAndOwnPublicKeyComposeGmailApiFlow
import com.flowcrypt.email.ui.gmailapi.PasswordProtectedEncryptedComposeGmailApiFlow
import com.flowcrypt.email.ui.gmailapi.StandardComposeGmailApiFlow
import com.flowcrypt.email.ui.gmailapi.StandardReplyAllComposeGmailApiFlow
import com.flowcrypt.email.ui.gmailapi.StandardWithAttachmentsComposeGmailApiFlow
import com.flowcrypt.email.ui.gmailapi.StandardWithPublicKeyComposeGmailApiFlow
import org.junit.runner.RunWith
import org.junit.runners.Suite

/**
 * @author Denys Bondarenko
 */
@NotReadyForCI(message = "this class just for local executing")
@RunWith(Suite::class)
@Suite.SuiteClasses(
  StandardComposeGmailApiFlow::class,
  StandardWithAttachmentsComposeGmailApiFlow::class,
  StandardWithPublicKeyComposeGmailApiFlow::class,
  StandardReplyAllComposeGmailApiFlow::class,
  EncryptedComposeGmailApiFlow::class,
  EncryptedWithAttachmentsComposeGmailApiFlow::class,
  EncryptedWithAttachmentsAndOwnPublicKeyComposeGmailApiFlow::class,
  EncryptedReplyAllComposeGmailApiFlow::class,
  PasswordProtectedEncryptedComposeGmailApiFlow::class,
)
class ComposeGmailFlowSuite