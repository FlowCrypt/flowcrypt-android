/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.util

import android.content.Context
import androidx.test.espresso.idling.CountingIdlingResource
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import java.util.UUID

/**
 * @author Denys Bondarenko
 */
object FlavorSettings : EnvironmentSettings {
  private var cachedGoogleIdTokenCredential: GoogleIdTokenCredential? = null
  private var cachedGoogleAuthorizationResult: AuthorizationResult? = null

  private val countingIdlingResource: CountingIdlingResource = CountingIdlingResource(
    GeneralUtil.genIdlingResourcesName(this::class.java),
    GeneralUtil.isDebugBuild()
  )

  override fun configure(context: Context) {}
  override fun getCountingIdlingResource() = countingIdlingResource
  override fun getGoogleIdToken(): String = UUID.randomUUID().toString()
  override fun getGoogleIdTokenCredential(): GoogleIdTokenCredential? = cachedGoogleIdTokenCredential
  fun setGoogleIdTokenCredential(credential: GoogleIdTokenCredential?) {
    cachedGoogleIdTokenCredential = credential
  }
  override fun getGoogleAuthorizationResult(): AuthorizationResult? = cachedGoogleAuthorizationResult
  fun setGoogleAuthorizationResult(result: AuthorizationResult?) {
    cachedGoogleAuthorizationResult = result
  }
  override fun getGmailAPIRootUrl() = "https://flowcrypt.test/"
  override fun isGMailAPIHttpRequestInitializerEnabled(): Boolean = false
}
