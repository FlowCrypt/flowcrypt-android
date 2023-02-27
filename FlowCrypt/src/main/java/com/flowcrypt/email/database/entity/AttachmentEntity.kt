/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.database.entity

import android.net.Uri
import android.provider.BaseColumns
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey
import com.flowcrypt.email.api.email.model.AttachmentInfo

/**
 * @author Denys Bondarenko
 */
@Entity(
  tableName = "attachment",
  indices = [
    Index(
      name = "email_uid_folder_path_in_attachment",
      value = ["email", "uid", "folder", "path"],
      unique = true
    ),
    Index(name = "email_folder_uid_in_attachment", value = ["email", "folder", "uid"])
  ],
  foreignKeys = [
    ForeignKey(
      entity = MessageEntity::class, parentColumns = ["email", "folder", "uid"],
      childColumns = ["email", "folder", "uid"], onDelete = ForeignKey.CASCADE
    )
  ]
)
data class AttachmentEntity(
  @PrimaryKey(autoGenerate = true) @ColumnInfo(name = BaseColumns._ID) val id: Long?,
  val email: String,
  val folder: String,
  val uid: Long,
  val name: String,
  @ColumnInfo(name = "encodedSize", defaultValue = "0") val encodedSize: Long?,
  val type: String,
  @ColumnInfo(name = "attachment_id") val attachmentId: String?,
  @ColumnInfo(name = "file_uri") val fileUri: String?,
  @ColumnInfo(name = "forwarded_folder") val forwardedFolder: String?,
  @ColumnInfo(name = "forwarded_uid", defaultValue = "-1") val forwardedUid: Long?,
  @ColumnInfo(name = "decrypt_when_forward", defaultValue = "0") val decryptWhenForward: Boolean,
  val path: String
) {

  @Ignore
  val isForwarded: Boolean =
    forwardedFolder?.isNotEmpty() == true && (forwardedUid != null && forwardedUid > 0)

  fun toAttInfo(): AttachmentInfo {
    return AttachmentInfo(
      email = email,
      folder = folder,
      uid = uid,
      name = name,
      encodedSize = encodedSize ?: 0,
      type = type,
      id = attachmentId,
      uri = if (fileUri.isNullOrEmpty()) null else Uri.parse(fileUri),
      fwdFolder = forwardedFolder,
      fwdUid = forwardedUid ?: -1,
      path = path,
      isForwarded = isForwarded,
      isEncryptionAllowed = true,
      decryptWhenForward = decryptWhenForward
    )
  }

  companion object {
    fun fromAttInfo(attachmentInfo: AttachmentInfo): AttachmentEntity? {
      with(attachmentInfo) {
        val email = email ?: return null
        val folder = folder ?: return null
        val name = name ?: return null

        return AttachmentEntity(
          id = null,
          email = email,
          folder = folder,
          uid = uid,
          name = name,
          encodedSize = encodedSize,
          type = type,
          attachmentId = id,
          fileUri = if (uri != null) uri.toString() else null,
          forwardedFolder = fwdFolder,
          forwardedUid = fwdUid,
          path = path,
          decryptWhenForward = decryptWhenForward
        )
      }
    }
  }
}
