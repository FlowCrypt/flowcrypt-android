/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.database.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
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

  @Query("SELECT * FROM imap_labels WHERE email = :account")
  fun getLabelsLD(account: String): LiveData<List<LabelEntity>>

  @Query("SELECT * FROM imap_labels WHERE email = :account")
  fun getLabels(account: String): List<LabelEntity>

  @Transaction
  fun update(existedLabels: Collection<LabelEntity>, freshLabels: Collection<LabelEntity>) {
    val deleteCandidates = mutableListOf<LabelEntity>()
    for (existedLabel in existedLabels) {
      var isFolderFound = false
      for (freshLabel in freshLabels) {
        if (freshLabel.folderName == existedLabel.folderName) {
          isFolderFound = true
          break
        }
      }

      if (!isFolderFound) {
        deleteCandidates.add(existedLabel)
      }
    }

    val newCandidates = mutableListOf<LabelEntity>()
    for (freshLabel in freshLabels) {
      var isFolderFound = false
      for (existedLabel in existedLabels) {
        if (existedLabel.folderName == freshLabel.folderName) {
          isFolderFound = true
          break
        }
      }

      if (!isFolderFound) {
        newCandidates.add(freshLabel)
      }
    }

    delete(deleteCandidates)
    insert(newCandidates)
  }
}