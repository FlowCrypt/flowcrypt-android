/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.database.entity

import android.provider.BaseColumns
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * @author Denis Bondarenko
 *         Date: 12/5/19
 *         Time: 4:22 PM
 *         E-mail: DenBond7@gmail.com
 */
@Entity(tableName = "contacts",
    indices = [
      Index(name = "email_in_contacts", value = ["email"], unique = true),
      Index(name = "name_in_contacts", value = ["name"]),
      Index(name = "has_pgp_in_contacts", value = ["has_pgp"]),
      Index(name = "long_id_in_contacts", value = ["long_id"]),
      Index(name = "last_use_in_contacts", value = ["last_use"])
    ]
)
data class ContactEntity(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = BaseColumns._ID) val id: Long,
    val email: String,
    @ColumnInfo(defaultValue = "NULL") val name: String?,
    @ColumnInfo(name = "public_key", defaultValue = "NULL") val publicKey: String?,
    @ColumnInfo(name = "has_pgp") val hasPgp: Boolean,
    @ColumnInfo(defaultValue = "NULL") val client: String?,
    @ColumnInfo(defaultValue = "NULL") val attested: Boolean?,
    @ColumnInfo(defaultValue = "NULL") val fingerprint: String?,
    @ColumnInfo(name = "long_id", defaultValue = "NULL") val longId: String?,
    @ColumnInfo(defaultValue = "NULL") val keywords: String?,
    @ColumnInfo(name = "last_use", defaultValue = "0") val lastUse: Int
)