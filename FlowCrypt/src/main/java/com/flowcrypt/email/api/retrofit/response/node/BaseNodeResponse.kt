/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.response.node

import com.flowcrypt.email.api.retrofit.response.base.ApiResponse
import java.io.BufferedInputStream

/**
 * It's a base response from the Node.js server.
 *
 * @author Denis Bondarenko
 * Date: 1/11/19
 * Time: 11:42 AM
 * E-mail: DenBond7@gmail.com
 */
interface BaseNodeResponse : ApiResponse {
  fun handleRawData(bufferedInputStream: BufferedInputStream)
}
