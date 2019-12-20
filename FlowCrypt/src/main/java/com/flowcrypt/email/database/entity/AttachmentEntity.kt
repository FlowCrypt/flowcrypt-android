/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.database.entity

import android.provider.BaseColumns
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * @author Denis Bondarenko
 *         Date: 12/5/19
 *         Time: 6:09 PM
 *         E-mail: DenBond7@gmail.com
 */
@Entity(tableName = "attachment",
    indices = [
      Index(name = "email_uid_folder_path_in_attachment", value = ["email", "uid", "folder", "path"], unique = true),
      Index(name = "email_folder_uid_in_attachment", value = ["email", "folder", "uid"])
    ],
    foreignKeys = [
      ForeignKey(entity = MessageEntity::class, parentColumns = ["email", "folder", "uid"],
          childColumns = ["email", "folder", "uid"], onDelete = ForeignKey.CASCADE)
    ]
)
data class AttachmentEntity(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = BaseColumns._ID) val id: Long?,
    val email: String,
    val folder: String,
    val uid: Int,
    val name: String,
    @ColumnInfo(name = "encodedSize", defaultValue = "0") val encodedSize: Int?,
    val type: String,
    @ColumnInfo(name = "attachment_id") val attachmentId: String?,
    @ColumnInfo(name = "file_uri") val fileUri: String?,
    @ColumnInfo(name = "forwarded_folder") val forwardedFolder: String?,
    @ColumnInfo(name = "forwarded_uid", defaultValue = "-1") val forwardedUid: Int?,
    val path: String
)
