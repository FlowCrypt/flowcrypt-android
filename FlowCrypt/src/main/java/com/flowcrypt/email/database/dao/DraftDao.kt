/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.database.dao

import androidx.room.Dao
import com.flowcrypt.email.database.entity.DraftEntity

@Dao
interface DraftDao : BaseDao<DraftEntity>
