/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.request.node

import android.text.TextUtils
import com.flowcrypt.email.api.retrofit.node.NodeService
import com.flowcrypt.email.api.retrofit.request.model.node.PrivateKeyInfo
import com.flowcrypt.email.model.PgpKeyInfo
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import retrofit2.Response
import java.util.*

/**
 * This class will be used for the message decryption.
 *
 * @author Denis Bondarenko
 * Date: 1/11/19
 * Time: 03:29 PM
 * E-mail: DenBond7@gmail.com
 */
class ParseDecryptMsgRequest @JvmOverloads constructor(val msg: String,
                                                       prvKeys: List<PgpKeyInfo>,
                                                       @Expose val passphrases: List<String>,
                                                       @Expose val isEmail: Boolean = false) : BaseNodeRequest() {

  @SerializedName("keys")
  @Expose
  private val privateKeyInfoList: MutableList<PrivateKeyInfo>

  override val endpoint: String = "parseDecryptMsg"

  override val data: ByteArray
    get() = if (TextUtils.isEmpty(msg)) byteArrayOf() else msg.toByteArray()

  init {
    this.privateKeyInfoList = ArrayList()

    for ((longid, private) in prvKeys) {
      privateKeyInfoList.add(PrivateKeyInfo(private!!, longid))
    }
  }

  override fun getResponse(nodeService: NodeService): Response<*> {
    return nodeService.parseDecryptMsg(this).execute()
  }
}
