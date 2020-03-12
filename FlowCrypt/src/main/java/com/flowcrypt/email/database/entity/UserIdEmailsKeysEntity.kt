/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
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
 *         Date: 12/4/19
 *         Time: 5:02 PM
 *         E-mail: DenBond7@gmail.com
 */
@Entity(tableName = "user_id_emails_and_keys",
    indices = [Index(name = "long_id_user_id_email_in_user_id_emails_and_keys", value = ["long_id", "user_id_email"], unique = true)]
)
data class UserIdEmailsKeysEntity(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = BaseColumns._ID) val id: Long? = null,
    @ColumnInfo(name = "long_id") val longId: String,
    @ColumnInfo(name = "user_id_email") val userIdEmail: String)