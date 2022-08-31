/*
 * © 2021-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors:
 *   Ivan Pizhenko
 */

package com.flowcrypt.email.api.wkd

import android.content.Context
import com.flowcrypt.email.api.retrofit.ApiHelper
import com.flowcrypt.email.api.retrofit.ApiService
import com.flowcrypt.email.extensions.kotlin.isValidEmail
import com.flowcrypt.email.util.BetterInternetAddress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import org.apache.commons.codec.binary.ZBase32
import org.apache.commons.codec.digest.DigestUtils
import org.bouncycastle.openpgp.PGPPublicKeyRingCollection
import org.pgpainless.PGPainless
import retrofit2.Response
import retrofit2.Retrofit
import java.io.InterruptedIOException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

object WkdClient {
  private const val DEFAULT_REQUEST_TIMEOUT = 4000L

  suspend fun lookupEmail(context: Context, email: String): PGPPublicKeyRingCollection? =
    withContext(Dispatchers.IO) {
      val pgpPublicKeyRingCollection = rawLookupEmail(context, email)
      val lowerCaseEmail = email.lowercase()
      val matchingKeys = pgpPublicKeyRingCollection?.keyRings?.asSequence()?.filter {
        for (userId in it.publicKey.userIDs) {
          try {
            val parsed = BetterInternetAddress(str = userId, verifySpecialCharacters = false)
            if (parsed.emailAddress.lowercase() == lowerCaseEmail) return@filter true
          } catch (ex: Exception) {
            ex.printStackTrace()
          }
        }
        return@filter false
      }?.toList()
      return@withContext if (matchingKeys?.isNotEmpty() == true) {
        PGPPublicKeyRingCollection(matchingKeys)
      } else null
    }

  private suspend fun rawLookupEmail(
    context: Context,
    email: String,
    wkdPort: Int? = null
  ): PGPPublicKeyRingCollection? = withContext(Dispatchers.IO) {
    if (!email.isValidEmail()) {
      throw IllegalArgumentException("Invalid email address")
    }

    val parts = email.split('@')
    val user = parts[0].lowercase()
    val directDomain = parts[1].lowercase()

    val advancedDomainPrefix = if (directDomain == "localhost") "" else "openpgpkey."
    val hu = ZBase32().encodeAsString(DigestUtils.sha1(user.toByteArray()))
    val directHost = if (wkdPort == null) directDomain else "${directDomain}:${wkdPort}"
    val advancedHost = "$advancedDomainPrefix$directHost"

    try {
      val result = urlLookup(
        context = context,
        advancedHost = advancedHost,
        directDomain = directDomain,
        hu = hu,
        user = user
      )
      // Do not retry "direct" if "advanced" had a policy file
      if (result.hasPolicy) return@withContext result.keys
    } catch (ex: Exception) {
      ex.printStackTrace()
    }

    return@withContext try {
      val result = urlLookup(
        context = context,
        directDomain = directDomain,
        hu = hu,
        user = user
      )
      result.keys
    } catch (ex: UnknownHostException) {
      null
    } catch (ex: InterruptedIOException) {
      if (ex.message == "timeout") null else throw ex
    }
  }

  private suspend fun urlLookup(
    context: Context,
    advancedHost: String? = null,
    directDomain: String,
    hu: String,
    user: String
  ): UrlLookupResult = withContext(Dispatchers.IO) {
    val apiService = prepareApiService(context, directDomain)
    val wkdResponse: Response<ResponseBody> = if (advancedHost != null) {
      val checkPolicyResponse = apiService.checkPolicyForWkdAdvanced(advancedHost, directDomain)
      if (!checkPolicyResponse.isSuccessful) return@withContext UrlLookupResult()
      apiService.getPubFromWkdAdvanced(advancedHost, directDomain, hu, user)
    } else {
      val checkPolicyResponse = apiService.checkPolicyForWkdDirect(directDomain)
      if (!checkPolicyResponse.isSuccessful) return@withContext UrlLookupResult()
      apiService.getPubFromWkdDirect(directDomain, hu, user)
    }

    val incomingBytes = wkdResponse.body()?.byteStream()

    if (!wkdResponse.isSuccessful || incomingBytes == null) {
      return@withContext UrlLookupResult(true)
    } else {
      val keys = PGPainless.readKeyRing().publicKeyRingCollection(incomingBytes)
      return@withContext UrlLookupResult(true, keys)
    }
  }

  private fun prepareApiService(context: Context, directDomain: String): ApiService {
    val okHttpClient = OkHttpClient.Builder()
      .connectTimeout(DEFAULT_REQUEST_TIMEOUT, TimeUnit.MILLISECONDS)
      .writeTimeout(DEFAULT_REQUEST_TIMEOUT, TimeUnit.MILLISECONDS)
      .readTimeout(DEFAULT_REQUEST_TIMEOUT, TimeUnit.MILLISECONDS)
      .apply {
        ApiHelper.configureOkHttpClientForDebuggingIfAllowed(context, this)
      }.build()

    val retrofit = Retrofit.Builder()
      .baseUrl("https://$directDomain")
      .client(okHttpClient)
      .build()

    return retrofit.create(ApiService::class.java)
  }

  private data class UrlLookupResult(
    val hasPolicy: Boolean = false,
    val keys: PGPPublicKeyRingCollection? = null
  )
}
