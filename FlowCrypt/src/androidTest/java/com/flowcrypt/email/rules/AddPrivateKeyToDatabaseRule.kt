/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.rules

import com.flowcrypt.email.TestConstants
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.database.entity.KeyEntity
import com.flowcrypt.email.model.KeyImportDetails
import com.flowcrypt.email.security.model.PgpKeyDetails
import com.flowcrypt.email.security.pgp.PgpKey
import com.flowcrypt.email.util.AccountDaoManager
import com.flowcrypt.email.util.PrivateKeysManager
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * @author Denis Bondarenko
 * Date: 21.02.2018
 * Time: 17:54
 * E-mail: DenBond7@gmail.com
 */
class AddPrivateKeyToDatabaseRule(
  val accountEntity: AccountEntity,
  val keyPath: String,
  val passphrase: String,
  val sourceType: KeyImportDetails.SourceType,
  val passphraseType: KeyEntity.PassphraseType = KeyEntity.PassphraseType.DATABASE
) : BaseRule() {
  var pgpKeyDetails: PgpKeyDetails
    private set

  init {
    pgpKeyDetails = PgpKey.parseKeys(context.assets.open(keyPath)).pgpKeyDetailsList.first()
  }

  constructor(
    keyPath: String = "pgp/default@flowcrypt.test_fisrtKey_prv_strong.asc",
    passphraseType: KeyEntity.PassphraseType = KeyEntity.PassphraseType.DATABASE
  ) : this(
    accountEntity = AccountDaoManager.getDefaultAccountDao(),
    keyPath,
    passphrase = TestConstants.DEFAULT_STRONG_PASSWORD,
    sourceType = KeyImportDetails.SourceType.EMAIL,
    passphraseType = passphraseType
  )

  override fun apply(base: Statement, description: Description): Statement {
    return object : Statement() {
      override fun evaluate() {
        PrivateKeysManager.saveKeyToDatabase(
          accountEntity = accountEntity,
          pgpKeyDetails = pgpKeyDetails,
          passphrase = passphrase,
          sourceType = sourceType,
          passphraseType = passphraseType
        )
        base.evaluate()
      }
    }
  }
}
