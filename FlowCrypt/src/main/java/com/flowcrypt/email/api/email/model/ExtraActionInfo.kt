/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.model

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Parcelable
import com.flowcrypt.email.api.email.EmailUtil
import com.flowcrypt.email.extensions.android.content.getParcelableArrayListExtraViaExt
import com.flowcrypt.email.extensions.android.content.getParcelableExtraViaExt
import com.flowcrypt.email.util.RFC6068Parser
import kotlinx.parcelize.Parcelize

/**
 * This class describes information about incoming extra info from the intent with one of next actions:
 *
 *  * [Intent.ACTION_VIEW]
 *  * [Intent.ACTION_SENDTO]
 *  * [Intent.ACTION_SEND]
 *  * [Intent.ACTION_SEND_MULTIPLE]
 *
 *
 * @author Denys Bondarenko
 */
@Parcelize
data class ExtraActionInfo(
  val atts: List<AttachmentInfo> = emptyList(),
  val initializationData: InitializationData
) : Parcelable {

  companion object {
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
      var infoFromRFC6068Parser: ExtraActionInfo? = null
      val attsList = ArrayList<AttachmentInfo>()

      //parse mailto: URI
      if (intent.action in listOf(Intent.ACTION_VIEW, Intent.ACTION_SENDTO)) {
        if (RFC6068Parser.isMailTo(intent.data)) {
          infoFromRFC6068Parser = RFC6068Parser.parse(intent.data)
        } else return null
      }

      when (intent.action) {
        Intent.ACTION_VIEW, Intent.ACTION_SENDTO, Intent.ACTION_SEND, Intent.ACTION_SEND_MULTIPLE -> {
          if (Intent.ACTION_SEND == intent.action) {
            val uri = intent.getParcelableExtraViaExt<Uri>(Intent.EXTRA_STREAM)
            if (uri != null) {
              val attachmentInfo = EmailUtil.getAttInfoFromUri(context, uri)
              attachmentInfo?.let { attsList.add(attachmentInfo) }
            }
          } else {
            val uriList = intent.getParcelableArrayListExtraViaExt<Parcelable>(Intent.EXTRA_STREAM)
            if (uriList != null) {
              for (parcelable in uriList) {
                val uri = parcelable as Uri
                val attachmentInfo = EmailUtil.getAttInfoFromUri(context, uri)
                attachmentInfo?.let { attsList.add(attachmentInfo) }
              }
            }
          }
        }
      }

      val initializationData = infoFromRFC6068Parser?.initializationData

      val finalSubject = initializationData?.subject ?: intent.getStringExtra(Intent.EXTRA_SUBJECT)
      val finalBody = initializationData?.body ?: intent.getStringExtra(Intent.EXTRA_TEXT)

      val getEmailList = { key: String ->
        ArrayList((intent.getStringArrayExtra(key) ?: emptyArray()).toList())
      }

      val finalTo = initializationData?.toAddresses ?: getEmailList(Intent.EXTRA_EMAIL)
      val finalCc = initializationData?.ccAddresses ?: getEmailList(Intent.EXTRA_CC)
      val finalBcc = initializationData?.bccAddresses ?: getEmailList(Intent.EXTRA_BCC)

      return infoFromRFC6068Parser?.copy(
        atts = attsList,
        initializationData = infoFromRFC6068Parser.initializationData.copy(
          subject = finalSubject,
          body = finalBody,
          toAddresses = finalTo,
          ccAddresses = finalCc,
          bccAddresses = finalBcc
        )
      ) ?: ExtraActionInfo(
        atts = attsList,
        initializationData = InitializationData(
          subject = finalSubject,
          body = finalBody,
          toAddresses = finalTo,
          ccAddresses = finalCc,
          bccAddresses = finalBcc
        )
      )
    }
  }
}
