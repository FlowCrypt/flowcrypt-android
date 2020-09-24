/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.node

import android.content.Context
import androidx.lifecycle.MutableLiveData
import com.flowcrypt.email.api.retrofit.base.BaseApiRepository
import com.flowcrypt.email.api.retrofit.request.node.ParseDecryptMsgRequest
import com.flowcrypt.email.api.retrofit.request.node.ParseKeysRequest
import com.flowcrypt.email.api.retrofit.request.node.ZxcvbnStrengthBarRequest
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.api.retrofit.response.model.node.NodeKeyDetails
import com.flowcrypt.email.api.retrofit.response.node.DecryptKeyResult
import com.flowcrypt.email.api.retrofit.response.node.GenerateKeyResult
import com.flowcrypt.email.api.retrofit.response.node.NodeResponseWrapper
import com.flowcrypt.email.api.retrofit.response.node.ParseDecryptedMsgResult
import com.flowcrypt.email.api.retrofit.response.node.ParseKeysResult
import com.flowcrypt.email.api.retrofit.response.node.ZxcvbnStrengthBarResult
import com.flowcrypt.email.model.PgpContact

/**
 * It's an entry point of all requests to work with PGP actions.
 *
 * @author Denis Bondarenko
 * Date: 2/15/19
 * Time: 10:25 AM
 * E-mail: DenBond7@gmail.com
 */
//todo-denbond7 need to review this class. Maybe some things can be migrated to use coroutines
interface PgpApiRepository : BaseApiRepository {
  /**
   * Parse the given raw string and fetch a list of [NodeKeyDetails].
   *
   * @param request     An instance of [ParseDecryptMsgRequest] which contains a raw string with one or many keys,
   *  it can be private or public keys, it can be armored or binary.. doesn't matter.
   */
  suspend fun fetchKeyDetails(request: ParseKeysRequest): Result<ParseKeysResult?>

  /**
   * Parse the given raw string and fetch a list of [NodeKeyDetails].
   *
   * @param requestCode A unique request code for identify the current action.
   * @param liveData    An instance of [MutableLiveData] which will be used for the result delivering.
   * @param raw         A raw string which can take one key or many keys,
   * it can be private or public keys, it can be armored or binary.. doesn't matter.
   */
  fun fetchKeyDetails(requestCode: Int = 0, liveData: MutableLiveData<NodeResponseWrapper<*>>, raw: String?)

  /**
   * Parse the given raw MIME message and decrypt some parts if needed.
   *
   * @param requestCode A unique request code for identify the current action.
   * @param request     An instance of [ParseDecryptMsgRequest] which contains information about a message.
   */
  suspend fun parseDecryptMsg(requestCode: Int = 0, request: ParseDecryptMsgRequest): Result<ParseDecryptedMsgResult?>

  /**
   * Check the passphrase strength
   *
   * @param requestCode A unique request code for identify the current action.
   * @param liveData    An instance of [MutableLiveData] which will be used for the result delivering.
   * @param request     An instance of [ZxcvbnStrengthBarRequest].
   */
  fun checkPassphraseStrength(requestCode: Int = 0, liveData: MutableLiveData<NodeResponseWrapper<*>>, request: ZxcvbnStrengthBarRequest)

  suspend fun decryptKey(context: Context, armoredKey: String, passphrases: List<String>): Result<DecryptKeyResult>

  /**
   * Generate a private key using the given parameters.
   *
   * @param passphrase  The given passphrase.
   * @param pgpContacts A list of contacts.
   * @return A result with an instance of [GenerateKeyResult]
   */
  suspend fun createPrivateKey(context: Context, passphrase: String, pgpContacts: List<PgpContact>): Result<GenerateKeyResult?>


  /**
   * Check the passphrase strong value.
   *
   * @param context Interface to global information about an application environment;
   * @param guesses  A value which was received via [Zxcvbn].
   * @return A result with an instance of [ZxcvbnStrengthBarResult]
   */
  suspend fun zxcvbnStrengthBar(context: Context, guesses: Double): Result<ZxcvbnStrengthBarResult?>
}
