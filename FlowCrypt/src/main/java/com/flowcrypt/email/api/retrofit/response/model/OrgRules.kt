/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.response.model

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

/**
 * @author Denis Bondarenko
 *         Date: 10/29/19
 *         Time: 11:30 AM
 *         E-mail: DenBond7@gmail.com
 */
data class OrgRules constructor(
    @Expose val flags: List<DomainRule>?,
    @SerializedName("custom_keyserver_url")
    @Expose val customKeyserverUrl: String?,
    @SerializedName("key_manager_url")
    @Expose val keyManagerUrl: String?,
    @SerializedName("disallow_attester_search_for_domains")
    @Expose val disallowAttesterSearchForDomains: List<String>?,
    @SerializedName("enforce_keygen_algo")
    @Expose val enforceKeygenAlgo: String?,
    @SerializedName("enforce_keygen_expire_months")
    @Expose val enforceKeygenExpireMonths: Int?
) : Parcelable {

  constructor(parcel: Parcel) : this(
      parcel.createTypedArrayList(DomainRule.CREATOR),
      parcel.readString(),
      parcel.readString(),
      parcel.createStringArrayList(),
      parcel.readString(),
      parcel.readValue(Int::class.java.classLoader) as? Int)

  override fun writeToParcel(parcel: Parcel, flagsList: Int) {
    parcel.writeTypedList(flags)
    parcel.writeString(customKeyserverUrl)
    parcel.writeString(keyManagerUrl)
    parcel.writeStringList(disallowAttesterSearchForDomains)
    parcel.writeString(enforceKeygenAlgo)
    parcel.writeValue(enforceKeygenExpireMonths)
  }

  override fun describeContents(): Int {
    return 0
  }

  enum class DomainRule : Parcelable {
    NO_PRV_CREATE,
    NO_PRV_BACKUP,
    PRV_AUTOIMPORT_OR_AUTOGEN,
    PASS_PHRASE_QUIET_AUTOGEN,
    ENFORCE_ATTESTER_SUBMIT,
    NO_ATTESTER_SUBMIT,
    NO_KEY_MANAGER_PUB_LOOKUP,
    USE_LEGACY_ATTESTER_SUBMIT,
    DEFAULT_REMEMBER_PASS_PHRASE,
    HIDE_ARMOR_META;

    companion object CREATOR : Parcelable.Creator<DomainRule> {
      override fun createFromParcel(parcel: Parcel): DomainRule = values()[parcel.readInt()]
      override fun newArray(size: Int): Array<DomainRule?> = arrayOfNulls(size)
    }

    override fun describeContents(): Int {
      return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
      dest.writeInt(ordinal)
    }
  }

  companion object CREATOR : Parcelable.Creator<OrgRules> {
    override fun createFromParcel(parcel: Parcel): OrgRules {
      return OrgRules(parcel)
    }

    override fun newArray(size: Int): Array<OrgRules?> {
      return arrayOfNulls(size)
    }
  }
}