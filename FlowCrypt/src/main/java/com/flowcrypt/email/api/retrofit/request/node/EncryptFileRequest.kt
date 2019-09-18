/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.request.node

import android.content.Context
import android.net.Uri
import com.flowcrypt.email.api.retrofit.node.NodeService
import com.google.gson.annotations.Expose
import retrofit2.Response

/**
 * Using this class we can create a request to encrypt an input data using the given public keys.
 *
 * @author Denis Bondarenko
 * Date: 1/15/19
 * Time: 9:07 AM
 * E-mail: DenBond7@gmail.com
 */
class EncryptFileRequest : BaseNodeRequest {

  override val data: ByteArray

  @Expose
  private var name: String? = null

  @Expose
  private var pubKeys: List<String>? = null

  override val endpoint: String = "encryptFile"

  constructor(data: ByteArray, name: String, pubKeys: List<String>) {
    this.data = data
    this.name = name
    this.pubKeys = pubKeys
  }

  constructor(context: Context?, uri: Uri?, name: String, pubKeys: List<String>)
      : super(context, uri) {
    this.name = name
    this.pubKeys = pubKeys
    this.data = ByteArray(0)
  }

  override fun getResponse(nodeService: NodeService): Response<*> {
    return nodeService.encryptFile(this).execute()
  }
}
