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
    val existingLabels = getLabelsSuspend(accountEntity.email, accountEntity.accountType)

    val deleteCandidates = mutableListOf<LabelEntity>()
    for (existingLabel in existingLabels) {
      var isFolderFound = false
      for (freshLabel in freshLabels) {
        if (freshLabel.name == existingLabel.name) {
          isFolderFound = true
          break
        }
      }

      if (!isFolderFound) {
        deleteCandidates.add(existingLabel)
      }
    }

    val newCandidates = mutableListOf<LabelEntity>()
    val updateCandidates = mutableListOf<LabelEntity>()

    for (freshLabel in freshLabels) {
      var isFolderFound = false
      for (existingLabel in existingLabels) {
        if (existingLabel.name == freshLabel.name) {
          isFolderFound = true
          if (existingLabel.alias != freshLabel.alias
            || existingLabel.labelColor != freshLabel.labelColor
            || existingLabel.textColor != freshLabel.textColor
            || existingLabel.labelListVisibility != freshLabel.labelListVisibility
          ) {
            updateCandidates.add(
              existingLabel.copy(
                alias = freshLabel.alias,
                labelColor = freshLabel.labelColor,
                textColor = freshLabel.textColor,
                attributes = freshLabel.attributes,
                labelListVisibility = freshLabel.labelListVisibility,
              )
            )
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
