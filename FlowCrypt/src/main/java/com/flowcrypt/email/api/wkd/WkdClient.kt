/*
 * © 2021-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors:
 *   Ivan Pizhenko
 */

package com.flowcrypt.email.api.wkd

import com.flowcrypt.email.extensions.kotlin.isValidEmail
import com.flowcrypt.email.util.BetterInternetAddress
import okhttp3.OkHttpClient
import okhttp3.Request
import org.apache.commons.codec.binary.ZBase32
import org.apache.commons.codec.digest.DigestUtils
import org.bouncycastle.openpgp.PGPPublicKeyRingCollection
import org.bouncycastle.openpgp.jcajce.JcaPGPPublicKeyRingCollection
import java.net.URLEncoder
import java.util.Locale
import java.util.concurrent.TimeUnit

object WkdClient {
  private const val DEFAULT_REQUEST_TIMEOUT = 4

  fun lookupEmail(
    email: String,
    timeout: Int = DEFAULT_REQUEST_TIMEOUT,
    wkdPort: Int? = null
  ): PGPPublicKeyRingCollection? {
    val keys = rawLookupEmail(email, timeout, wkdPort) ?: return null
    val lowerCaseEmail = email.toLowerCase(Locale.ROOT)
    val matchingKeys = keys.keyRings.asSequence().filter {
      for (userId in it.publicKey.userIDs) {
        try {
          val parsed = BetterInternetAddress(userId)
          if (parsed.emailAddress.toLowerCase(Locale.ROOT) == lowerCaseEmail) return@filter true
        } catch (ex: Exception) {
          // ignore
        }
      }
      false
    }.toList()
    return if (matchingKeys.isNotEmpty()) PGPPublicKeyRingCollection(matchingKeys) else null
  }

  @Suppress("private")
  fun rawLookupEmail(
    email: String,
    timeout: Int = DEFAULT_REQUEST_TIMEOUT,
    wkdPort: Int? = null
  ): PGPPublicKeyRingCollection? {
    if (!email.isValidEmail()) throw IllegalArgumentException("Invalid email address")
    val parts = email.split('@')
    val user = parts[0].toLowerCase(Locale.ROOT)
    val hu = ZBase32().encodeAsString(DigestUtils.sha1(user.toByteArray()))
    val directDomain = parts[1].toLowerCase(Locale.ROOT)
    val advancedDomainPrefix = if (directDomain == "localhost") "" else "openpgpkey."
    val directHost = if (wkdPort == null) directDomain else "${directDomain}:${wkdPort}"
    val advancedHost = "$advancedDomainPrefix$directHost"
    val advancedUrl = "https://${advancedHost}/.well-known/openpgpkey/${directDomain}"
    val directUrl = "https://${directHost}/.well-known/openpgpkey"
    val userPart = "hu/$hu?l=${URLEncoder.encode(user, "UTF-8")}"
    try {
      val result = urlLookup(advancedUrl, userPart, timeout)
      // Do not retry "direct" if "advanced" had a policy file
      if (result.hasPolicy) return result.keys
    } catch (ex: Exception) {
      // ignore
    }
    return urlLookup(directUrl, userPart, timeout).keys
  }

  private data class UrlLookupResult(
    val hasPolicy: Boolean = false,
    val keys: PGPPublicKeyRingCollection? = null
  )

  private fun urlLookup(methodUrlBase: String, userPart: String, timeout: Int): UrlLookupResult {
    val httpClient = OkHttpClient.Builder().callTimeout(timeout.toLong(), TimeUnit.SECONDS).build()
    val policyRequest = Request.Builder().url("$methodUrlBase/policy").build()
    httpClient.newCall(policyRequest).execute().use { policyResponse ->
      if (policyResponse.code != 200) return UrlLookupResult()
    }
    val userRequest = Request.Builder().url("$methodUrlBase/$userPart").build()
    httpClient.newCall(userRequest).execute().use { userResponse ->
      if (userResponse.code != 200 || userResponse.body == null) return UrlLookupResult(true)
      val keys = JcaPGPPublicKeyRingCollection(userResponse.body!!.byteStream())
      return UrlLookupResult(true, keys)
    }
  }
}
