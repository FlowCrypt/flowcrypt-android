/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.extensions.com.google.api.services.gmail.model

import android.accounts.Account
import com.flowcrypt.email.database.entity.AccountAliasesEntity
import com.flowcrypt.email.database.entity.AccountEntity
import com.google.api.services.gmail.model.SendAs

/**
 * @author Denys Bondarenko
 */
fun SendAs.toAccountAliasesEntity(account: Account): AccountAliasesEntity {
  return AccountAliasesEntity(
    email = account.name.lowercase(),
    accountType = account.type ?: AccountEntity.ACCOUNT_TYPE_GOOGLE,
    sendAsEmail = sendAsEmail?.lowercase(),
    displayName = displayName,
    replyToAddress = replyToAddress,
    signature = signature,
    isPrimary = isPrimary,
    isDefault = isDefault,
    treatAsAlias = treatAsAlias,
    verificationStatus = verificationStatus
  )
}