/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.request.node

import com.flowcrypt.email.api.retrofit.node.NodeService
import com.flowcrypt.email.model.PgpContact
import com.google.gson.annotations.Expose
import retrofit2.Response
import java.io.IOException
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
    this.variant = DEFAULT_KEY_VARIANT
    this.userIds = ArrayList()
    for ((email, name) in pgpContacts) {
      userIds.add(UserId(email, name))
    }
  }

  @Throws(IOException::class)
  override fun getResponse(nodeService: NodeService): Response<*> {
    return nodeService.generateKey(this).execute()
  }

  private class UserId internal constructor(@Expose val email: String,
                                            @Expose val name: String?)

  companion object {
    private const val DEFAULT_KEY_VARIANT = "rsa2048"
  }
}
