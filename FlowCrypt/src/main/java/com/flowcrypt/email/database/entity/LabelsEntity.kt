/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.database.entity

import android.provider.BaseColumns
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * @author Denis Bondarenko
 *         Date: 12/5/19
 *         Time: 5:57 PM
 *         E-mail: DenBond7@gmail.com
 */
@Entity(tableName = "imap_labels")
data class LabelsEntity(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = BaseColumns._ID) val id: Long,
    val email: String,
    @ColumnInfo(name = "folder_name") val folderName: String,
    @ColumnInfo(name = "is_custom_label", defaultValue = "0") val isCustomLabel: Boolean,
    @ColumnInfo(name = "folder_alias", defaultValue = "NULL") val folderAlias: String?,
    @ColumnInfo(name = "message_count", defaultValue = "0") val messageCount: Int,
    @ColumnInfo(name = "folder_attributes") val folderAttributes: String,
    @ColumnInfo(name = "folder_message_count", defaultValue = "0") val folderMessageCount: Int)