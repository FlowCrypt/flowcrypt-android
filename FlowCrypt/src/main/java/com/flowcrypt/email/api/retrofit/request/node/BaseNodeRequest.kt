/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.request.node

import android.content.Context
import android.net.Uri
import com.flowcrypt.email.api.retrofit.node.NodeService
import retrofit2.Response

/**
 * It's a base request for the Node backend.
 *
 * @author Denis Bondarenko
 * Date: 1/10/19
 * Time: 5:43 PM
 * E-mail: DenBond7@gmail.com
 */
open class BaseNodeRequest constructor(
  final override var context: Context?,
  override val uri: Uri?,
  override val hasEncryptedDataInUri: Boolean = false
) : NodeRequest {

  init {
    this.context = context?.applicationContext
  }

  override val endpoint: String
    get() = throw UnsupportedOperationException("not defined")

  override val data: ByteArray
    get() = ByteArray(0)

  constructor() : this(null, null)

  override fun getResponse(nodeService: NodeService): Response<*> {
    throw UnsupportedOperationException("not defined")
  }
}
