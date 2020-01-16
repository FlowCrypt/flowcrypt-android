/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.response.node

import android.os.Parcelable
import com.flowcrypt.email.api.retrofit.response.model.node.Error
import java.io.BufferedInputStream

/**
 * It's a base response from the Node.js server.
 *
 * @author Denis Bondarenko
 * Date: 1/11/19
 * Time: 11:42 AM
 * E-mail: DenBond7@gmail.com
 */
interface BaseNodeResponse : Parcelable {
  val error: Error?

  fun handleRawData(bufferedInputStream: BufferedInputStream)
}
