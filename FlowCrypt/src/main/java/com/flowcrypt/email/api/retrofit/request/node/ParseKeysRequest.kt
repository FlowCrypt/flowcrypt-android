/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.request.node

import android.text.TextUtils
import com.flowcrypt.email.api.retrofit.node.NodeService
import retrofit2.Response
import java.io.IOException

/**
 * Using this class we can create a request to parse the given string to find keys. It can take one key or many keys,
 * it can be private or public keys, it can be armored or binary.. doesn't matter.
 *
 * @author Denis Bondarenko
 * Date: 2/11/19
 * Time: 11:59 AM
 * E-mail: DenBond7@gmail.com
 */
class ParseKeysRequest(val rawKey: String) : BaseNodeRequest() {

  override val endpoint: String = "parseKeys"

  override val data: ByteArray
    get() = if (TextUtils.isEmpty(rawKey)) byteArrayOf() else rawKey.toByteArray()

  @Throws(IOException::class)
  override fun getResponse(nodeService: NodeService): Response<*> {
    return nodeService.parseKeys(this).execute()
  }
}
