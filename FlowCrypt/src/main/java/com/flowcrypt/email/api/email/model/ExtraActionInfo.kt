/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.model

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Parcel
import android.os.Parcelable
import android.text.TextUtils
import com.flowcrypt.email.api.email.EmailUtil
import com.flowcrypt.email.util.RFC6068Parser

/**
 * This class describes information about incoming extra info from the intent with one of next actions:
 *
 *  * [Intent.ACTION_VIEW]
 *  * [Intent.ACTION_SENDTO]
 *  * [Intent.ACTION_SEND]
 *  * [Intent.ACTION_SEND_MULTIPLE]
 *
 *
 * @author Denis Bondarenko
 * Date: 13.03.2018
 * Time: 16:16
 * E-mail: DenBond7@gmail.com
 */
data class ExtraActionInfo constructor(
  var atts: List<AttachmentInfo> = emptyList(),
  val toAddresses: List<String> = arrayListOf(),
  val ccAddresses: List<String> = arrayListOf(),
  val bccAddresses: List<String> = arrayListOf(),
  var subject: String = "",
  var body: String = ""
) : Parcelable {

  constructor(parcel: Parcel) : this(
    mutableListOf<AttachmentInfo>().apply { parcel.readTypedList(this, AttachmentInfo.CREATOR) },
    parcel.createStringArrayList()!!,
    parcel.createStringArrayList()!!,
    parcel.createStringArrayList()!!,
    parcel.readString()!!,
    parcel.readString()!!
  )

  override fun describeContents(): Int {
    return 0
  }

  override fun writeToParcel(dest: Parcel, flags: Int) {
    dest.writeTypedList(atts)
    dest.writeStringList(toAddresses)
    dest.writeStringList(ccAddresses)
    dest.writeStringList(bccAddresses)
    dest.writeString(subject)
    dest.writeString(body)
  }

  companion object {
    @JvmField
    @Suppress("unused")
    val CREATOR: Parcelable.Creator<ExtraActionInfo> =
      object : Parcelable.Creator<ExtraActionInfo> {
        override fun createFromParcel(source: Parcel): ExtraActionInfo = ExtraActionInfo(source)
        override fun newArray(size: Int): Array<ExtraActionInfo?> = arrayOfNulls(size)
      }

    /**
     * Parse incoming information from the intent which can have the next actions:
     *
     *  * [Intent.ACTION_VIEW]
     *  * [Intent.ACTION_SENDTO]
     *  * [Intent.ACTION_SEND]
     *  * [Intent.ACTION_SEND_MULTIPLE]
     *
     *
     * @param intent An incoming intent.
     */
    fun parseExtraActionInfo(context: Context, intent: Intent): ExtraActionInfo? {
      var info: ExtraActionInfo? = null

      //parse mailto: URI
      if (intent.action in listOf(Intent.ACTION_VIEW, Intent.ACTION_SENDTO)) {
        if (RFC6068Parser.isMailTo(intent.data)) {
          info = RFC6068Parser.parse(intent.data)
        } else return null
      }

      if (info == null) {
        info = ExtraActionInfo()
      }

      when (intent.action) {
        Intent.ACTION_VIEW, Intent.ACTION_SENDTO, Intent.ACTION_SEND, Intent.ACTION_SEND_MULTIPLE -> {

          val extraText = intent.getCharSequenceExtra(Intent.EXTRA_TEXT)
          // Only use EXTRA_TEXT if the body hasn't already been set by the mailto: URI
          if (extraText != null && TextUtils.isEmpty(info.body)) {
            info.body = extraText.toString()
          }

          val subj = intent.getStringExtra(Intent.EXTRA_SUBJECT)
          // Only use EXTRA_SUBJECT if the subject hasn't already been set by the mailto: URI
          if (subj != null && TextUtils.isEmpty(info.subject)) {
            info.subject = subj
          }

          val attsList = ArrayList<AttachmentInfo>()

          if (Intent.ACTION_SEND == intent.action) {
            val stream = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
            if (stream != null) {
              val attachmentInfo = EmailUtil.getAttInfoFromUri(context, stream)
              attachmentInfo?.let {
                attsList.add(attachmentInfo)
              }
            }
          } else {
            val uriList = intent.getParcelableArrayListExtra<Parcelable>(Intent.EXTRA_STREAM)
            if (uriList != null) {
              for (parcelable in uriList) {
                val uri = parcelable as Uri
                val attachmentInfo = EmailUtil.getAttInfoFromUri(context, uri)
                attachmentInfo?.let {
                  attsList.add(attachmentInfo)
                }
              }
            }
          }

          info.atts = attsList
        }
      }

      return info
    }
  }
}
