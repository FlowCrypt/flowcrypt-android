/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.database.dao

import androidx.room.Dao
import androidx.room.Query
import com.flowcrypt.email.database.entity.LabelEntity

/**
 * This class describes available methods for [LabelEntity]
 *
 * @author Denis Bondarenko
 *         Date: 12/20/19
 *         Time: 4:54 PM
 *         E-mail: DenBond7@gmail.com
 */
@Dao
interface LabelDao : BaseDao<LabelEntity> {

  @Query("SELECT * FROM imap_labels WHERE email = :account AND folder_name = :label")
  suspend fun getLabelSuspend(account: String?, label: String): LabelEntity?

  @Query("SELECT * FROM imap_labels WHERE email = :account AND folder_name = :label")
  fun getLabel(account: String?, label: String): LabelEntity?
}