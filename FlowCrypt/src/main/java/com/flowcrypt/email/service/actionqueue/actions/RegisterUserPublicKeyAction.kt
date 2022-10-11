/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.service.actionqueue.actions

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import com.flowcrypt.email.api.retrofit.ApiHelper
import com.flowcrypt.email.api.retrofit.ApiService
import com.google.gson.annotations.SerializedName

/**
 * This action describes a task which registers a user public key
 * using API "https://flowcrypt.com/attester/initial/legacy_submit".
 *
 * @author Denis Bondarenko
 * Date: 30.01.2018
 * Time: 18:01
 * E-mail: DenBond7@gmail.com
 */
data class RegisterUserPublicKeyAction @JvmOverloads constructor(
  override var id: Long = 0,
  override var email: String? = null,
  override val version: Int = 0,
  private val publicKey: String
) : Action {
  @SerializedName(Action.TAG_NAME_ACTION_TYPE)
  override val type: Action.Type = Action.Type.REGISTER_USER_PUBLIC_KEY

  override suspend fun run(context: Context) {
    val apiService = ApiHelper.getInstance(context).retrofit.create(ApiService::class.java)
    /*val body = InitialLegacySubmitModel(email!!, publicKey)
    val response = apiService.postInitialLegacySubmit(body).execute()

    val (apiError) = response.body() ?: throw IllegalArgumentException("The response is null!")

    if (apiError != null) {
      val code = apiError.code ?: 0
      if (code < 400 || code >= 500) {
        throw ApiException(apiError)
      }
    }*/
  }

  constructor(source: Parcel) : this(
    source.readLong(),
    source.readString(),
    source.readInt(),
    source.readString()!!
  )

  override fun describeContents(): Int {
    return 0
  }

  override fun writeToParcel(dest: Parcel, flags: Int) =
    with(dest) {
      writeLong(id)
      writeString(email)
      writeInt(version)
      writeString(publicKey)
    }

  companion object {
    @JvmField
    val CREATOR: Parcelable.Creator<RegisterUserPublicKeyAction> =
      object : Parcelable.Creator<RegisterUserPublicKeyAction> {
        override fun createFromParcel(source: Parcel): RegisterUserPublicKeyAction =
          RegisterUserPublicKeyAction(source)

        override fun newArray(size: Int): Array<RegisterUserPublicKeyAction?> = arrayOfNulls(size)
      }
  }
}
