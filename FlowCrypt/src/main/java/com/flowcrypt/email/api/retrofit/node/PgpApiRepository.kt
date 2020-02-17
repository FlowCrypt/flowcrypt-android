/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.node

import android.content.Context
import androidx.lifecycle.MutableLiveData
import com.flowcrypt.email.api.retrofit.base.BaseApiRepository
import com.flowcrypt.email.api.retrofit.request.node.ParseDecryptMsgRequest
import com.flowcrypt.email.api.retrofit.request.node.ZxcvbnStrengthBarRequest
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.api.retrofit.response.model.node.NodeKeyDetails
import com.flowcrypt.email.api.retrofit.response.node.DecryptKeyResult
import com.flowcrypt.email.api.retrofit.response.node.NodeResponseWrapper
import com.flowcrypt.email.api.retrofit.response.node.ParseDecryptedMsgResult

/**
 * It's an entry point of all requests to work with PGP actions.
 *
 * @author Denis Bondarenko
 * Date: 2/15/19
 * Time: 10:25 AM
 * E-mail: DenBond7@gmail.com
 */
interface PgpApiRepository : BaseApiRepository {
  /**
   * Parse the given raw string and fetch a list of [NodeKeyDetails].
   *
   * @param requestCode The unique request code for identify the current action.
   * @param liveData    An instance of [MutableLiveData] which will be used for the result delivering.
   * @param raw         The raw string which can take one key or many keys,
   * it can be private or public keys, it can be armored or binary.. doesn't matter.
   */
  fun fetchKeyDetails(requestCode: Int = 0, liveData: MutableLiveData<NodeResponseWrapper<*>>, raw:
  String?)

  /**
   * Parse the given raw MIME message and decrypt some parts if needed.
   *
   * @param requestCode The unique request code for identify the current action.
   * @param request     An instance of [ParseDecryptMsgRequest] which contains information about a message.
   */
  suspend fun parseDecryptMsg(requestCode: Int = 0, request: ParseDecryptMsgRequest):
      Result<ParseDecryptedMsgResult?>

  /**
   * Check the passphrase strength
   *
   * @param requestCode The unique request code for identify the current action.
   * @param liveData    An instance of [MutableLiveData] which will be used for the result delivering.
   * @param request     An instance of [ZxcvbnStrengthBarRequest].
   */
  fun checkPassphraseStrength(requestCode: Int = 0, liveData:
  MutableLiveData<NodeResponseWrapper<*>>,
                              request: ZxcvbnStrengthBarRequest)

  suspend fun decryptKey(context: Context, armoredKey: String, passphrases: List<String>):
      Result<DecryptKeyResult>
}
