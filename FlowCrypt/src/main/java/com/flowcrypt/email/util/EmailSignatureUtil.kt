/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.util

internal object EmailSignatureUtil {
  fun containsSignature(message: CharSequence?, signature: String): Boolean {
    return message != null && findSignature(message, signature) != null
  }

  fun replaceSignature(
    message: String,
    oldSignature: String,
    newSignature: String
  ): String? {
    val signatureMatch = findSignature(message, oldSignature) ?: return null
    return message.replaceRange(signatureMatch.range, newSignature)
  }

  private fun findSignature(message: CharSequence, signature: String): MatchResult? {
    val signatureRegex = ("^${Regex.escape(signature)}$").toRegex(RegexOption.MULTILINE)
    return signatureRegex.find(message)
  }
}
