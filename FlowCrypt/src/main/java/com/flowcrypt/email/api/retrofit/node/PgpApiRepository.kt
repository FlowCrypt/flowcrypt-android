/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.node

import com.flowcrypt.email.api.retrofit.base.BaseApiRepository
import com.flowcrypt.email.api.retrofit.request.node.ParseDecryptMsgRequest
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.api.retrofit.response.node.ParseDecryptedMsgResult

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
   * Parse the given raw MIME message and decrypt some parts if needed.
   *
   * @param requestCode A unique request code for identify the current action.
   * @param request     An instance of [ParseDecryptMsgRequest] which contains information about a message.
   */
  suspend fun parseDecryptMsg(
    requestCode: Int = 0,
    request: ParseDecryptMsgRequest
  ): Result<ParseDecryptedMsgResult?>
}
