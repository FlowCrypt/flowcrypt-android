/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.api.retrofit.request.model

import com.flowcrypt.email.BuildConfig
import com.google.gson.annotations.Expose

/**
 * @author Denys Bondarenko
 */
data class CrashReportModel(
  @Expose val name: String? = null,
  @Expose val message: String? = null,
  @Expose val url: String? = null,
  @Expose val line: Int? = null,
  @Expose val col: Int? = null,
  @Expose val trace: String? = null,
) : RequestModel {
  @Expose
  val version: String = BuildConfig.VERSION_CODE.toString()

  @Expose
  val environment: String = BuildConfig.BUILD_TYPE

  @Expose
  val product: String = "android"

  @Expose
  val buildType: String = BuildConfig.FLAVOR
}
