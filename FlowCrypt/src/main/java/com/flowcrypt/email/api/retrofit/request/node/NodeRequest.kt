/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.request.node

import android.content.Context
import android.net.Uri
import com.flowcrypt.email.api.retrofit.node.NodeService
import retrofit2.Response
import java.io.IOException

/**
 * This interface describes common things for all requests to Node.js
 *
 * @author Denis Bondarenko
 * Date: 1/17/19
 * Time: 5:40 PM
 * E-mail: DenBond7@gmail.com
 */
interface NodeRequest {
  val endpoint: String

  val data: ByteArray

  val uri: Uri?

  val context: Context?

  @Throws(IOException::class)
  fun getResponse(nodeService: NodeService): Response<*>
}
