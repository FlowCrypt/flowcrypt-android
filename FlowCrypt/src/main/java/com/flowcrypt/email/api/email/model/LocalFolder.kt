/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.model

import android.os.Parcelable
import com.flowcrypt.email.api.email.FoldersManager
import com.flowcrypt.email.api.email.JavaEmailConstants
import com.flowcrypt.email.database.entity.LabelEntity
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

/**
 * This is a simple POJO object, which describe information about the email folder.
 *
 * @author Denys Bondarenko
 */
@Parcelize
data class LocalFolder constructor(
  val account: String,
  val fullName: String,
  var folderAlias: String? = null,
  val attributes: List<String>? = null,
  val isCustom: Boolean = false,
  var msgCount: Int = 0,
  var searchQuery: String? = null,
  val labelColor: String? = null,
  val textColor: String? = null,
) : Parcelable {
  constructor(source: LabelEntity) : this(
    source.email,
    source.name,
    source.alias,
    source.attributesList,
    source.isCustom,
    source.messagesTotal,
    null
  )

  @IgnoredOnParcel
  val isOutbox: Boolean = JavaEmailConstants.FOLDER_OUTBOX.equals(fullName, ignoreCase = true)

  @IgnoredOnParcel
  val isAll: Boolean = JavaEmailConstants.FOLDER_ALL_MAIL.equals(fullName, ignoreCase = true)

  @IgnoredOnParcel
  val isDrafts: Boolean = FoldersManager.FolderType.DRAFTS == getFolderType()

  fun getFolderType(): FoldersManager.FolderType? {
    return FoldersManager.getFolderType(this)
  }
}
