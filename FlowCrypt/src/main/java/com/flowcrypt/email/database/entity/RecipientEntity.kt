/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.database.entity

import android.os.Parcel
import android.os.Parcelable
import android.provider.BaseColumns
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey
import com.flowcrypt.email.model.PgpContact
import com.flowcrypt.email.security.model.PgpKeyDetails

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
data class RecipientEntity(
  @PrimaryKey(autoGenerate = true) @ColumnInfo(name = BaseColumns._ID) val id: Long? = null,
  val email: String,
  @ColumnInfo(defaultValue = "NULL") val name: String? = null,
  @ColumnInfo(name = "last_use", defaultValue = "0") val lastUse: Long = 0
) : Parcelable {

  @Ignore
  var pgpKeyDetails: PgpKeyDetails? = null

  constructor(parcel: Parcel) : this(
    parcel.readValue(Long::class.java.classLoader) as? Long,
    requireNotNull(parcel.readString()),
    parcel.readString(),
    parcel.readLong()
  ) {
    pgpKeyDetails = parcel.readParcelable(PgpKeyDetails::class.java.classLoader)
  }

  fun toPgpContact(): PgpContact {
    return PgpContact(
      email = email,
      name = name,
      lastUse = lastUse,
      pgpKeyDetails = pgpKeyDetails
    )
  }

  //todo-denbond7 need to think about this class.
  enum class Type {
    TO, CC, BCC
  }

  override fun writeToParcel(parcel: Parcel, flags: Int) {
    parcel.writeValue(id)
    parcel.writeString(email)
    parcel.writeString(name)
    parcel.writeLong(lastUse)
    parcel.writeParcelable(pgpKeyDetails, flags)
  }

  override fun describeContents(): Int {
    return 0
  }

  companion object CREATOR : Parcelable.Creator<RecipientEntity> {
    override fun createFromParcel(parcel: Parcel): RecipientEntity = RecipientEntity(parcel)
    override fun newArray(size: Int): Array<RecipientEntity?> = arrayOfNulls(size)
  }
}
