/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.database.dao.source

import androidx.room.Dao
import com.flowcrypt.email.database.dao.BaseDao
import com.flowcrypt.email.database.entity.AccountAliasesEntity

/**
 * This object describes a logic of work with [AccountAliases].
 *
 * @author Denis Bondarenko
 * Date: 26.10.2017
 * Time: 15:51
 * E-mail: DenBond7@gmail.com
 */
@Dao
interface AccountAliasesDao : BaseDao<AccountAliasesEntity>
