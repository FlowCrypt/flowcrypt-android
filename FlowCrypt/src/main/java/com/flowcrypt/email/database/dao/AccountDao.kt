/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.database.dao

import androidx.room.Dao
import androidx.room.Query
import com.flowcrypt.email.database.entity.AccountEntity

/**
 * This class describes available methods for [AccountEntity]
 *
 * @author Denis Bondarenko
 *         Date: 12/15/19
 *         Time: 4:23 PM
 *         E-mail: DenBond7@gmail.com
 */
@Dao
interface AccountDao {

  @Query("SELECT * FROM accounts WHERE is_active = 1")
  suspend fun getActiveAccount(): AccountEntity?
}