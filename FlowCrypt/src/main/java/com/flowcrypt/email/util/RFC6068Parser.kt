/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.util

import android.net.Uri
import android.text.TextUtils
import com.flowcrypt.email.api.email.model.ExtraActionInfo
import com.flowcrypt.email.api.email.model.InitializationData
import java.util.Arrays

/**
 * This class defines the parser of 'mailto' URIs.
 * It depends on the [document](https://tools.ietf.org/html/rfc6068) which defines the format of Uniform
 * Resource Identifiers (URIs) to identify resources that are reached using Internet mail.
 *
 *
 * See details here https://github.com/k9mail/k-9/blob/master/k9mail/src/main/java/com/fsck/k9/helper/MailTo.java
 *
 * @author Denis Bondarenko
 * Date: 13.03.2018
 * Time: 14:41
 * E-mail: DenBond7@gmail.com
 */
class RFC6068Parser {
  companion object {
    private const val TO = "to"
    private const val BODY = "body"
    private const val CC = "cc"
    private const val BCC = "bcc"
    private const val SUBJECT = "subject"

    fun isMailTo(uri: Uri?): Boolean {
      return androidx.core.net.MailTo.isMailTo(uri)
    }

    fun parse(uri: Uri?): ExtraActionInfo {
      if (uri == null) {
        throw NullPointerException("Argument 'uri' must not be null")
      }

      if (!isMailTo(uri)) {
        throw IllegalArgumentException("Not a mailto scheme")
      }

      val schemaSpecific = uri.schemeSpecificPart
      var end = schemaSpecific.indexOf('?')
      if (end == -1) {
        end = schemaSpecific.length
      }

      val newUri = Uri.parse("foo://bar?" + uri.encodedQuery)
      val params = CaseInsensitiveParamWrapper(newUri)

      // Extract the recipient's email address from the mailto URI if there's one.
      val recipient = Uri.decode(schemaSpecific.substring(0, end))

      var toList = params.getQueryParameters(TO)
      if (recipient.length != 0) {
        toList.add(0, recipient)
      }

      toList = checkToList(toList)

      val ccList = params.getQueryParameters(CC)
      val bccList = params.getQueryParameters(BCC)

      val subject = getFirstParameterValue(params, SUBJECT)
      val body = getFirstParameterValue(params, BODY)

      return ExtraActionInfo(
        atts = emptyList(),
        initializationData = InitializationData(
          subject = subject,
          body = body,
          toAddresses = toList,
          ccAddresses = ccList,
          bccAddresses = bccList
        )
      )
    }

    private fun checkToList(toList: ArrayList<String>): ArrayList<String> {
      val newToList = ArrayList<String>()
      if (!toList.isEmpty()) {
        for (section in toList) {
          if (!TextUtils.isEmpty(section)) {
            if (section.indexOf(',') != -1) {
              val arraysRecipients =
                section.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
              newToList.addAll(ArrayList(Arrays.asList(*arraysRecipients)))
            } else {
              newToList.add(section)
            }
          }
        }
      }
      return newToList
    }

    private fun getFirstParameterValue(
      params: CaseInsensitiveParamWrapper,
      paramName: String
    ): String? {
      val paramValues = params.getQueryParameters(paramName)
      return if (paramValues.isEmpty()) null else paramValues[0]
    }
  }


  private class CaseInsensitiveParamWrapper(private val uri: Uri) {

    fun getQueryParameters(key: String): ArrayList<String> {
      val params = ArrayList<String>()
      for (paramName in uri.queryParameterNames) {
        if (paramName.equals(key, ignoreCase = true)) {
          params.addAll(uri.getQueryParameters(paramName))
        }
      }

      return params
    }
  }
}
