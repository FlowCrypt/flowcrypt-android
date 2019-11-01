/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email

/**
 * The settings for the enterprise builds
 *
 * @author Denis Bondarenko
 *         Date: 11/01/19
 *         Time: 09:52 AM
 *         E-mail: DenBond7@gmail.com
 */
object FlavourSettingsImpl : FlavourSettings {
  private const val SERVER_CLIENT_ID = "374364070962-n83b6asllhfkhij6slijr61576lqqi3v.apps.googleusercontent.com"
  override val isIdTokenNeeded: Boolean = true
  override val serverClientId: String
    get() = SERVER_CLIENT_ID
  override val buildType: FlavourSettings.BuildType = FlavourSettings.BuildType.ENTERPRISE
}