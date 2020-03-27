/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.database.dao.source.imap

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.flowcrypt.email.database.dao.BaseDao
import com.flowcrypt.email.database.entity.LabelEntity

/**
 * This class describes the structure of IMAP labels for different accounts and methods which
 * will be used to manipulate this data.
 *
 * @author DenBond7
 * Date: 14.06.2017
 * Time: 15:59
 * E-mail: DenBond7@gmail.com
 */
@Dao
interface ImapLabelsDaoSource : BaseDao<LabelEntity> {
  @Query("SELECT * FROM imap_labels WHERE email = :account AND folder_name = :label")
  suspend fun getLabelSuspend(account: String?, label: String): LabelEntity?

  @Query("SELECT * FROM imap_labels WHERE email = :account AND folder_name = :label")
  fun getLabel(account: String?, label: String): LabelEntity?

  @Query("SELECT * FROM imap_labels WHERE email = :account")
  fun getLabelsLD(account: String): LiveData<List<LabelEntity>>

  @Query("SELECT * FROM imap_labels WHERE email = :account")
  fun getLabels(account: String): List<LabelEntity>

  @Query("SELECT * FROM imap_labels WHERE email = :account")
  suspend fun getLabelsSuspend(account: String): List<LabelEntity>

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