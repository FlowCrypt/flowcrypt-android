/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.database.dao

import androidx.room.Dao
import androidx.room.Query
import com.flowcrypt.email.database.entity.AccountSettingsEntity

/**
 * @author Denys Bondarenko
 */
@Dao
interface AccountSettingsDao : BaseDao<AccountSettingsEntity> {
  @Query("SELECT * FROM account_settings WHERE account = :account AND account_type = :accountType")
  suspend fun getAccountSettings(account: String, accountType: String?): AccountSettingsEntity?
}
