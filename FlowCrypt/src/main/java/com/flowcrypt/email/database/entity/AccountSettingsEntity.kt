/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.database.entity

import android.os.Parcelable
import android.provider.BaseColumns
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.flowcrypt.email.database.entity.AccountSettingsEntity.Companion.TABLE_NAME
import kotlinx.parcelize.Parcelize
import java.util.concurrent.TimeUnit

/**
 * @author Denys Bondarenko
 */
@Entity(
  tableName = TABLE_NAME,
  indices = [
    Index(
      name = "account_account_type_in_account_settings",
      value = ["account", "account_type"],
      unique = true
    )
  ],
  foreignKeys = [
    ForeignKey(
      entity = AccountEntity::class,
      parentColumns = ["email", "account_type"],
      childColumns = ["account", "account_type"],
      onDelete = ForeignKey.CASCADE
    )
  ]
)
@Parcelize
data class AccountSettingsEntity constructor(
  @PrimaryKey(autoGenerate = true)
  @ColumnInfo(name = BaseColumns._ID) val id: Long? = null,
  val account: String,
  @ColumnInfo(name = "account_type", defaultValue = "NULL") val accountType: String? = null,
  @ColumnInfo(
    name = "check_pass_phrase_attempts_count",
    defaultValue = "0"
  ) val checkPassPhraseAttemptsCount: Int = 0,
  @ColumnInfo(
    name = "last_unsuccessful_check_pass_phrase_attempt_time",
    defaultValue = "0"
  ) val lastUnsuccessfulCheckPassPhraseAttemptTime: Long = 0,
) : Parcelable {
  companion object {
    const val TABLE_NAME = "account_settings"
    const val ANTI_BRUTE_FORCE_PROTECTION_ATTEMPTS_MAX_VALUE = 5
    val BLOCKING_TIME_IN_MILLISECONDS = TimeUnit.MINUTES.toMillis(5)
    val RESET_COUNT_TIME_IN_MILLISECONDS = TimeUnit.MINUTES.toMillis(5)
  }
}
