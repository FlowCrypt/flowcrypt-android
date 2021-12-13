/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: Ivan Pizhenko,
 *               DenBond7
 */

package com.flowcrypt.email.security.pgp

import android.content.Context
import com.flowcrypt.email.api.email.JavaEmailConstants
import com.flowcrypt.email.api.retrofit.response.model.AttMeta
import com.flowcrypt.email.api.retrofit.response.model.AttMsgBlock
import com.flowcrypt.email.api.retrofit.response.model.DecryptErrorMsgBlock
import com.flowcrypt.email.api.retrofit.response.model.DecryptedAndOrSignedContentMsgBlock
import com.flowcrypt.email.api.retrofit.response.model.EncryptedAttLinkMsgBlock
import com.flowcrypt.email.api.retrofit.response.model.EncryptedAttMsgBlock
import com.flowcrypt.email.api.retrofit.response.model.MsgBlock
import com.flowcrypt.email.api.retrofit.response.model.MsgBlockError
import com.flowcrypt.email.api.retrofit.response.model.MsgBlockFactory
import com.flowcrypt.email.api.retrofit.response.model.PublicKeyMsgBlock
import com.flowcrypt.email.api.retrofit.response.model.SignedMsgBlock
import com.flowcrypt.email.api.retrofit.response.model.VerificationResult
import com.flowcrypt.email.core.msg.MimeUtils
import com.flowcrypt.email.core.msg.MsgBlockParser
import com.flowcrypt.email.database.FlowCryptRoomDatabase
import com.flowcrypt.email.extensions.java.io.readText
import com.flowcrypt.email.extensions.javax.mail.internet.hasFileName
import com.flowcrypt.email.extensions.javax.mail.isInline
import com.flowcrypt.email.extensions.kotlin.decodeFcHtmlAttr
import com.flowcrypt.email.extensions.kotlin.escapeHtmlAttr
import com.flowcrypt.email.extensions.kotlin.stripHtmlRootTags
import com.flowcrypt.email.extensions.kotlin.toEscapedHtml
import com.flowcrypt.email.extensions.kotlin.toInputStream
import com.flowcrypt.email.extensions.kotlin.unescapeHtml
import com.flowcrypt.email.extensions.org.bouncycastle.openpgp.armor
import com.flowcrypt.email.extensions.org.owasp.html.allowAttributesOnElementsExt
import com.flowcrypt.email.security.KeysStorageImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.bouncycastle.openpgp.PGPPublicKeyRing
import org.bouncycastle.openpgp.PGPPublicKeyRingCollection
import org.bouncycastle.openpgp.PGPSecretKeyRingCollection
import org.bouncycastle.openpgp.PGPSignature
import org.json.JSONObject
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode
import org.owasp.html.HtmlPolicyBuilder
import org.pgpainless.key.SubkeyIdentifier
import org.pgpainless.key.protection.SecretKeyRingProtector
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.util.Properties
import javax.mail.Multipart
import javax.mail.Part
import javax.mail.Session
import javax.mail.internet.ContentType
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage
import javax.mail.internet.MimePart
import kotlin.random.Random

object PgpMsg {
  private const val GENERAL_CSS =
    "background: white;padding-left: 8px;min-height: 50px;padding-top: 4px;" +
        "padding-bottom: 4px;width: 100%;"
  private const val SEAMLESS_LOCK_BG = "iVBORw0KGgoAAAANSUhEUgAAAFoAAABaCAMAAAAPdrEwAAAAh1BMVEXw" +
      "8PD////w8PDw8PDw8PDw8PDw8PDw8PDw8PDw8PDw8PDw8PDw8PDw8PDw8PDw8PDw8PDw8PDw8PDw8PDw8PDw8PD" +
      "w8PDw8PDw8PDw8PDw8PDw8PDw8PDw8PDw8PDw8PDw8PDw8PDw8PDw8PDw8PDw8PDw8PDw8PDw8PDw8PDw8PDw8P" +
      "Dw8PD7MuHIAAAALXRSTlMAAAECBAcICw4QEhUZIyYqMTtGTV5kdn2Ii5mfoKOqrbG0uL6/xcnM0NTX2t1l7cN4A" +
      "AAB0UlEQVR4Ae3Y3Y4SQRCG4bdHweFHRBTBH1FRFLXv//qsA8kmvbMdXhh2Q0KfknpSCQc130c67s22+e9+v/+d" +
      "84fxkSPH0m/+5P9vN7vRV0vPfx7or1NB23e99KAHuoXOOc6moQsBwNN1Q9g4Wdh1uq3MA7Qn0+2ylAt7WbWpyT+" +
      "Wo8roKH6v2QhZ2ghZ2ghZ2ghZ2ghZ2ghZ2ghZ2ghZ2ghZ2ghZ2ghZ2ghZ2ghZ2gjZ2AUNOLmwgQdogEJ2dnF3UJ" +
      "dU3WjqO/u96aYtVd/7jqvIyu76G5se6GaY7tNNcy5d7se7eWVnDz87fMkuVuS8epF6f9NPObPY5re9y4N1/vya9" +
      "Gr3se2bfvl9M0mkyZdv077p+a/3z4Meby5Br4NWiV51BaiUqfLro9I3WiR61RVcffwfXI7u5zZ20EOA82Uu8x3S" +
      "lrSwXQuBSvSqK0AletUVoBK96gpIwlZy0MJWctDCVnLQwlZy0MJWctDCVnLQwlZy0MJWctDCVnLQwlZy0MJWctD" +
      "CVnLQwlZy0MJWckIletUVIJJxITN6wtZd2EI+0NquyIJOnUpFVvRpcwmV6FVXgEr0qitAJXrVFaASveoKUIledQ" +
      "WoRK+6AlSiV13BP+/VVbky7Xq1AAAAAElFTkSuQmCC"
  private const val FC_INNER_TEXT_TYPE_ATTR = "data-fc-inner-text-type"
  private const val FC_FROM_IMAGE_ATTR = "data-fc-is-from-image"

  private val HIDDEN_FILE_NAMES = setOf(
    "PGPexch.htm.pgp",
    "PGPMIME version identification",
    "Version.txt",
    "PGPMIME Versions Identification"
  )

  private val ENCRYPTED_MSG_NAMES = setOf(
    "message",
    "msg.asc",
    "message.asc",
    "encrypted.asc",
    "encrypted.eml.pgp",
    "Message.pgp",
    "openpgp-encrypted-message.asc"
  )

  private val ENCRYPTED_FILE_REGEX = Regex(
    pattern = "(\\.pgp\$)|(\\.gpg\$)|(\\.[a-zA-Z0-9]{3,4}\\.asc\$)",
    option = RegexOption.IGNORE_CASE
  )
  private val PRIVATE_KEY_REGEX = Regex(
    pattern = "(cryptup|flowcrypt)-backup-[a-z0-9]+\\.(key|asc)\$",
    option = RegexOption.IGNORE_CASE
  )
  private val PUBLIC_KEY_REGEX_1 = Regex(
    pattern = "^(0|0x)?[A-F0-9]{8}([A-F0-9]{8})?.*\\.asc\$",
    option = RegexOption.IGNORE_CASE
  )
  private val PUBLIC_KEY_REGEX_2 = Regex(
    pattern = "[A-F0-9]{8}.*\\.asc\$",
    option = RegexOption.IGNORE_CASE
  )
  private val PUBLIC_KEY_REGEX_3 = Regex(
    pattern = "^(0x)?[A-Fa-f0-9]{16,40}\\.asc\\.pgp$",
    option = RegexOption.IGNORE_CASE
  )
  private val CID_CORRECTION_REGEX_1 = Regex(">$")
  private val CID_CORRECTION_REGEX_2 = Regex("^<")
  private val IMG_SRC_WITH_CID_REGEX = Regex("src=\"cid:([^\"]+)\"")
  private val BLOCK_START_REGEX = Regex(
    "<(p|h1|h2|h3|h4|h5|h6|ol|ul|pre|address|blockquote|dl|div|fieldset|form|hr|table)[^>]*>"
  )
  private val BLOCK_END_REGEX = Regex(
    "</(p|h1|h2|h3|h4|h5|h6|ol|ul|pre|address|blockquote|dl|div|fieldset|form|hr|table)[^>]*>"
  )
  private val MULTI_NEW_LINE_REGEX = Regex("\\n{2,}")
  private val HTML_BR_REGEX = Regex("<br[^>]*>")
  private val FC_REPLY_TOKEN_REGEX = Regex("<div[^>]+class=\"cryptup_reply\"[^>]+></div>")
  private val FC_ATT_REGEX = Regex(
    "<a\\s+href=\"([^\"]+)\"\\s+class=\"cryptup_file\"\\s+cryptup-data=" +
        "\"([^\"]+)\"\\s*>[^<]+</a>\\n?"
  )

  private val FRAME_CSS_MAP = mapOf(
    FrameColor.GREEN to "border: 1px solid #f0f0f0;border-left: 8px solid #31A217;" +
        "border-right: none;background-image: url(data:image/png;base64,${SEAMLESS_LOCK_BG});",
    FrameColor.GRAY to "border: 1px solid #f0f0f0;border-left: 8px solid #989898;" +
        "border-right: none;",
    FrameColor.RED to "border: 1px solid #f0f0f0;border-left: 8px solid #d14836;" +
        "border-right: none;",
    FrameColor.PLAIN to "border: none;"
  )

  private val ALLOWED_ELEMENTS = arrayOf(
    "p",
    "div",
    "br",
    "u",
    "i",
    "em",
    "b",
    "ol",
    "ul",
    "pre",
    "li",
    "table",
    "thead",
    "tbody",
    "tfoot",
    "tr",
    "td",
    "th",
    "h1",
    "h2",
    "h3",
    "h4",
    "h5",
    "h6",
    "hr",
    "address",
    "blockquote",
    "dl",
    "fieldset",
    "a",
    "font",
    "strong",
    "strike",
    "code",
    "img"
  )

  private val ALLOWED_ATTRS = mapOf(
    "a" to arrayOf("href", "name", "target", "data-x-fc-inner-text-type"),
    "img" to arrayOf("src", "width", "height", "alt"),
    "font" to arrayOf("size", "color", "face"),
    "span" to arrayOf("color"),
    "div" to arrayOf("color"),
    "p" to arrayOf("color"),
    "em" to arrayOf("style"), // Typescript: tests rely on this, could potentially remove
    "td" to arrayOf("width", "height"),
    "hr" to arrayOf("color", "height")
  )

  private val ALLOWED_PROTOCOLS = arrayOf(
    "data",
    "http",
    "https",
    "mailto"
  )

  private val HTML_POLICY_WITH_BASIC_TAGS_ONLY_FACTORY = HtmlPolicyBuilder()
    .allowElements(*ALLOWED_ELEMENTS)
    .allowUrlProtocols(*ALLOWED_PROTOCOLS)
    .allowAttributesOnElementsExt(ALLOWED_ATTRS)
    .toFactory()

  suspend fun processMimeMessage(context: Context, inputStream: InputStream):
      ProcessedMimeMessageResult = withContext(Dispatchers.IO) {
    val keysStorage = KeysStorageImpl.getInstance(context)
    val pgpSecretKeyRingCollection = PGPSecretKeyRingCollection(keysStorage.getPGPSecretKeyRings())
    val protector = keysStorage.getSecretKeyRingProtector()
    val msg = MimeMessage(Session.getInstance(Properties()), inputStream)

    val keys = mutableListOf<PGPPublicKeyRing>()
    val pubKeyDao = FlowCryptRoomDatabase.getDatabase(context).pubKeyDao()

    for (address in msg.from) {
      if (address is InternetAddress) {
        val existingPubKeysInfo = pubKeyDao.getPublicKeysByRecipient(address.address.lowercase())
        for (existingPublicKeyEntity in existingPubKeysInfo) {
          keys.addAll(
            //ask Tom about this place. Do we need to catch exception here or we can throw it
            PgpKey.parseKeys(source = existingPublicKeyEntity.publicKey)
              .pgpKeyRingCollection.pgpPublicKeyRingCollection
          )
        }
      }
    }

    return@withContext processMimeMessage(
      msg = msg,
      pgpPublicKeyRingCollection = PGPPublicKeyRingCollection(keys),
      pgpSecretKeyRingCollection = pgpSecretKeyRingCollection,
      protector = protector
    )
  }

  fun processMimeMessage(
    inputStream: InputStream,
    pgpSecretKeyRingCollection: PGPSecretKeyRingCollection,
    protector: SecretKeyRingProtector
  ): ProcessedMimeMessageResult {
    return processMimeMessage(
      msg = MimeMessage(Session.getInstance(Properties()), inputStream),
      pgpSecretKeyRingCollection = pgpSecretKeyRingCollection,
      protector = protector
    )
  }

  fun processMimeMessage(
    msg: MimeMessage,
    pgpPublicKeyRingCollection: PGPPublicKeyRingCollection = PGPPublicKeyRingCollection(listOf()),
    pgpSecretKeyRingCollection: PGPSecretKeyRingCollection,
    protector: SecretKeyRingProtector
  ): ProcessedMimeMessageResult {
    val extractedMimeContent = extractMimeContent(msg)
    val extractedMsgBlocks = extractMsgBlocks(extractedMimeContent)
    return processExtractedMsgBlocks(
      extractedMsgBlocks,
      pgpPublicKeyRingCollection,
      pgpSecretKeyRingCollection,
      protector
    )
  }

  fun extractMimeContent(msg: MimeMessage): ExtractedMimeContent {
    var signature: ByteArray? = null
    var html: StringBuilder? = null
    var plainText: StringBuilder? = null
    val attachments = mutableListOf<MimePart>()

    val arrayDeque = ArrayDeque<Part>()
    arrayDeque.addFirst(msg)

    while (arrayDeque.isNotEmpty()) {
      val part = arrayDeque.removeFirst()
      if (part.isMimeType(JavaEmailConstants.MIME_TYPE_MULTIPART)) {
        val multipart = (part.content as? Multipart) ?: continue
        val partsCount = multipart.count
        for (i in 0 until partsCount) {
          arrayDeque.addFirst(multipart.getBodyPart(partsCount - i - 1))
        }
      } else {
        val singlePart = (part as? MimePart) ?: continue
        if (Part.ATTACHMENT.equals(singlePart.disposition, ignoreCase = true)) {
          attachments.add(singlePart)
        } else {
          val contentType = try {
            ContentType(singlePart.contentType).baseType?.lowercase()
          } catch (e: Exception) {
            null
          }

          when (contentType) {
            "application/pgp-signature" -> {
              signature = singlePart.inputStream.readBytes()
            }

            // this one was not in the Typescript, but I had to add it to pass some tests
            "message/rfc822" -> {
              arrayDeque.addFirst(singlePart.content as Part)
            }

            "text/html" -> {
              if (!singlePart.hasFileName()) {
                if (html == null) {
                  html = StringBuilder()
                }
                html.append(singlePart.content)
              }
            }

            "text/plain" -> {
              if (!singlePart.hasFileName() || singlePart.isInline()) {
                if (plainText == null) {
                  plainText = StringBuilder()
                } else {
                  plainText.append("\n\n")
                }
                plainText.append(singlePart.content)
              }
            }

            "text/rfc822-headers" -> {
              // skip
            }

            else -> {
              attachments.add(singlePart)
            }
          }
        }
      }
    }

    return ExtractedMimeContent(attachments, signature, html?.toString(), plainText?.toString())
  }

  fun treatAs(att: MimePart): TreatAs {
    val name = att.fileName?.lowercase() ?: ""
    val type = try {
      ContentType(att.contentType).baseType?.lowercase()
    } catch (e: Exception) {
      null
    }
    val length = att.size
    when {
      name in HIDDEN_FILE_NAMES -> {
        // PGPexch.htm.pgp is html alternative of textual body content produced
        // by the PGP Desktop and GPG4o
        return TreatAs.HIDDEN
      }

      "" == name && type?.startsWith("image/") != true -> {
        return if (length < 100) TreatAs.SIGNATURE else TreatAs.ENCRYPTED_MSG
      }

      "signature.asc" == name || "application/pgp-signature" == type -> {
        return TreatAs.SIGNATURE
      }

      "msg.asc" == name && length < 100 && "application/pgp-encrypted" == type -> {
        // mail.ch does this - although it looks like encrypted msg,
        // it will just contain PGP version eg "Version: 1"
        return TreatAs.SIGNATURE
      }

      "Message.pgp" == att.fileName || name in ENCRYPTED_MSG_NAMES -> {
        return TreatAs.ENCRYPTED_MSG
      }

      ENCRYPTED_FILE_REGEX.containsMatchIn(name) -> {
        // ends with one of .gpg, .pgp, .???.asc, .????.asc
        return TreatAs.ENCRYPTED_FILE
      }

      PRIVATE_KEY_REGEX.containsMatchIn(name) -> {
        return TreatAs.PRIVATE_KEY
      }

      "application/pgp-keys" == type -> {
        return TreatAs.PUBLIC_KEY
      }

      PUBLIC_KEY_REGEX_1.containsMatchIn(name) -> {
        // name starts with a key id
        return TreatAs.PUBLIC_KEY
      }

      name.contains("public") && PUBLIC_KEY_REGEX_2.containsMatchIn(name) -> {
        // name contains the word "public", any key id and ends with .asc
        return TreatAs.PUBLIC_KEY
      }

      name.endsWith(".asc") && length > 0 && checkForPublicKeyBlock(att.inputStream) -> {
        return TreatAs.PUBLIC_KEY
      }

      name.endsWith(".asc") && length < 100000 && !att.isInline() -> {
        return TreatAs.ENCRYPTED_MSG
      }

      else -> {
        return TreatAs.PLAIN_FILE
      }
    }
  }

  /**
   * Used whenever untrusted remote content (eg html email) is rendered, but we still want to
   * preserve HTML. "imgToLink" is ignored. Remote links are replaced with <a>, and local images
   * are preserved.
   */
  fun sanitizeHtmlKeepBasicTags(dirtyHtml: String?): String? {
    if (dirtyHtml == null) return null
    val imgContentReplaceable = "IMG_ICON_${generateRandomSuffix()}"
    var remoteContentReplacedWithLink = false
    val policyFactory = HtmlPolicyBuilder()
      .allowElements(
        { elementName, attrs ->
          // Remove tiny elements (often contain hidden content, tracking pixels, etc)
          for (i in 0 until attrs.size / 2) {
            val j = i * 2
            if (
              (attrs[j] == "width" && attrs[j + 1] == "1")
              || (attrs[j] == "height" && attrs[j + 1] == "1" && elementName != "hr")
            ) {
              return@allowElements null
            }
          }

          // let the browser/web view decide how big should elements be, based on their content
          var i = 0
          while (i < attrs.size) {
            if (
              (attrs[i] == "width" && attrs[i + 1] != "1" && elementName != "img")
              || (attrs[i] == "height" && attrs[i + 1] != "1" && elementName != "img")
            ) {
              attrs.removeAt(i)
              attrs.removeAt(i)
            } else {
              i += 2
            }
          }

          var newElementName = elementName
          if (elementName == "img") {
            val srcAttr = getAttribute(attrs, "src", "")
            val altAttr = getAttribute(attrs, "alt")
            when {
              srcAttr.startsWith("data:") == true -> {
                attrs.clear()
                attrs.add("src")
                attrs.add(srcAttr)
                if (altAttr != null) {
                  attrs.add("alt")
                  attrs.add(altAttr)
                }
              }

              (srcAttr.startsWith("http://") == true
                  || srcAttr.startsWith("https://") == true) -> {
                // Orignal typecript:
                // return { tagName: 'a', attribs: { href: String(attribs.src), target: "_blank" },
                //   text: imgContentReplaceable };
                // Github: https://github.com/OWASP/java-html-sanitizer/issues/230
                // SO: https://stackoverflow.com/questions/67976114
                // There is no way to achieve this with OWASP sanitizer, so we do it with jsoup
                // as post-processing step
                remoteContentReplacedWithLink = true
                newElementName = "a"
                attrs.clear()
                attrs.add("href")
                attrs.add(srcAttr)
                attrs.add("target")
                attrs.add("_blank")
                attrs.add(FC_INNER_TEXT_TYPE_ATTR)
                attrs.add("1")
              }

              else -> {
                newElementName = "a"
                val titleAttr = getAttribute(attrs, "title")
                attrs.clear()
                if (altAttr != null) {
                  attrs.add("alt")
                  attrs.add(altAttr)
                }
                if (titleAttr != null) {
                  attrs.add("title")
                  attrs.add(titleAttr)
                }
                attrs.add(FC_INNER_TEXT_TYPE_ATTR)
                attrs.add("2")
              }
            }
            attrs.add(FC_FROM_IMAGE_ATTR)
            attrs.add(true.toString())
          }

          return@allowElements newElementName
        },
        *ALLOWED_ELEMENTS
      )
      .allowUrlProtocols(*ALLOWED_PROTOCOLS)
      .allowAttributesOnElementsExt(ALLOWED_ATTRS)
      .toFactory()

    val cleanHtml1 = policyFactory.sanitize(dirtyHtml)
    val document = Jsoup.parse(cleanHtml1)
    document.outputSettings().prettyPrint(false)

    moveElementsOutOfAnchorTag(document)

    for (element in document.select("a")) {
      if (element.hasAttr(FC_INNER_TEXT_TYPE_ATTR)) {
        val innerTextType = element.attr(FC_INNER_TEXT_TYPE_ATTR)
        element.attributes().remove(FC_INNER_TEXT_TYPE_ATTR)
        var innerText: String? = null
        when (innerTextType) {
          "1" -> innerText = imgContentReplaceable
          "2" -> innerText = "[image]"
        }
        if (innerText != null) element.html(innerText)
      }
    }
    var cleanHtml2 = document.outerHtml()

    if (remoteContentReplacedWithLink) {
      cleanHtml2 = HTML_POLICY_WITH_BASIC_TAGS_ONLY_FACTORY.sanitize(
        "<font size=\"-1\" color=\"#31a217\" face=\"monospace\">[remote content blocked " +
            "for your privacy]</font><br /><br />$cleanHtml2"
      )
    }

    return cleanHtml2.replace(
      imgContentReplaceable,
      "<font color=\"#D14836\" face=\"monospace\">[img]</font>"
    )
  }

  fun sanitizeHtmlStripAllTags(dirtyHtml: String?, outputNl: String = "\n"): String? {
    val html = sanitizeHtmlKeepBasicTags(dirtyHtml) ?: return null
    val randomSuffix = generateRandomSuffix()
    val br = "CU_BR_$randomSuffix"
    val blockStart = "CU_BS_$randomSuffix"
    val blockEnd = "CU_BE_$randomSuffix"

    var text = html.replace(HTML_BR_REGEX, br)
      .replace("\n", "")
      .replace(BLOCK_END_REGEX, blockEnd)
      .replace(BLOCK_START_REGEX, blockStart)
      .replace(Regex("($blockStart)+"), blockStart)
      .replace(Regex("($blockEnd)+"), blockEnd)

    val policyFactory = HtmlPolicyBuilder()
      .allowUrlProtocols(*ALLOWED_PROTOCOLS)
      .allowElements(
        { elementName, attrs ->
          when (elementName) {
            "img" -> {
              var innerText = "no name"
              val alt = getAttribute(attrs, "alt")
              if (alt != null) {
                innerText = alt
              } else {
                val title = getAttribute(attrs, "title")
                if (title != null) innerText = title
              }
              attrs.clear()
              attrs.add(FC_INNER_TEXT_TYPE_ATTR)
              attrs.add(innerText)
              return@allowElements "span"
            }
            "a" -> {
              val fromImage = getAttribute(attrs, FC_FROM_IMAGE_ATTR)
              if (fromImage == true.toString()) {
                var innerText = "[image]"
                val alt = getAttribute(attrs, "alt")
                if (alt != null) {
                  innerText = "[image: $alt]"
                }
                attrs.clear()
                attrs.add(FC_INNER_TEXT_TYPE_ATTR)
                attrs.add(innerText)
                return@allowElements "span"
              } else {
                return@allowElements elementName
              }
            }
            else -> return@allowElements elementName
          }
        },
        "img",
        "span",
        "a"
      )
      .allowAttributes("src", "alt", "title").onElements("img")
      .allowAttributes(FC_INNER_TEXT_TYPE_ATTR).onElements("span")
      .allowAttributes("src", "alt", "title", FC_FROM_IMAGE_ATTR).onElements("a")
      .toFactory()

    text = policyFactory.sanitize(text)
    val doc = Jsoup.parse(text)
    doc.outputSettings().prettyPrint(false)
    for (element in doc.select("span")) {
      if (element.hasAttr(FC_INNER_TEXT_TYPE_ATTR)) {
        val innerText = element.attr(FC_INNER_TEXT_TYPE_ATTR)
        element.attributes().remove(FC_INNER_TEXT_TYPE_ATTR)
        element.html(innerText)
      }
    }

    text = HtmlPolicyBuilder().toFactory().sanitize(doc.outerHtml())
    text = text.split(br + blockEnd + blockStart)
      .joinToString(br)
      .split(blockEnd + blockStart)
      .joinToString(br)
      .split(br + blockEnd)
      .joinToString(br)
      .split(br)
      .joinToString("\n")
      .split(blockStart)
      .filter { it != "" }
      .joinToString("\n")
      .split(blockEnd)
      .filter { it != "" }
      .joinToString("\n")
      .replace(MULTI_NEW_LINE_REGEX, "\n\n")

    if (outputNl != "\n") text = text.replace("\n", outputNl)
    return text
  }

  fun extractFcAttachments(decryptedContent: String, blocks: MutableList<MsgBlock>): String {
    // these tags were created by FlowCrypt exclusively, so the structure is fairly rigid
    // `<a href="${att.url}" class="cryptup_file" cryptup-data="${fcData}">${linkText}</a>\n`
    // thus we can use Regex
    if (!decryptedContent.contains("class=\"cryptup_file\"")) return decryptedContent
    var i = 0
    val result = java.lang.StringBuilder()
    for (match in FC_ATT_REGEX.findAll(decryptedContent)) {
      if (match.range.first > i) {
        result.append(decryptedContent.substring(i, match.range.first))
        i = match.range.last + 1
      }
      val url = match.groups[1]!!.value
      val attr = match.groups[2]!!.value.decodeFcHtmlAttr()
      if (isFcAttachmentLinkData(attr)) {
        attr!!.put("url", url)
        blocks.add(EncryptedAttLinkMsgBlock(AttMeta(attr)))
      }
    }
    if (i < decryptedContent.length) result.append(decryptedContent.substring(i))
    return result.toString()
  }

  fun isFcAttachmentLinkData(obj: JSONObject?): Boolean {
    return obj != null && obj.has("name") && obj.has("size") && obj.has("type")
  }

  fun stripFcReplyToken(decryptedContent: String): String {
    return decryptedContent.replace(FC_REPLY_TOKEN_REGEX, "")
  }

  fun stripPublicKeys(decryptedContent: String, foundPublicKeys: MutableList<String>): String {
    val normalizedTextAndBlocks = MsgBlockParser.detectBlocks(decryptedContent)
    var result = normalizedTextAndBlocks.normalized
    for (block in normalizedTextAndBlocks.blocks) {
      if (block.type == MsgBlock.Type.PUBLIC_KEY && block.content != null) {
        val content = block.content!!
        foundPublicKeys.add(content)
        result = result.replace(content, "")
      }
    }
    return result
  }

  fun extractMsgBlocks(mimeContent: ExtractedMimeContent): MutableList<MsgBlock> {
    val blocks = mutableListOf<MsgBlock>()
    blocks.addAll(extractMsgBlocksFromText(mimeContent))

    var signature: String? = mimeContent.signature?.let { String(it) }
    for (att in mimeContent.attachments) {
      var content = att.content
      when (treatAs(att)) {
        TreatAs.HIDDEN -> {
          // ignore
        }

        TreatAs.ENCRYPTED_MSG -> {
          if (content is InputStream) content = content.readText(StandardCharsets.US_ASCII)
          if (content is String) {
            PgpArmor.clip(content)?.let {
              blocks.add(MsgBlockFactory.fromContent(MsgBlock.Type.ENCRYPTED_MSG, it))
            }
          }
        }

        TreatAs.SIGNATURE -> {
          if (content is InputStream) content = content.readText(StandardCharsets.US_ASCII)
          if (content is String && signature == null) signature = content
        }

        TreatAs.PUBLIC_KEY -> {
          if (content is InputStream) content = content.readText(StandardCharsets.US_ASCII)
          if (content is String) blocks.addAll(MsgBlockParser.detectBlocks(content).blocks)
        }

        TreatAs.PRIVATE_KEY -> {
          if (content is InputStream) content = content.readText(StandardCharsets.US_ASCII)
          if (content is String) blocks.addAll(MsgBlockParser.detectBlocks(content).blocks)
        }

        TreatAs.ENCRYPTED_FILE -> {
          blocks.add(MsgBlockFactory.fromAttachment(MsgBlock.Type.ENCRYPTED_ATT, att))
        }

        TreatAs.PLAIN_FILE -> {
          blocks.add(MsgBlockFactory.fromAttachment(MsgBlock.Type.PLAIN_ATT, att))
        }
      }
    }

    if (signature != null) fixSignedBlocks(blocks, signature)

    return blocks
  }

  private fun extractMsgBlocksFromText(mimeContent: ExtractedMimeContent): MutableList<MsgBlock> {
    val blocks = mutableListOf<MsgBlock>()
    if (mimeContent.text != null) {
      val blocksFromText = MsgBlockParser.detectBlocks(mimeContent.text).blocks
      val suitableBlock = blocksFromText.firstOrNull {
        it.type in MsgBlock.Type.WELL_KNOWN_BLOCK_TYPES
      }

      when {
        suitableBlock != null -> {
          // if there are some encryption-related blocks found in the text section,
          // which we can use, and not look at the html section, because the html most likely
          // contains the same thing, just harder to parse pgp sections cause it's html
          blocks.addAll(blocksFromText)
        }

        mimeContent.html != null -> {
          // if no pgp blocks found in text part and there is html part, prefer html
          blocks.add(MsgBlockFactory.fromContent(MsgBlock.Type.PLAIN_HTML, mimeContent.html))
        }

        else -> {
          // else if no html and just a plain text message, use that
          blocks.addAll(blocksFromText)
        }
      }
    } else if (mimeContent.html != null) {
      blocks.add(MsgBlockFactory.fromContent(MsgBlock.Type.PLAIN_HTML, mimeContent.html))
    }

    return blocks
  }

  private fun fixSignedBlocks(blocks: MutableList<MsgBlock>, signature: String) {
    for (i in 0 until blocks.size) {
      val block = blocks[i]
      when (block.type) {
        MsgBlock.Type.PLAIN_TEXT -> {
          blocks[i] = MsgBlockFactory.fromContent(
            type = MsgBlock.Type.SIGNED_TEXT,
            content = block.content,
            missingEnd = !block.complete,
            signature = signature
          )
        }
        MsgBlock.Type.PLAIN_HTML -> {
          blocks[i] = MsgBlockFactory.fromContent(
            type = MsgBlock.Type.SIGNED_HTML,
            content = block.content,
            missingEnd = !block.complete,
            signature = signature
          )
        }
        else -> {
        }
      }
    }
  }

  private fun checkForPublicKeyBlock(stream: InputStream): Boolean {
    val a = ByteArray(100)
    val r = stream.read(a)
    if (r < 1) return false
    val s = String(if (r == a.size) a else a.copyOf(r), StandardCharsets.US_ASCII)
    return s.contains("-----BEGIN PGP PUBLIC KEY BLOCK-----")
  }

  private fun processExtractedMsgBlocks(
    msgBlocks: List<MsgBlock>,
    pgpPublicKeyRingCollection: PGPPublicKeyRingCollection,
    pgpSecretKeyRingCollection: PGPSecretKeyRingCollection,
    protector: SecretKeyRingProtector
  ): ProcessedMimeMessageResult {
    val sequentialProcessedBlocks = handleExtractedMsgBlocks(
      msgBlocks = msgBlocks,
      pgpPublicKeyRingCollection = pgpPublicKeyRingCollection,
      pgpSecretKeyRingCollection = pgpSecretKeyRingCollection,
      protector = protector
    )

    var isEncrypted = false
    val contentBlocks = mutableListOf<MsgBlock>()
    val resultBlocks = mutableListOf<MsgBlock>()

    var hasMixedSignatures = false
    var hasUnverifiedSignatures = false
    var signedBlockCount = 0
    var isPartialSigned = false
    val verifiedSignatures = mutableMapOf<SubkeyIdentifier, PGPSignature>()

    for (block in sequentialProcessedBlocks) {
      // We don't need Base64 correction here, fromAttachment() does this for us
      // We also seem to don't need to make correction between raw and utf8
      // But I'd prefer MsgBlock.content to be ByteArray
      // So, at least meanwhile, not porting this:
      // block.content = isContentBlock(block.type)
      //     ? block.content.toUtfStr() : block.content.toRawBytesStr();
      if (block is DecryptedAndOrSignedContentMsgBlock) {
        if (!isEncrypted) {
          isEncrypted = block.openPgpMetadata?.isEncrypted ?: false
        }

        if (block.openPgpMetadata?.isSigned == true) {
          signedBlockCount++
          block.openPgpMetadata?.let { openPgpMetadata ->
            if (openPgpMetadata.invalidInbandSignatures.isNotEmpty()
              || openPgpMetadata.invalidDetachedSignatures.isNotEmpty()
            ) {
              hasUnverifiedSignatures = true
            }
            if (verifiedSignatures.isEmpty()) {
              verifiedSignatures.putAll(openPgpMetadata.verifiedSignatures)
            } else {
              if (verifiedSignatures.keys.map { it.keyId } != openPgpMetadata.verifiedSignatures.keys.map { it.keyId }) {
                hasMixedSignatures = true
                //todo-denbond7 need to check it
                verifiedSignatures.putAll(openPgpMetadata.verifiedSignatures)
              }
            }
          }
        }

        for (innerBlock in block.blocks) {
          if (canBeAddedToCombinedContent(innerBlock)) {
            contentBlocks.add(innerBlock)
          } else {
            resultBlocks.add(innerBlock)
          }
        }
      }

      if (canBeAddedToCombinedContent(block)) {
        contentBlocks.add(block)
      } else {
        resultBlocks.add(block)
      }
    }

    val fmtRes = prepareFormattedContentBlock(contentBlocks)
    resultBlocks.add(0, fmtRes.contentBlock)

    if (signedBlockCount > 0 && signedBlockCount != sequentialProcessedBlocks.size) {
      isPartialSigned = true
    }

    return ProcessedMimeMessageResult(
      text = fmtRes.text,
      blocks = resultBlocks,
      verificationResult = VerificationResult(
        isEncrypted = isEncrypted,
        isSigned = signedBlockCount > 0,
        hasMixedSignatures = hasMixedSignatures,
        isPartialSigned = isPartialSigned,
        hasUnverifiedSignatures = hasUnverifiedSignatures
      )
    )
  }

  private fun canBeAddedToCombinedContent(block: MsgBlock): Boolean {
    return when {
      block.type.isContentBlockType() || MimeUtils.isPlainImgAtt(block) -> {
        block.error == null
      }

      else -> false
    }
  }

  private fun handleExtractedMsgBlocks(
    msgBlocks: List<MsgBlock>,
    pgpPublicKeyRingCollection: PGPPublicKeyRingCollection,
    pgpSecretKeyRingCollection: PGPSecretKeyRingCollection,
    protector: SecretKeyRingProtector
  ): MutableList<MsgBlock> {
    val sequentialProcessedBlocks = mutableListOf<MsgBlock>()
    for (msgBlock in msgBlocks) {
      when {
        msgBlock is SignedMsgBlock -> {
          processSignedMsgBlock(msgBlock)?.let { sequentialProcessedBlocks.add(it) }
        }

        msgBlock.type == MsgBlock.Type.ENCRYPTED_MSG -> {
          val decryptedContentMsgBlock = processEncryptedMsgBlock(
            msgBlock = msgBlock,
            pgpPublicKeyRingCollection = pgpPublicKeyRingCollection,
            pgpSecretKeyRingCollection = pgpSecretKeyRingCollection,
            protector = protector
          )
          sequentialProcessedBlocks.add(decryptedContentMsgBlock)
        }

        msgBlock.type == MsgBlock.Type.ENCRYPTED_ATT
            && (msgBlock as EncryptedAttMsgBlock).attMeta.name != null
            && PUBLIC_KEY_REGEX_3.matches(msgBlock.attMeta.name ?: "") -> {
          sequentialProcessedBlocks.add(
            processPublicKeyMsgBlock(
              msgBlock = msgBlock,
              pgpPublicKeyRingCollection = pgpPublicKeyRingCollection,
              pgpSecretKeyRingCollection = pgpSecretKeyRingCollection,
              protector = protector
            )
          )
        }

        msgBlock.type == MsgBlock.Type.PUBLIC_KEY -> {
          msgBlock.content?.let { source ->
            try {
              val keyRings = PgpKey.parseAndNormalizeKeyRings(source)
              if (keyRings.isNotEmpty()) {
                sequentialProcessedBlocks.addAll(keyRings.map {
                  MsgBlockFactory.fromContent(MsgBlock.Type.PUBLIC_KEY, it.armor(null))
                })
              } else {
                sequentialProcessedBlocks.add(
                  PublicKeyMsgBlock(
                    content = source,
                    complete = false,
                    keyDetails = null,
                    error = MsgBlockError("empty KeyRing")
                  )
                )
              }
            } catch (ex: Exception) {
              ex.printStackTrace()
              sequentialProcessedBlocks.add(
                PublicKeyMsgBlock(
                  content = source,
                  complete = false,
                  keyDetails = null,
                  error = MsgBlockError(ex.javaClass.simpleName + ": " + ex.message)
                )
              )
            }
          }
        }

        else -> {
          sequentialProcessedBlocks.add(msgBlock)
        }
      }
    }
    return sequentialProcessedBlocks
  }

  private fun processPublicKeyMsgBlock(
    msgBlock: MsgBlock,
    pgpPublicKeyRingCollection: PGPPublicKeyRingCollection,
    pgpSecretKeyRingCollection: PGPSecretKeyRingCollection,
    protector: SecretKeyRingProtector
  ): MsgBlock {
    // encrypted public key attached
    val decryptionResult = PgpDecryptAndOrVerify.decryptAndOrVerifyWithResult(
      srcInputStream = msgBlock.content?.toInputStream()!!,
      pgpPublicKeyRingCollection = pgpPublicKeyRingCollection,
      pgpSecretKeyRingCollection = pgpSecretKeyRingCollection,
      protector = protector
    )
    return if (decryptionResult.content != null) {
      val content = decryptionResult.content.toString(StandardCharsets.UTF_8.displayName())
      MsgBlockFactory.fromContent(MsgBlock.Type.PUBLIC_KEY, content)
    } else {
      // will show as encrypted attachment
      msgBlock
    }
  }

  private fun processEncryptedMsgBlock(
    msgBlock: MsgBlock,
    pgpPublicKeyRingCollection: PGPPublicKeyRingCollection,
    pgpSecretKeyRingCollection: PGPSecretKeyRingCollection,
    protector: SecretKeyRingProtector
  ): DecryptedAndOrSignedContentMsgBlock {
    val decryptionResult = PgpDecryptAndOrVerify.decryptAndOrVerifyWithResult(
      srcInputStream = msgBlock.content?.toInputStream()!!,
      pgpPublicKeyRingCollection = pgpPublicKeyRingCollection,
      pgpSecretKeyRingCollection = pgpSecretKeyRingCollection,
      protector = protector
    )

    val blocks = mutableListOf<MsgBlock>()
    if (decryptionResult.exception == null) {
      if (decryptionResult.isEncrypted) {
        val decrypted = decryptionResult.content?.toByteArray()
        val formatted = MsgBlockParser.fmtDecryptedAsSanitizedHtmlBlocks(decrypted)
        //todo-denbond7 fix it //if (subject == null) subject = formatted.subject
        blocks.addAll(formatted.blocks)
      } else {
        // ------------------------------------------------------------------------------------
        // Comment from TS code:
        // ------------------------------------------------------------------------------------
        // treating as text, converting to html - what about plain signed html?
        // This could produce html tags although hopefully, that would, typically, result in
        // the `(msgBlock.type === 'signedMsg' || msgBlock.type === 'signedHtml')` block above
        // the only time I can imagine it screwing up down here is if it was a signed-only
        // message that was actually fully armored (text not visible) with a mime msg inside
        // ... -> in which case the user would I think see full mime content?
        // ------------------------------------------------------------------------------------
        blocks.add(
          MsgBlockFactory.fromContent(
            type = MsgBlock.Type.VERIFIED_MSG,
            content = decryptionResult.content?.toString(
              StandardCharsets.UTF_8.displayName()
            )?.toEscapedHtml(),
            signature = decryptionResult.signature
          )
        )
      }
    } else {
      if (PgpDecryptAndOrVerify.DecryptionErrorType.NO_MDC == decryptionResult.exception.decryptionErrorType) {
        val resultWithIgnoredMDCErrors = PgpDecryptAndOrVerify.decryptAndOrVerifyWithResult(
          srcInputStream = msgBlock.content?.toInputStream()!!,
          pgpPublicKeyRingCollection = pgpPublicKeyRingCollection,
          pgpSecretKeyRingCollection = pgpSecretKeyRingCollection,
          protector = protector,
          ignoreMdcErrors = true
        )
        val decryptErrorMsgBlock = if (resultWithIgnoredMDCErrors.exception == null) {
          DecryptErrorMsgBlock(
            content = String(resultWithIgnoredMDCErrors.content?.toByteArray() ?: byteArrayOf()),
            complete = true,
            decryptErr = decryptionResult.exception.toDecryptError()
          )
        } else {
          DecryptErrorMsgBlock(
            content = msgBlock.content,
            complete = true,
            decryptErr = decryptionResult.exception.toDecryptError()
          )
        }

        blocks.add(decryptErrorMsgBlock)
      } else {
        blocks.add(
          DecryptErrorMsgBlock(
            content = msgBlock.content,
            complete = true,
            decryptErr = decryptionResult.exception.toDecryptError()
          )
        )
      }
    }

    return DecryptedAndOrSignedContentMsgBlock(blocks = blocks).apply {
      openPgpMetadata = decryptionResult.openPgpMetadata
    }
  }

  private fun processSignedMsgBlock(msgBlock: SignedMsgBlock): MsgBlock? {
    return when {
      msgBlock.signature != null -> {
        when (msgBlock.type) {
          MsgBlock.Type.SIGNED_MSG -> {
            // skip verification for now
            MsgBlockFactory.fromContent(
              type = MsgBlock.Type.VERIFIED_MSG,
              content = msgBlock.content?.toEscapedHtml(),
              signature = msgBlock.signature
            )
          }

          MsgBlock.Type.SIGNED_HTML -> {
            // skip verification for now
            return MsgBlockFactory.fromContent(
              type = MsgBlock.Type.VERIFIED_MSG,
              content = sanitizeHtmlKeepBasicTags(msgBlock.content),
              signature = msgBlock.signature
            )
          }

          MsgBlock.Type.SIGNED_TEXT -> {
            // skip verification for now
            return MsgBlockFactory.fromContent(
              type = MsgBlock.Type.SIGNED_TEXT,
              content = msgBlock.content,
              signature = msgBlock.signature
            )
          }
          else -> null
        }
      }

      msgBlock.type == MsgBlock.Type.SIGNED_MSG -> {
        return try {
          val cleartext = PgpSignature.extractClearText(msgBlock.content, false)
          msgBlock.copy(content = cleartext)
        } catch (e: Exception) {
          msgBlock.copy(error = MsgBlockError("[" + e.javaClass.simpleName + "]: " + e.message))
        }
      }

      else -> null
    }
  }

  private fun prepareFormattedContentBlock(allContentBlocks: List<MsgBlock>):
      FormattedContentBlockResult {
    val inlineImagesByCid = mutableMapOf<String, MsgBlock>()
    val imagesAtTheBottom = mutableListOf<MsgBlock>()
    for (plainImageBlock in allContentBlocks.filter { MimeUtils.isPlainImgAtt(it) }) {
      var contentId = (plainImageBlock as AttMsgBlock).attMeta.contentId ?: ""
      if (contentId.isNotEmpty()) {
        contentId =
          contentId.replace(CID_CORRECTION_REGEX_1, "").replace(CID_CORRECTION_REGEX_2, "")
        inlineImagesByCid[contentId] = plainImageBlock
      } else {
        imagesAtTheBottom.add(plainImageBlock)
      }
    }

    val msgContentAsHtml = StringBuilder()
    val msgContentAsText = StringBuilder()
    for (block in allContentBlocks.filterNot { MimeUtils.isPlainImgAtt(it) }) {
      if (block.content != null) {
        val content = block.content!!
        when (block.type) {
          MsgBlock.Type.DECRYPTED_TEXT -> {
            val html = fmtMsgContentBlockAsHtml(content.toEscapedHtml(), FrameColor.GREEN)
            msgContentAsHtml.append(html)
            msgContentAsText.append(content).append('\n')
          }

          MsgBlock.Type.DECRYPTED_HTML -> {
            // Typescript comment: todo: add support for inline imgs? when included using cid
            var html = content.stripHtmlRootTags()
            html = fmtMsgContentBlockAsHtml(html, FrameColor.GREEN)
            msgContentAsHtml.append(html)
            msgContentAsText
              .append(sanitizeHtmlStripAllTags(content)?.unescapeHtml())
              .append('\n')
          }

          MsgBlock.Type.PLAIN_TEXT -> {
            val html = fmtMsgContentBlockAsHtml(content.toEscapedHtml(), FrameColor.PLAIN)
            msgContentAsHtml.append(html)
            msgContentAsText.append(content).append('\n')
          }

          MsgBlock.Type.PLAIN_HTML -> {
            val stripped = content.stripHtmlRootTags()
            val dirtyHtmlWithImgs = fillInlineHtmlImages(stripped, inlineImagesByCid)
            msgContentAsHtml.append(fmtMsgContentBlockAsHtml(dirtyHtmlWithImgs, FrameColor.PLAIN))
            val text = sanitizeHtmlStripAllTags(dirtyHtmlWithImgs)?.unescapeHtml()
            msgContentAsText.append(text).append('\n')
          }

          MsgBlock.Type.VERIFIED_MSG, MsgBlock.Type.SIGNED_MSG -> {
            msgContentAsHtml.append(fmtMsgContentBlockAsHtml(content, FrameColor.GRAY))
            msgContentAsText.append(sanitizeHtmlStripAllTags(content)).append('\n')
          }

          else -> {
            msgContentAsHtml.append(fmtMsgContentBlockAsHtml(content, FrameColor.PLAIN))
            msgContentAsText.append(content).append('\n')
          }
        }
      }
    }

    imagesAtTheBottom.addAll(inlineImagesByCid.values)
    for (inlineImg in imagesAtTheBottom) {
      inlineImg as AttMsgBlock
      val imageName = inlineImg.attMeta.name ?: "(unnamed image)"
      val imageLengthKb = inlineImg.attMeta.length / 1024
      val alt = "$imageName - $imageLengthKb Kb"
      val inlineImgTag = "<img src=\"data:${inlineImg.attMeta.type ?: ""};base64," +
          "${inlineImg.attMeta.data ?: ""}\" alt=\"${alt.escapeHtmlAttr()}\" />"
      msgContentAsHtml.append(fmtMsgContentBlockAsHtml(inlineImgTag, FrameColor.PLAIN))
      msgContentAsText.append("[image: ${alt}]\n")
    }

    return FormattedContentBlockResult(
      text = msgContentAsText.toString().trim(),
      contentBlock = MsgBlockFactory.fromContent(
        type = MsgBlock.Type.PLAIN_HTML,
        """
          <!DOCTYPE html>
          <html>
          <head>
              <meta name="viewport" content="width=device-width"/>
              <style>
                body { word-wrap: break-word; word-break: break-word; hyphens: auto; margin-left: 0px; padding-left: 0px; }
                body img { display: inline !important; height: auto !important; max-width: 95% !important; }
                body pre { white-space: pre-wrap !important; }
                body > div.MsgBlock > table { zoom: 75% } /* table layouts tend to overflow - eg emails from fb */
              </style>
          </head>
          <body>$msgContentAsHtml</body>
          </html>
        """.trimIndent()
      )
    )
  }

  /**
   * replace content of images: <img src="cid:16c7a8c3c6a8d4ab1e01">
   */
  private fun fillInlineHtmlImages(
    htmlContent: String,
    inlineImagesByCid: MutableMap<String, MsgBlock>
  ): String {
    val usedCids = mutableSetOf<String>()
    val result = StringBuilder()
    var startPos = 0
    while (true) {
      val match = IMG_SRC_WITH_CID_REGEX.find(htmlContent, startPos)
      if (match == null) {
        result.append(htmlContent.substring(startPos, htmlContent.length))
        break
      }
      if (match.range.first > startPos) {
        result.append(htmlContent.substring(startPos, match.range.first))
      }
      val cid = match.groupValues[0]
      val img = inlineImagesByCid[cid]
      if (img != null) {
        img as AttMsgBlock
        // Typescript comment:
        // in current usage, as used by `endpoints.ts`: `block.attMeta!.data`
        // actually contains base64 encoded data, not Uint8Array as the type claims
        result.append("src=\"data:${img.attMeta.type ?: ""};base64,${img.attMeta.data ?: ""}\"")
        // Typescript comment:
        // Delete to find out if any imgs were unused. Later we can add the unused ones
        // at the bottom (though as implemented will cause issues if the same cid is reused
        // in several places in html - which is theoretically valid - only first will get replaced)
        // Kotlin:
        // Collect used CIDs and delete later
        usedCids.add(cid)
      } else {
        result.append(htmlContent.substring(match.range))
      }
      startPos = match.range.last + 1
    }
    for (cid in usedCids) {
      inlineImagesByCid.remove(cid)
    }
    return result.toString()
  }

  private fun fmtMsgContentBlockAsHtml(dirtyContent: String?, frameColor: FrameColor): String {
    if (dirtyContent == null) return ""
    val sanitizedHtml = sanitizeHtmlKeepBasicTags(dirtyContent)
    return "<div class=\"MsgBlock ${frameColor}\" style=\"$GENERAL_CSS" +
        "${FRAME_CSS_MAP[frameColor]!!}\">${sanitizedHtml}</div><!-- next MsgBlock -->\n"
  }

  private fun getAttribute(
    attrs: List<String>,
    attrName: String
  ): String? {
    val srcAttrIndex = attrs.withIndex().indexOfFirst {
      it.index % 2 == 0 && it.value == attrName
    }
    return if (srcAttrIndex != -1) attrs[srcAttrIndex + 1] else null
  }

  @Suppress("SameParameterValue")
  private fun getAttribute(attrs: List<String>, attrName: String, defaultValue: String): String {
    return getAttribute(attrs, attrName) ?: defaultValue
  }

  private fun generateRandomSuffix(length: Int = 5): String {
    val rnd = Random(System.currentTimeMillis())
    var s = rnd.nextInt().toString(16)
    while (s.length < length) s += rnd.nextInt().toString(16)
    return s.substring(0, length)
  }

  private fun moveElementsOutOfAnchorTag(document: Document) {
    // IMPORTANT: Do not change belo while into for loop,
    // because document.childrenSize() may change
    var i = 0
    while (i < document.childrenSize()) {
      moveElementsOutOfAnchorTag(document.child(i++), document)
    }
  }

  private fun moveElementsOutOfAnchorTag(element: Element, parent: Element) {
    if (element.tag().normalName() == "a" && element.hasAttr(FC_INNER_TEXT_TYPE_ATTR)) {
      val children = element.children().map { it as Node }.toTypedArray()
      val n = element.childrenSize()
      var index = 0
      while (index < n && parent.child(index) !== element) ++index
      parent.insertChildren(index, *children)
      if (element.childNodeSize() > 0) {
        for (childNode in element.childNodes()) {
          if (childNode is TextNode) {
            parent.insertChildren(index++, childNode)
          }
        }
      }
    } else {
      // IMPORTANT: Do not change belo while into for loop,
      // because element.childrenSize() may change
      var i = 0
      while (i < element.childrenSize()) {
        moveElementsOutOfAnchorTag(element.child(i++), element)
      }
    }
  }

  data class ExtractedMimeContent(
    val attachments: List<MimePart>,
    var signature: ByteArray? = null,
    val html: String? = null,
    val text: String? = null
  ) {
    override fun equals(other: Any?): Boolean {
      if (this === other) return true
      if (javaClass != other?.javaClass) return false

      other as ExtractedMimeContent

      if (attachments != other.attachments) return false
      if (signature != null) {
        if (other.signature == null) return false
        if (!signature.contentEquals(other.signature)) return false
      } else if (other.signature != null) return false
      if (html != other.html) return false
      if (text != other.text) return false

      return true
    }

    override fun hashCode(): Int {
      var result = attachments.hashCode()
      result = 31 * result + (signature?.contentHashCode() ?: 0)
      result = 31 * result + (html?.hashCode() ?: 0)
      result = 31 * result + (text?.hashCode() ?: 0)
      return result
    }
  }

  data class ProcessedMimeMessageResult(
    val text: String,
    val blocks: List<MsgBlock>,
    val verificationResult: VerificationResult
  )

  enum class TreatAs {
    HIDDEN,
    ENCRYPTED_MSG,
    SIGNATURE,
    PUBLIC_KEY,
    PRIVATE_KEY,
    ENCRYPTED_FILE,
    PLAIN_FILE
  }

  private data class FormattedContentBlockResult(
    val text: String,
    val contentBlock: MsgBlock
  )

  private enum class FrameColor {
    GREEN,
    GRAY,
    RED,
    PLAIN
  }
}
