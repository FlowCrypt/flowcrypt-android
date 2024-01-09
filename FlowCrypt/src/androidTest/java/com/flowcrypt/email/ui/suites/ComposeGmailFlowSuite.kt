/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.ui.suites

import com.flowcrypt.email.junit.annotations.NotReadyForCI
import com.flowcrypt.email.ui.gmailapi.EncryptedComposeGmailApiFlow
import com.flowcrypt.email.ui.gmailapi.StandardComposeGmailApiFlow
import com.flowcrypt.email.ui.gmailapi.StandardWithAttachmentComposeGmailApiFlow
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
  StandardWithAttachmentComposeGmailApiFlow::class,
)
class ComposeGmailFlowSuite