/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.rules

import com.flowcrypt.email.TestConstants
import com.flowcrypt.email.api.retrofit.node.gson.NodeGson
import com.flowcrypt.email.api.retrofit.response.model.node.NodeKeyDetails
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.model.KeyDetails
import com.flowcrypt.email.util.AccountDaoManager
import com.flowcrypt.email.util.PrivateKeysManager
import com.flowcrypt.email.util.TestGeneralUtil
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * @author Denis Bondarenko
 * Date: 21.02.2018
 * Time: 17:54
 * E-mail: DenBond7@gmail.com
 */
class AddPrivateKeyToDatabaseRule(val accountEntity: AccountEntity,
                                  val keyPath: String,
                                  val passphrase: String,
                                  val type: KeyDetails.Type) : BaseRule() {

  lateinit var nodeKeyDetails: NodeKeyDetails
    private set

  constructor() : this(AccountDaoManager.getDefaultAccountDao(), "node/default@denbond7.com_fisrtKey_prv_strong.json",
      TestConstants.DEFAULT_STRONG_PASSWORD, KeyDetails.Type.EMAIL)

  override fun apply(base: Statement, description: Description): Statement {
    return object : Statement() {
      override fun evaluate() {
        nodeKeyDetails = NodeGson.gson.fromJson(TestGeneralUtil.readFileFromAssetsAsString(
            context, keyPath), NodeKeyDetails::class.java)
        PrivateKeysManager.saveKeyToDatabase(accountEntity, nodeKeyDetails, passphrase, type)
        base.evaluate()
      }
    }
  }
}
