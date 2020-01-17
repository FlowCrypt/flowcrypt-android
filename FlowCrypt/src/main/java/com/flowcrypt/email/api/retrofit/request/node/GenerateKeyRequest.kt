/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.request.node

import com.flowcrypt.email.api.retrofit.node.NodeService
import com.flowcrypt.email.model.PgpContact
import com.google.gson.annotations.Expose
import retrofit2.Response
import java.util.*

/**
 * Using this class we can create a request to create a new key with the given parameters.
 *
 * @author Denis Bondarenko
 * Date: 4/1/19
 * Time: 9:44 AM
 * E-mail: DenBond7@gmail.com
 */
class GenerateKeyRequest(@Expose val passphrase: String,
                         pgpContacts: List<PgpContact>) : BaseNodeRequest() {

  @Expose
  private val variant: String

  @Expose
  private val userIds: MutableList<UserId>

  override val endpoint: String = "generateKey"

  init {
    this.variant = KEY_VARIANT_CURVE25519 // default, not yet configurable
    this.userIds = ArrayList()
    for ((email, name) in pgpContacts) {
      userIds.add(UserId(email, name ?: email)) // todo - fix https://github.com/FlowCrypt/flowcrypt-android/issues/591
    }
  }

  override fun getResponse(nodeService: NodeService): Response<*> {
    return nodeService.generateKey(this).execute()
  }

  private class UserId internal constructor(@Expose val email: String,
                                            @Expose val name: String)

  companion object {
    private const val KEY_VARIANT_CURVE25519 = "curve25519"
    private const val KEY_VARIANT_RSA2048 = "rsa2048"
    private const val KEY_VARIANT_RSA4096 = "rsa4096"
  }
}
