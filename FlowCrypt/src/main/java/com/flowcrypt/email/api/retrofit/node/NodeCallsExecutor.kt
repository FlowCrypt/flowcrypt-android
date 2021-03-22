/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.node

import com.flowcrypt.email.api.retrofit.request.node.DecryptKeyRequest
import com.flowcrypt.email.api.retrofit.request.node.EncryptKeyRequest
import com.flowcrypt.email.api.retrofit.request.node.ParseKeysRequest
import com.flowcrypt.email.api.retrofit.response.model.node.NodeKeyDetails
import com.flowcrypt.email.api.retrofit.response.node.BaseNodeResponse
import com.flowcrypt.email.api.retrofit.response.node.DecryptKeyResult
import com.flowcrypt.email.api.retrofit.response.node.EncryptKeyResult
import com.flowcrypt.email.util.exception.NodeException
import com.google.android.gms.common.util.CollectionUtils
import java.io.IOException

/**
 * It's an utility class which contains only static methods. Don't call this methods in the UI thread.
 *
 * @author Denis Bondarenko
 * Date: 2/11/19
 * Time: 2:54 PM
 * E-mail: DenBond7@gmail.com
 */
class NodeCallsExecutor {
  companion object {
    /**
     * Parse a list of [NodeKeyDetails] from the given string. It can take one key or many keys, it can be
     * private or
     * public keys, it can be armored or binary... doesn't matter.
     *
     * @param key The given key.
     * @return A list of [NodeKeyDetails]
     * @throws IOException   Such exceptions can occur during network calls.
     * @throws NodeException If Node.js server will return any errors we will throw such type of errors.
     */
    fun parseKeys(key: String?): List<NodeKeyDetails> {
      key ?: return emptyList()

      val service = NodeRetrofitHelper.getRetrofit()!!.create(NodeService::class.java)
      val request = ParseKeysRequest(key)

      val response = service.parseKeys(request).execute()
      val result = response.body()

      checkResult(result)

      val details = result!!.nodeKeyDetails

      return if (CollectionUtils.isEmpty(details)) emptyList() else details
    }

    /**
     * Decrypt the given private key using an input passphrase.
     *
     * @param key        The given private key.
     * @param passphrase The given passphrase candidate.
     * @return An instance of [DecryptKeyResult]
     * @throws IOException   Such exceptions can occur during network calls.
     * @throws NodeException If Node.js server will return any errors we will throw such type of errors.
     */
    fun decryptKey(key: String, passphrase: String): DecryptKeyResult {
      return decryptKey(key, listOf(passphrase))
    }

    /**
     * Decrypt the given private key using input passphrases.
     *
     * @param key         The given private key.
     * @param passphrases A list of passphrase candidates.
     * @return An instance of [DecryptKeyResult]
     * @throws IOException   Such exceptions can occur during network calls.
     * @throws NodeException If Node.js server will return any errors we will throw such type of errors.
     */
    fun decryptKey(key: String, passphrases: List<String>): DecryptKeyResult {
      val service = NodeRetrofitHelper.getRetrofit()!!.create(NodeService::class.java)
      val request = DecryptKeyRequest(key, passphrases)

      val response = service.decryptKey(request).execute()
      val result = response.body()

      checkResult(result)

      return result!!
    }

    /**
     * Encrypt a private key using the given passphrase.
     *
     * @param key        A private key.
     * @param passphrase The given passphrase.
     * @return An instance of [EncryptKeyResult]
     * @throws IOException   Such exceptions can occur during network calls.
     * @throws NodeException If Node.js server will return any errors we will throw such type of errors.
     */
    fun encryptKey(key: String, passphrase: String): EncryptKeyResult {
      val service = NodeRetrofitHelper.getRetrofit()!!.create(NodeService::class.java)
      val request = EncryptKeyRequest(key, passphrase)

      val response = service.encryptKey(request).execute()
      val result = response.body()

      checkResult(result)

      return result!!
    }

    private fun checkResult(result: BaseNodeResponse?) {
      if (result == null) {
        throw NullPointerException("Result is null")
      }

      if (result.apiError != null) {
        throw NodeException(result.apiError)
      }
    }
  }
}
