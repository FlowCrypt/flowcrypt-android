/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.request.node

import android.content.Context
import android.net.Uri
import com.flowcrypt.email.api.retrofit.node.NodeService
import com.flowcrypt.email.api.retrofit.request.model.node.PrivateKeyInfo
import com.flowcrypt.email.model.PgpKeyInfo
import com.google.gson.annotations.Expose
import retrofit2.Response

/**
 * This class will be used for the message decryption.
 *
 * @author Denis Bondarenko
 * Date: 1/11/19
 * Time: 03:29 PM
 * E-mail: DenBond7@gmail.com
 */
class ParseDecryptMsgRequest @JvmOverloads constructor(
    context: Context? = null,
    override val data: ByteArray = ByteArray(0),
    override val uri: Uri? = null,
    pgpKeyInfos: List<PgpKeyInfo>,
    @Expose val isEmail: Boolean = false) : BaseNodeRequest(context, uri) {

  @Expose
  private val keys: List<PrivateKeyInfo> = pgpKeyInfos.map { PrivateKeyInfo(it.private!!, it.longid, it.passphrase) }

  override val endpoint: String = "parseDecryptMsg"

  override fun getResponse(nodeService: NodeService): Response<*> {
    return nodeService.parseDecryptMsgOld(this).execute()
  }
}
