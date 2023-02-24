/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.database.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.database.entity.LabelEntity

/**
 * This class describes the structure of IMAP labels for different accounts and methods which
 * will be used to manipulate this data.
 *
 * @author Denys Bondarenko
 */
@Dao
interface LabelDao : BaseDao<LabelEntity> {
  @Query(
    "SELECT * FROM labels " +
        "WHERE email = :account AND account_type = :accountType AND name = :label"
  )
  suspend fun getLabelSuspend(account: String, accountType: String?, label: String): LabelEntity?

  @Query(
    "SELECT * FROM labels " +
        "WHERE email = :account AND account_type = :accountType AND name = :label"
  )
  fun getLabel(account: String, accountType: String?, label: String): LabelEntity?

  @Query("SELECT * FROM labels WHERE email = :account AND account_type = :accountType")
  fun getLabelsLD(account: String, accountType: String?): LiveData<List<LabelEntity>>

  @Query("SELECT * FROM labels WHERE email = :account AND account_type = :accountType")
  fun getLabels(account: String, accountType: String?): List<LabelEntity>

  @Query("SELECT * FROM labels WHERE email = :account AND account_type = :accountType")
  suspend fun getLabelsSuspend(account: String, accountType: String?): List<LabelEntity>

  @Query("DELETE FROM labels WHERE email = :account AND account_type = :accountType")
  suspend fun deleteByEmailSuspend(account: String, accountType: String?): Int

  @Transaction
  suspend fun update(accountEntity: AccountEntity, freshLabels: Collection<LabelEntity>) {
    val existedLabels = getLabelsSuspend(accountEntity.email, accountEntity.accountType)

    val deleteCandidates = mutableListOf<LabelEntity>()
    for (existedLabel in existedLabels) {
      var isFolderFound = false
      for (freshLabel in freshLabels) {
        if (freshLabel.name == existedLabel.name) {
          isFolderFound = true
          break
        }
      }

      if (!isFolderFound) {
        deleteCandidates.add(existedLabel)
      }
    }

    val newCandidates = mutableListOf<LabelEntity>()
    val updateCandidates = mutableListOf<LabelEntity>()

    for (freshLabel in freshLabels) {
      var isFolderFound = false
      for (existedLabel in existedLabels) {
        if (existedLabel.name == freshLabel.name) {
          isFolderFound = true
          if (existedLabel.alias != freshLabel.alias) {
            if (existedLabel.attributes == freshLabel.attributes) {
              updateCandidates.add(existedLabel.copy(alias = freshLabel.alias))
            } else {
              updateCandidates.add(
                existedLabel.copy(
                  alias = freshLabel.alias,
                  attributes = freshLabel.attributes
                )
              )
            }
          }
          break
        }
      }

      if (!isFolderFound) {
        newCandidates.add(freshLabel)
      }
    }

    deleteSuspend(deleteCandidates)
    updateSuspend(updateCandidates)
    insertWithReplaceSuspend(newCandidates)
  }
}
