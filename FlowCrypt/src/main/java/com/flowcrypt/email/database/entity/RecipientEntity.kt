/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.database.entity

import android.os.Parcelable
import android.provider.BaseColumns
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import jakarta.mail.internet.InternetAddress
import kotlinx.parcelize.Parcelize

/**
 * @author Denis Bondarenko
 *         Date: 12/5/19
 *         Time: 4:22 PM
 *         E-mail: DenBond7@gmail.com
 */
@Entity(
  tableName = "recipients",
  indices = [
    Index(name = "name_in_recipients", value = ["name"]),
    Index(name = "last_use_in_recipients", value = ["last_use"]),
    Index(name = "email_in_recipients", value = ["email"], unique = true)
  ]
)
@Parcelize
data class RecipientEntity(
  @PrimaryKey(autoGenerate = true) @ColumnInfo(name = BaseColumns._ID) val id: Long? = null,
  val email: String,
  @ColumnInfo(defaultValue = "NULL") val name: String? = null,
  @ColumnInfo(name = "last_use", defaultValue = "0") val lastUse: Long = 0
) : Parcelable {
  fun toInternetAddress(): InternetAddress {
    return InternetAddress(email, name)
  }
}
