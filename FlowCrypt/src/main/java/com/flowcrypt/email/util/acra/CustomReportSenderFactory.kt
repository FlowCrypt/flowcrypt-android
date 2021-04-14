/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.util.acra

import android.content.Context
import org.acra.config.CoreConfiguration
import org.acra.sender.ReportSender
import org.acra.sender.ReportSenderFactory

/**
 * It's a custom realization of [ReportSenderFactory]
 *
 * @author Denis Bondarenko
 *         Date: 7/25/19
 *         Time: 3:17 PM
 *         E-mail: DenBond7@gmail.com
 */
class CustomReportSenderFactory : ReportSenderFactory {
  override fun create(context: Context, config: CoreConfiguration): ReportSender {
    return CustomReportSender(config)
  }
}