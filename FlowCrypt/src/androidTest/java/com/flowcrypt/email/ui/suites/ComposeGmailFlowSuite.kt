/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.ui.suites

import com.flowcrypt.email.junit.annotations.NotReadyForCI
import com.flowcrypt.email.ui.gmailapi.EncryptedComposeGmailApiFlow
import com.flowcrypt.email.ui.gmailapi.EncryptedWithAttachmentsComposeGmailApiFlow
import com.flowcrypt.email.ui.gmailapi.EncryptedWithAttachmentsAndOwnPublicKeyComposeGmailApiFlow
import com.flowcrypt.email.ui.gmailapi.StandardComposeGmailApiFlow
import com.flowcrypt.email.ui.gmailapi.StandardWithAttachmentsComposeGmailApiFlow
import org.junit.runner.RunWith
import org.junit.runners.Suite

/**
 * @author Denys Bondarenko
 */
@NotReadyForCI(message = "this class just for local executing")
@RunWith(Suite::class)
@Suite.SuiteClasses(
  StandardComposeGmailApiFlow::class,
  EncryptedComposeGmailApiFlow::class,
  StandardWithAttachmentsComposeGmailApiFlow::class,
  EncryptedWithAttachmentsComposeGmailApiFlow::class,
  EncryptedWithAttachmentsAndOwnPublicKeyComposeGmailApiFlow::class,
)
class ComposeGmailFlowSuite