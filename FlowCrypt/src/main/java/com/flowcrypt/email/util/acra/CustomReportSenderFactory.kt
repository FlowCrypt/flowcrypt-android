/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.util.acra

import android.content.Context
import com.google.auto.service.AutoService
import org.acra.config.CoreConfiguration
import org.acra.config.HttpSenderConfiguration
import org.acra.plugins.HasConfigPlugin
import org.acra.sender.ReportSender
import org.acra.sender.ReportSenderFactory

/**
 * It's a custom realization of [ReportSenderFactory]
 *
 * @author Denys Bondarenko
 */
@AutoService(ReportSenderFactory::class)
class CustomReportSenderFactory :
  HasConfigPlugin(HttpSenderConfiguration::class.java), ReportSenderFactory {
  override fun create(context: Context, config: CoreConfiguration): ReportSender {
    return CustomReportSender(config)
  }

  override fun enabled(config: CoreConfiguration): Boolean = true
}
