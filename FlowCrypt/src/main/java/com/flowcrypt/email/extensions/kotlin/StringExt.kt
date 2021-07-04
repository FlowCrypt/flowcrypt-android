/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors:
 *   Ivan Pizhenko
 */

package com.flowcrypt.email.extensions.kotlin

import org.json.JSONObject
import java.io.InputStream
import java.io.UnsupportedEncodingException
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.Base64

fun String.normalizeDashes(): String {
  return this.replace(dashesRegex, "-----")
}

private val dashesRegex = Regex("^—–|—–$")

fun String.normalizeSpaces(): String {
  return this.replace(char160, ' ')
}

private const val char160 = 160.toChar()

fun String.normalize(): String {
  return this.normalizeSpaces().normalizeDashes()
}

fun String.countOfMatches(needle: String): Int {
  var result = 0
  var pos = this.indexOf(needle)
  while (pos > -1) {
    ++result
    pos = this.indexOf(needle, pos + needle.length)
  }
  return result
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
    .replace("\n", "<br/>")
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
  return replace(htmlTagRegex, "") // remove opening and closing html tags
    .replace(headSectionRegex, "") // remove the whole head section
    .replace(bodyTagRegex, "") // remove opening and closing body tags
    .trim()
}

private val htmlTagRegex = Regex("</?html[^>]*>")
private val headSectionRegex = Regex("<head[^>]*>.*</head>")
private val bodyTagRegex = Regex("</?body[^>]*>")

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
    .joinToString { it.toUrlHex() }.decodeUriComponent()
}

fun String.decodeBase64(): ByteArray {
  return Base64.getDecoder().decode(this)
}

// see https://stackoverflow.com/a/611117/1540501
fun String.decodeUriComponent(): String {
  return try {
    URLDecoder.decode(this, "UTF-8")
  } catch (e: UnsupportedEncodingException) {
    // should never happen
    throw IllegalStateException(e)
  }
}

// see https://stackoverflow.com/a/611117/1540501
fun String.encodeUriComponent(): String {
  return try {
    URLEncoder.encode(this, "UTF-8")
      .replace("+", "%20")
      .replace("%21", "!")
      .replace("%27", "'")
      .replace("%28", "(")
      .replace("%29", ")")
      .replace("%7E", "~")
  } catch (e: UnsupportedEncodingException) {
    // should never happen
    throw IllegalStateException(e)
  }
}

fun String.toInputStream(charset: Charset = StandardCharsets.UTF_8): InputStream {
  return toByteArray(charset).inputStream()
}
