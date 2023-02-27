/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.model

import android.os.Parcel
import android.os.Parcelable
import com.flowcrypt.email.database.entity.relation.RecipientWithPubKeys
import com.flowcrypt.email.extensions.android.os.readParcelableViaExt
import com.flowcrypt.email.security.model.PrivateKeySourceType
import kotlinx.parcelize.Parcelize

/**
 * This class describes a details about the key. The key can be one of three
 * different types:
 *
 *  * [KeyImportDetails.SourceType.EMAIL]
 *  * [KeyImportDetails.SourceType.FILE]
 *  * [KeyImportDetails.SourceType.CLIPBOARD]
 *
 *
 * @author Denys Bondarenko
 */
@Parcelize
data class KeyImportDetails constructor(
  val keyName: String? = null,
  val value: String,
  val sourceType: SourceType,
  val isPrivateKey: Boolean = false,
  val recipientWithPubKeys: RecipientWithPubKeys? = null
) : Parcelable {

  constructor(parcel: Parcel) : this(
    parcel.readString(),
    parcel.readString()!!,
    parcel.readParcelableViaExt(SourceType::class.java)!!,
    parcel.readByte() != 0.toByte(),
    parcel.readParcelableViaExt(RecipientWithPubKeys::class.java)
  )

  constructor(value: String, sourceType: SourceType) : this(
    keyName = null,
    value = value,
    sourceType = sourceType,
    isPrivateKey = true
  )

  constructor(value: String, sourceType: SourceType, isPrivateKey: Boolean) : this(
    keyName = null,
    value = value,
    sourceType = sourceType,
    isPrivateKey = isPrivateKey,
    recipientWithPubKeys = null
  )

  /**
   * The key available types.
   */
  @Parcelize
  enum class SourceType : Parcelable {
    EMAIL, FILE, CLIPBOARD, NEW, MANUAL_ENTERING, EKM;

    fun toPrivateKeySourceTypeString(): String {
      return when (this) {
        EMAIL, EKM -> PrivateKeySourceType.BACKUP
        FILE, CLIPBOARD, MANUAL_ENTERING -> PrivateKeySourceType.IMPORT
        NEW -> PrivateKeySourceType.NEW
      }.toString()
    }
  }
}
