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
import androidx.room.ForeignKey
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey
import org.pgpainless.util.Passphrase

/**
 * @author Denis Bondarenko
 *         Date: 12/5/19
 *         Time: 4:12 PM
 *         E-mail: DenBond7@gmail.com
 */
@Entity(
  tableName = "keys",
  indices = [
    Index(
      name = "account_account_type_in_keys",
      value = ["account", "account_type"]
    ),
    Index(
      name = "fingerprint_account_account_type_in_keys",
      value = ["fingerprint", "account", "account_type"],
      unique = true
    )
  ],
  foreignKeys = [
    ForeignKey(
      entity = AccountEntity::class,
      parentColumns = ["email", "account_type"],
      childColumns = ["account", "account_type"],
      onDelete = ForeignKey.CASCADE
    )
  ]
)
data class KeyEntity(
  @PrimaryKey(autoGenerate = true) @ColumnInfo(name = BaseColumns._ID) val id: Long? = null,
  @ColumnInfo(name = "fingerprint") val fingerprint: String,
  val account: String,
  @ColumnInfo(name = "account_type", defaultValue = "NULL") val accountType: String? = null,
  val source: String,
  @ColumnInfo(name = "public_key") val publicKey: ByteArray,
  @ColumnInfo(name = "private_key") val privateKey: ByteArray,
  @ColumnInfo(name = "passphrase", defaultValue = "NULL") val storedPassphrase: String?,
  @ColumnInfo(name = "passphrase_type", defaultValue = "0") val passphraseType: PassphraseType
) {

  @Ignore
  val privateKeyAsString = String(privateKey)

  @Ignore
  val publicKeyAsString = String(publicKey)

  @Ignore
  val passphrase: Passphrase = if (storedPassphrase == null) {
    Passphrase.emptyPassphrase()
  } else {
    Passphrase.fromPassword(storedPassphrase)
  }

  override fun toString(): String {
    return "KeyEntity(id=$id," +
        " fingerprint='$fingerprint'," +
        " account='$account'," +
        " account_type='$accountType'," +
        " source='$source'," +
        " publicKey=${publicKey.contentToString()}," +
        " privateKey=${privateKey.contentToString()}," +
        " storedPassphrase=(hidden))" +
        " passphraseType='$passphraseType',"
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as KeyEntity

    if (id != other.id) return false
    if (fingerprint != other.fingerprint) return false
    if (account != other.account) return false
    if (accountType != other.accountType) return false
    if (source != other.source) return false
    if (!publicKey.contentEquals(other.publicKey)) return false
    if (!privateKey.contentEquals(other.privateKey)) return false
    if (storedPassphrase != other.storedPassphrase) return false
    if (passphraseType != other.passphraseType) return false
    if (passphrase != other.passphrase) return false

    return true
  }

  override fun hashCode(): Int {
    var result = id?.hashCode() ?: 0
    result = 31 * result + fingerprint.hashCode()
    result = 31 * result + account.hashCode()
    result = 31 * result + (accountType?.hashCode() ?: 0)
    result = 31 * result + source.hashCode()
    result = 31 * result + publicKey.contentHashCode()
    result = 31 * result + privateKey.contentHashCode()
    result = 31 * result + (storedPassphrase?.hashCode() ?: 0)
    result = 31 * result + passphraseType.hashCode()
    result = 31 * result + passphrase.hashCode()
    return result
  }

  enum class PassphraseType(val id: Int) : Parcelable {
    DATABASE(0),
    RAM(1);

    override fun describeContents(): Int {
      return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
      dest.writeInt(ordinal)
    }

    companion object CREATOR : Parcelable.Creator<PassphraseType> {
      override fun createFromParcel(source: Parcel): PassphraseType = values()[source.readInt()]
      override fun newArray(size: Int): Array<PassphraseType?> = arrayOfNulls(size)

      fun findValueById(id: Int): PassphraseType {
        return values().firstOrNull { it.id == id }
          ?: throw IllegalArgumentException("Unsupported key type")
      }
    }
  }
}
