/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.extensions.kotlin

import android.content.Context
import android.util.Patterns
import android.util.TypedValue
import android.webkit.MimeTypeMap
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import com.flowcrypt.email.R
import com.flowcrypt.email.util.BetterInternetAddress
import com.flowcrypt.email.util.UIUtil
import jakarta.mail.internet.AddressException
import jakarta.mail.internet.ContentType
import jakarta.mail.internet.InternetAddress
import org.apache.commons.io.FilenameUtils
import org.json.JSONObject
import java.io.InputStream
import java.net.URLDecoder
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.Base64
import java.util.Locale

fun String.normalizeDashes(): String {
  return this.replace(DASHES_REGEX, "-----")
}

private val DASHES_REGEX = Regex("^—–|—–$")

fun String.normalizeSpaces(): String {
  return this.replace(CHAR_160, ' ')
}

private const val CHAR_160 = 160.toChar()

fun String.normalize(): String {
  return this.normalizeSpaces().normalizeDashes()
}

fun String.countOfMatchesZeroOneOrMore(needle: String): Int {
  var result = 0
  var pos = this.indexOf(needle)
  while (pos > -1) {
    if (++result == 2) break
    pos = this.indexOf(needle, pos + needle.length)
  }
  return result
}

fun String.normalizeEol(): String {
  return this.replace("\r\n", "\n").replace('\r', '\n')
}

fun String.removeUtf8Bom(): String {
  return if (this.startsWith("\uFEFF")) this.substring(1) else this
}

fun String.toEscapedHtml(): String {
  return this.replace("&", "&amp;")
    .replace("\"", "&quot;")
    .replace("'", "&#39;")
    .replace("<", "&lt;")
    .replace(">", "&gt;")
    .replace("/", "&#x2F;")
    .replace("\n", "<br>")
}

fun String.unescapeHtml(): String {
  // Comment from Typescript:
  // the &nbsp; at the end is replaced with an actual NBSP character, not a space character.
  // IDE won't show you the difference. Do not change.
  return replace("&#x2F;", "/")
    .replace("&quot;", "\"")
    .replace("&#39;", "'")
    .replace("&lt;", "<")
    .replace("&gt;", ">")
    .replace("&amp;", "&")
    .replace("&nbsp;", " ")
}

fun String.escapeHtmlAttr(): String {
  return replace("&", "&amp;")
    .replace("\"", "&quot;")
    .replace("'", "&#39;")
    .replace("<", "&lt;")
    .replace("<", "&gt;")
    .replace("/", "&#x2F;")
}

fun String.stripHtmlRootTags(): String {
  // Typescript comment: todo - this is very rudimentary, use a proper parser
  return replace(HTML_TAG_REGEX, "") // remove opening and closing html tags
    .replace(HTML_HEAD_SECTION_REGEX, "") // remove the whole head section
    .replace(HTML_BODY_TAG_REGEX, "") // remove opening and closing body tags
    .trim()
}

private val HTML_TAG_REGEX = Regex("</?html[^>]*>")
private val HTML_HEAD_SECTION_REGEX = Regex("<head[^>]*>.*</head>")
private val HTML_BODY_TAG_REGEX = Regex("</?body[^>]*>")

fun String.decodeFcHtmlAttr(): JSONObject? {
  return try {
    JSONObject(decodeBase64Url())
  } catch (e: Exception) {
    e.printStackTrace()
    null
  }
}

fun String.decodeBase64Url(): String {
  return this.replace('+', '-').replace('_', '/').decodeBase64()
    .joinToString { it.toUrlHex() }.urlDecoded()
}

fun String.decodeBase64(): ByteArray {
  return Base64.getDecoder().decode(this)
}

fun String.toInputStream(charset: Charset = StandardCharsets.UTF_8): InputStream {
  return toByteArray(charset).inputStream()
}

fun String.isValidEmail(): Boolean {
  return BetterInternetAddress.isValidEmail(this)
}

fun String.urlDecoded(): String {
  return URLDecoder.decode(this, StandardCharsets.UTF_8.name())
}

fun String?.parseAsColorBasedOnDefaultSettings(
  context: Context,
  defaultColorResourceId: Int = android.R.attr.colorControlNormal,
  secondDefaultColorResourceId: Int = R.color.gray
): Int {
  return runCatching {
    requireNotNull(this).toColorInt()
  }.getOrElse {
    TypedValue().also {
      context.theme.resolveAttribute(defaultColorResourceId, it, true)
    }.let { typedValue ->
      runCatching {
        ContextCompat.getColor(context, typedValue.resourceId)
      }.getOrElse {
        UIUtil.getColor(context, secondDefaultColorResourceId)
      }
    }
  }
}

fun String.capitalize(): String {
  return lowercase().replaceFirstChar { it.titlecase(Locale.ROOT) }
}

fun String?.asInternetAddress(): InternetAddress? {
  return asInternetAddresses().firstOrNull()
}

fun String?.asInternetAddresses(): Array<InternetAddress> {
  return try {
    InternetAddress.parse(this)
  } catch (e: Exception) {
    if (e is AddressException) {
      val pattern = Patterns.EMAIL_ADDRESS
      val matcher = this?.let { pattern.matcher(it) }
      if (matcher?.find() == true) {
        arrayOf(InternetAddress(matcher.group().lowercase()))
      } else emptyArray<InternetAddress>()
    } else {
      emptyArray<InternetAddress>()
    }
  }
}

fun String?.asContentTypeOrNull(): ContentType? {
  return try {
    ContentType(this)
  } catch (e: Exception) {
    null
  }
}

fun String?.getPossibleAndroidMimeType(): String? {
  return MimeTypeMap.getSingleton()
    .getMimeTypeFromExtension(FilenameUtils.getExtension(this).lowercase())
}

val String.toLongRadix16: Long
  get() = toLong(radix = 16)

fun String.clip(context: Context, maxSize: Int): String {
  return if (length > maxSize) {
    take(maxSize) + "\n\n" + context.getString(R.string.clipped_message_too_large)
  } else this
}