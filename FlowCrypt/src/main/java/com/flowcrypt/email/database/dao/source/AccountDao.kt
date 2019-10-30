/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.database.dao.source

import android.accounts.Account
import android.os.Parcel
import android.os.Parcelable
import android.text.TextUtils
import com.flowcrypt.email.api.email.model.AuthCredentials
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import java.util.*

/**
 * The simple POJO object which describes an account information.
 *
 * @author Denis Bondarenko
 * Date: 14.07.2017
 * Time: 17:44
 * E-mail: DenBond7@gmail.com
 */

data class AccountDao constructor(val email: String,
                                  var accountType: String? = null,
                                  val displayName: String? = null,
                                  val givenName: String? = null,
                                  val familyName: String? = null,
                                  val photoUrl: String? = null,
                                  val areContactsLoaded: Boolean = false,
                                  val authCreds: AuthCredentials? = null,
                                  val uuid: String? = null,
                                  val domainRules: List<String>? = null) : Parcelable {

  init {
    if (TextUtils.isEmpty(accountType)) {
      if (!TextUtils.isEmpty(email)) {
        this.accountType = email.substring(email.indexOf('@') + 1, email.length)
      }
    }
  }

  val account: Account? = Account(this.email, accountType)

  constructor(googleSignInAccount: GoogleSignInAccount) : this(
      email = googleSignInAccount.email!!,
      displayName = googleSignInAccount.displayName,
      accountType = googleSignInAccount.account?.type?.toLowerCase(Locale.getDefault()),
      givenName = googleSignInAccount.givenName,
      familyName = googleSignInAccount.familyName,
      photoUrl = googleSignInAccount.photoUrl?.toString())

  constructor(source: Parcel) : this(
      source.readString()!!,
      source.readString(),
      source.readString(),
      source.readString(),
      source.readString(),
      source.readString(),
      1 == source.readInt(),
      source.readParcelable(AuthCredentials::class.java.classLoader),
      source.readString(),
      mutableListOf<String>().apply { source.readStringList(this) })

  constructor(email: String, accountTypeGoogle: String) : this(
      email = email,
      accountType = accountTypeGoogle)

  override fun describeContents(): Int {
    return 0
  }

  override fun writeToParcel(dest: Parcel, flags: Int) {
    with(dest) {
      writeString(email)
      writeString(accountType)
      writeString(displayName)
      writeString(givenName)
      writeString(familyName)
      writeString(photoUrl)
      writeInt((if (areContactsLoaded) 1 else 0))
      writeParcelable(authCreds, flags)
      writeString(uuid)
      writeStringList(domainRules)
    }
  }

  companion object {
    const val ACCOUNT_TYPE_GOOGLE = "com.google"
    const val ACCOUNT_TYPE_OUTLOOK = "outlook.com"
    @JvmField
    val CREATOR: Parcelable.Creator<AccountDao> = object : Parcelable.Creator<AccountDao> {
      override fun createFromParcel(source: Parcel): AccountDao = AccountDao(source)
      override fun newArray(size: Int): Array<AccountDao?> = arrayOfNulls(size)
    }
  }
}
