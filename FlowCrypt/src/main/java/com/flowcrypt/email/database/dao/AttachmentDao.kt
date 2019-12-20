/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.database.dao

import androidx.room.Dao
import androidx.room.Query
import com.flowcrypt.email.database.entity.AttachmentEntity

/**
 * This class describes available methods for [AttachmentEntity]
 *
 * @author Denis Bondarenko
 *         Date: 12/20/19
 *         Time: 10:12 AM
 *         E-mail: DenBond7@gmail.com
 */
@Dao
interface AttachmentDao : BaseDao<AttachmentEntity> {
  @Query("DELETE FROM attachment WHERE email = :email AND folder = :label")
  suspend fun delete(email: String?, label: String?): Int
}