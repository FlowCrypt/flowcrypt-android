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
import com.flowcrypt.email.api.retrofit.request.model.TestWelcomeModel
import com.flowcrypt.email.util.exception.ApiException
import com.google.gson.annotations.SerializedName

/**
 * This action describes a task which sends a welcome message to the user
 * using API "https://flowcrypt.com/attester/test/welcome".
 *
 * @author Denis Bondarenko
 * Date: 30.01.2018
 * Time: 18:10
 * E-mail: DenBond7@gmail.com
 */
data class SendWelcomeTestEmailAction @JvmOverloads constructor(
  override var id: Long = 0,
  override var email: String? = null,
  override val version: Int = 0,
  private val publicKey: String
) : Action, Parcelable {
  @SerializedName(Action.TAG_NAME_ACTION_TYPE)
  override val type: Action.Type = Action.Type.SEND_WELCOME_TEST_EMAIL

  override fun run(context: Context) {
    val apiService = ApiHelper.getInstance(context).retrofit.create(ApiService::class.java)
    val body = TestWelcomeModel(email!!, publicKey)
    val response = apiService.postTestWelcome(body).execute()

    val (apiError) = response.body() ?: throw IllegalArgumentException("The response is null!")

    if (apiError != null) {
      throw ApiException(apiError)
    }
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
    val CREATOR: Parcelable.Creator<SendWelcomeTestEmailAction> =
      object : Parcelable.Creator<SendWelcomeTestEmailAction> {
        override fun createFromParcel(source: Parcel): SendWelcomeTestEmailAction =
          SendWelcomeTestEmailAction(source)

        override fun newArray(size: Int): Array<SendWelcomeTestEmailAction?> = arrayOfNulls(size)
      }
  }
}
