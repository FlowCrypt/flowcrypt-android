/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.model

import android.os.Parcel
import android.os.Parcelable
import com.flowcrypt.email.database.MessageState
import java.util.*
import javax.mail.internet.InternetAddress

/**
 * Simple POJO class which describe a general message details.
 *
 * @author DenBond7
 * Date: 28.04.2017
 * Time: 11:51
 * E-mail: DenBond7@gmail.com
 */

data class GeneralMessageDetails constructor(val email: String,
                                             val label: String,
                                             val uid: Int = 0,
                                             val receivedDate: Long = 0,
                                             val sentDate: Long = 0,
                                             var from: List<InternetAddress>? = null,
                                             var to: List<InternetAddress>? = null,
                                             var cc: List<InternetAddress>? = null,
                                             var subject: String? = null,
                                             var msgFlags: List<String> = arrayListOf(),
                                             var rawMsgWithoutAtts: String? = null,
                                             var hasAtts: Boolean = false,
                                             var isEncrypted: Boolean = false,
                                             var msgState: MessageState = MessageState.NONE,
                                             var attsDir: String? = null,
                                             var errorMsg: String? = null) : Parcelable {

  fun isSeen(): Boolean {
    return msgFlags.contains(MessageFlag.SEEN.value)
  }

  /**
   * Generate a list of the all recipients.
   *
   * @return A list of the all recipients
   */
  val allRecipients: List<String>
    get() {
      val emails = ArrayList<String>()

      if (to != null) {
        for (internetAddress in to!!) {
          emails.add(internetAddress.address)
        }
      }

      if (cc != null) {
        for (internetAddress in cc!!) {
          emails.add(internetAddress.address)
        }
      }

      return emails
    }

  constructor(source: Parcel) : this(
      source.readString()!!,
      source.readString()!!,
      source.readInt(),
      source.readLong(),
      source.readLong(),
      mutableListOf<InternetAddress>().apply { source.readList(this, InternetAddress::class.java.classLoader) },
      mutableListOf<InternetAddress>().apply { source.readList(this, InternetAddress::class.java.classLoader) },
      mutableListOf<InternetAddress>().apply { source.readList(this, InternetAddress::class.java.classLoader) },
      source.readString(),
      source.createStringArrayList()!!,
      source.readString(),
      1 == source.readInt(),
      1 == source.readInt(),
      source.readParcelable(MessageState::class.java.classLoader)!!,
      source.readString(),
      source.readString()
  )

  override fun describeContents() = 0

  override fun writeToParcel(dest: Parcel, flags: Int) {
    with(dest) {
      writeString(email)
      writeString(label)
      writeInt(uid)
      writeLong(receivedDate)
      writeLong(sentDate)
      writeList(from)
      writeList(to)
      writeList(cc)
      writeString(subject)
      writeStringList(msgFlags)
      writeString(rawMsgWithoutAtts)
      writeInt((if (hasAtts) 1 else 0))
      writeInt((if (isEncrypted) 1 else 0))
      writeParcelable(msgState, flags)
      writeString(attsDir)
      writeString(errorMsg)
    }
  }

  companion object {
    @JvmField
    @Suppress("unused")
    val CREATOR: Parcelable.Creator<GeneralMessageDetails> = object : Parcelable.Creator<GeneralMessageDetails> {
      override fun createFromParcel(source: Parcel): GeneralMessageDetails = GeneralMessageDetails(source)
      override fun newArray(size: Int): Array<GeneralMessageDetails?> = arrayOfNulls(size)
    }
  }
}
