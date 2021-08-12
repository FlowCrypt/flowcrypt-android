/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.util

import android.content.Context
import com.flowcrypt.email.api.wkd.WkdClient
import com.flowcrypt.email.extensions.org.bouncycastle.openpgp.toPgpKeyDetails
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody.Companion.toResponseBody
import org.pgpainless.algorithm.EncryptionPurpose
import org.pgpainless.key.info.KeyRingInfo
import retrofit2.Response
import java.net.HttpURLConnection

/**
 * @author Denis Bondarenko
 *         Date: 8/11/21
 *         Time: 2:48 PM
 *         E-mail: DenBond7@gmail.com
 */
object PubLookup {
  /**
   * Fetch pub keys using the user email.
   *
   * @return pub key. For now, we just peak at the first matching key. It should be improved in
   * the future. See more details here https://github.com/FlowCrypt/flowcrypt-android/issues/480
   */
  suspend fun lookupEmail(context: Context, email: String): Response<String> =
    withContext(Dispatchers.IO) {
      val pgpPublicKeyRingCollection = WkdClient.lookupEmail(context, email)

      val firstMatchingKey = pgpPublicKeyRingCollection?.firstOrNull {
        KeyRingInfo(it)
          .getEncryptionSubkeys(EncryptionPurpose.STORAGE_AND_COMMUNICATIONS)
          .isNotEmpty()
      }
      return@withContext firstMatchingKey?.toPgpKeyDetails()?.publicKey?.let { armoredPubKey ->
        Response.success(armoredPubKey)
      } ?: Response.error(HttpURLConnection.HTTP_NOT_FOUND, "Not found".toResponseBody())
    }
}
