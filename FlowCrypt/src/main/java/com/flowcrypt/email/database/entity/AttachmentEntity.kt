/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
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
  tableName = "attachments",
  indices = [
    Index(
      name = "account_account_type_folder_uid_in_attachments",
      value = ["account", "account_type", "folder", "uid"]
    ),
    Index(
      name = "account_account_type_folder_uid_path_in_attachments",
      value = ["account", "account_type", "folder", "uid", "path"],
      unique = true
    ),
  ],
  foreignKeys = [
    ForeignKey(
      entity = MessageEntity::class, parentColumns = ["account", "account_type", "folder", "uid"],
      childColumns = ["account", "account_type", "folder", "uid"], onDelete = ForeignKey.CASCADE
    )
  ]
)
data class AttachmentEntity(
  @PrimaryKey(autoGenerate = true) @ColumnInfo(name = BaseColumns._ID) val id: Long?,
  val account: String,
  @ColumnInfo(name = "account_type") val accountType: String,
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
    return AttachmentInfo.Builder(
      email = account,
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
      isLazyForwarded = isForwarded,
      isEncryptionAllowed = true,
      decryptWhenForward = decryptWhenForward
    ).build()
  }

  companion object {
    fun fromAttInfo(attachmentInfo: AttachmentInfo, accountType: String): AttachmentEntity? {
      with(attachmentInfo) {
        val email = email ?: return null
        val folder = folder ?: return null
        val name = name ?: return null

        return AttachmentEntity(
          id = null,
          account = email,
          accountType = accountType,
          folder = folder,
          uid = uid,
          name = name,
          encodedSize = encodedSize,
          type = type,
          attachmentId = id,
          fileUri = uri?.toString(),
          forwardedFolder = fwdFolder,
          forwardedUid = fwdUid,
          path = path,
          decryptWhenForward = decryptWhenForward
        )
      }
    }
  }
}
