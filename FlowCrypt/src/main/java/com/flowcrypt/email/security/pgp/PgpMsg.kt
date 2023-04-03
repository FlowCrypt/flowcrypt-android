/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: Ivan Pizhenko,
 *               DenBond7
 */

package com.flowcrypt.email.security.pgp

import android.content.Context
import com.flowcrypt.email.api.retrofit.response.model.AttMeta
import com.flowcrypt.email.api.retrofit.response.model.AttMsgBlock
import com.flowcrypt.email.api.retrofit.response.model.DecryptErrorMsgBlock
import com.flowcrypt.email.api.retrofit.response.model.DecryptedAndOrSignedContentMsgBlock
import com.flowcrypt.email.api.retrofit.response.model.DecryptedAttMsgBlock
import com.flowcrypt.email.api.retrofit.response.model.EncryptedAttLinkMsgBlock
import com.flowcrypt.email.api.retrofit.response.model.GenericMsgBlock
import com.flowcrypt.email.api.retrofit.response.model.MsgBlock
import com.flowcrypt.email.api.retrofit.response.model.MsgBlockError
import com.flowcrypt.email.api.retrofit.response.model.MsgBlockFactory
import com.flowcrypt.email.api.retrofit.response.model.PlainAttMsgBlock
import com.flowcrypt.email.api.retrofit.response.model.PublicKeyMsgBlock
import com.flowcrypt.email.api.retrofit.response.model.SignedMsgBlock
import com.flowcrypt.email.api.retrofit.response.model.VerificationResult
import com.flowcrypt.email.core.msg.MimeUtils
import com.flowcrypt.email.core.msg.RawBlockParser
import com.flowcrypt.email.database.FlowCryptRoomDatabase
import com.flowcrypt.email.extensions.jakarta.mail.hasPgpMessage
import com.flowcrypt.email.extensions.jakarta.mail.isMultipart
import com.flowcrypt.email.extensions.jakarta.mail.isMultipartAlternative
import com.flowcrypt.email.extensions.jakarta.mail.isOpenPGPMimeSigned
import com.flowcrypt.email.extensions.jakarta.mail.isPlainText
import com.flowcrypt.email.extensions.kotlin.decodeFcHtmlAttr
import com.flowcrypt.email.extensions.kotlin.escapeHtmlAttr
import com.flowcrypt.email.extensions.kotlin.stripHtmlRootTags
import com.flowcrypt.email.extensions.kotlin.toEscapedHtml
import com.flowcrypt.email.extensions.kotlin.unescapeHtml
import com.flowcrypt.email.extensions.org.owasp.html.allowAttributesOnElementsExt
import com.flowcrypt.email.security.KeysStorageImpl
import com.flowcrypt.email.util.GeneralUtil
import jakarta.mail.Multipart
import jakarta.mail.Part
import jakarta.mail.Session
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeMessage
import jakarta.mail.internet.MimePart
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
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.util.Properties
import kotlin.random.Random

object PgpMsg {
  private const val GENERAL_CSS =
    "padding-left: 8px;min-height: 50px;padding-top: 4px;padding-bottom: 4px;width: 100%;"
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
    val accountSecretKeys = PGPSecretKeyRingCollection(keysStorage.getPGPSecretKeyRings())
    val protector = keysStorage.getSecretKeyRingProtector()
    val msg = MimeMessage(Session.getInstance(Properties()), inputStream)

    val verificationPublicKeys = mutableListOf<PGPPublicKeyRing>()
    val pubKeyDao = FlowCryptRoomDatabase.getDatabase(context).pubKeyDao()

    for (address in msg.from) {
      if (address is InternetAddress) {
        val existingPubKeysInfo = pubKeyDao.getPublicKeysByRecipient(address.address.lowercase())
        for (existingPublicKeyEntity in existingPubKeysInfo) {
          verificationPublicKeys.addAll(
            //ask Tom about this place. Do we need to catch exception here or we can throw it
            PgpKey.parseKeys(source = existingPublicKeyEntity.publicKey)
              .pgpKeyRingCollection.pgpPublicKeyRingCollection
          )
        }
      }
    }

    return@withContext processMimeMessage(
      msg = msg,
      verificationPublicKeys = PGPPublicKeyRingCollection(verificationPublicKeys),
      secretKeys = accountSecretKeys,
      protector = protector
    )
  }

  fun processMimeMessage(
    inputStream: InputStream,
    secretKeys: PGPSecretKeyRingCollection,
    protector: SecretKeyRingProtector
  ): ProcessedMimeMessageResult {
    return processMimeMessage(
      msg = MimeMessage(Session.getInstance(Properties()), inputStream),
      secretKeys = secretKeys,
      protector = protector
    )
  }

  fun processMimeMessage(
    msg: MimeMessage,
    verificationPublicKeys: PGPPublicKeyRingCollection = PGPPublicKeyRingCollection(listOf()),
    secretKeys: PGPSecretKeyRingCollection,
    protector: SecretKeyRingProtector
  ): ProcessedMimeMessageResult {
    val extractedMsgBlocks = extractMsgBlocksFromPart(
      part = msg,
      verificationPublicKeys = verificationPublicKeys,
      secretKeys = secretKeys,
      protector = protector
    )
    return processExtractedMsgBlocks(extractedMsgBlocks)
  }

  fun extractMsgBlocksFromPart(
    part: Part,
    verificationPublicKeys: PGPPublicKeyRingCollection,
    secretKeys: PGPSecretKeyRingCollection,
    protector: SecretKeyRingProtector,
    isOpenPGPMimeSigned: Boolean = false
  ): Collection<MsgBlock> {
    val blocks = mutableListOf<MsgBlock>()
    when {
      //we found OpenPGP/MIME Signed message that should contains 2 parts.
      //See https://datatracker.ietf.org/doc/html/rfc3156#section-5 for more details
      part.isOpenPGPMimeSigned() -> {
        try {
          val multiPart = part.content as? Multipart
            ?: throw IllegalStateException("Wrong OpenPGP/MIME structure")
          if (multiPart.count != 2) throw IllegalStateException("Wrong OpenPGP/MIME structure")

          val contentPart = multiPart.getBodyPart(0)
          val signaturePart = multiPart.getBodyPart(1)
          val openPGPMIMESignedContent = ByteArrayOutputStream().apply {
            contentPart.writeTo(this)
          }.toByteArray()
          val signatureInputStream = signaturePart.inputStream

          val detachedSignatureVerificationResult = PgpSignature.verifyDetachedSignature(
            srcInputStream = openPGPMIMESignedContent.inputStream(),
            signatureInputStream = signatureInputStream,
            publicKeys = verificationPublicKeys
          )

          val decryptedAndOrSignedContentMsgBlock = DecryptedAndOrSignedContentMsgBlock(
            blocks = extractMsgBlocksFromPart(
              part = contentPart,
              verificationPublicKeys = verificationPublicKeys,
              secretKeys = secretKeys,
              protector = protector,
              isOpenPGPMimeSigned = true
            ).toList(),
            isOpenPGPMimeSigned = true
          ).apply {
            openPgpMetadata = detachedSignatureVerificationResult.openPgpMetadata
          }

          blocks.add(decryptedAndOrSignedContentMsgBlock)
        } catch (e: Exception) {
          if (GeneralUtil.isDebugBuild()) {
            e.printStackTrace()
          }
        }
      }

      //it's a multipart that should be investigated.
      part.isMultipart() -> {
        val multiPart = part.content as Multipart
        val isMultipartAlternative = part.isMultipartAlternative()
        var isAlternativePartUsed = false
        for (partCount in 0 until multiPart.count) {
          val subPart = multiPart.getBodyPart(partCount)
          if (!subPart.isMultipart() && isMultipartAlternative && multiPart.count > 1) {
            //if it multipart/alternative case with more than 1 part
            //we should handle only one part and skip other
            if (isAlternativePartUsed) {
              continue
            } else if (subPart.isPlainText()) {
              if (subPart.hasPgpMessage()) {
                isAlternativePartUsed = true
              } else {
                //we prefer to use HTML part if there are no PGP things
                continue
              }
            }
          }

          blocks.addAll(
            extractMsgBlocksFromPart(
              part = subPart,
              verificationPublicKeys = verificationPublicKeys,
              secretKeys = secretKeys,
              protector = protector,
              isOpenPGPMimeSigned = isOpenPGPMimeSigned
            )
          )
        }
      }

      //it's a part that should be handled
      else -> {
        try {
          val detectedRawBlocks = RawBlockParser.detectBlocks(part, isOpenPGPMimeSigned)
          val attachmentRawBlocks =
            detectedRawBlocks.filter { it.type == RawBlockParser.RawBlockType.ATTACHMENT }

          for (rawBlock in attachmentRawBlocks) {
            blocks.add(
              MsgBlockFactory.fromAttachment(
                type = MsgBlock.Type.PLAIN_ATT,
                attachment = part as MimePart,
                isOpenPGPMimeSigned = isOpenPGPMimeSigned
              )
            )
          }

          val msgBlocks = processRawBlocks(
            rawBlocks = detectedRawBlocks - attachmentRawBlocks,
            verificationPublicKeys = verificationPublicKeys,
            secretKeys = secretKeys,
            protector = protector
          )

          blocks.addAll(msgBlocks)
        } catch (e: Exception) {
          if (GeneralUtil.isDebugBuild()) {
            e.printStackTrace()
          }
          //here we should handle an error that relates to the current Part
          //and add a block to be able to show an error later
          blocks.add(
            GenericMsgBlock(
              MsgBlock.Type.UNKNOWN,
              null,
              MsgBlockError(e.javaClass.simpleName + ": " + e.message),
              isOpenPGPMimeSigned
            )
          )
        }
      }
    }

    return blocks
  }

  private fun processRawBlocks(
    rawBlocks: Collection<RawBlockParser.RawBlock>,
    verificationPublicKeys: PGPPublicKeyRingCollection,
    secretKeys: PGPSecretKeyRingCollection,
    protector: SecretKeyRingProtector
  ): Collection<MsgBlock> {
    val msgBlocks = mutableListOf<MsgBlock>()
    for (rawBlock in rawBlocks) {
      when (rawBlock.type) {
        RawBlockParser.RawBlockType.PGP_MSG -> {
          processPgpMsgRawBlock(
            rawBlock = rawBlock,
            verificationPublicKeys = verificationPublicKeys,
            secretKeys = secretKeys,
            protector = protector
          ).let {
            msgBlocks.add(it)
          }
        }

        RawBlockParser.RawBlockType.PLAIN_TEXT -> {
          msgBlocks.add(
            MsgBlockFactory.fromContent(
              type = MsgBlock.Type.PLAIN_TEXT,
              content = rawBlock.content.decodeToString(),
              isOpenPGPMimeSigned = rawBlock.isOpenPGPMimeSigned
            )
          )
        }

        RawBlockParser.RawBlockType.HTML_TEXT -> {
          msgBlocks.add(
            MsgBlockFactory.fromContent(
              type = MsgBlock.Type.PLAIN_HTML,
              content = rawBlock.content.decodeToString(),
              isOpenPGPMimeSigned = rawBlock.isOpenPGPMimeSigned
            )
          )
        }

        RawBlockParser.RawBlockType.PGP_CLEARSIGN_MSG -> {
          processClearSignedRawBlock(rawBlock, verificationPublicKeys)?.let {
            msgBlocks.add(it)
          }
        }

        RawBlockParser.RawBlockType.PGP_PUBLIC_KEY -> {
          processPgpPublicKeyRawBlock(rawBlock).let {
            msgBlocks.add(it)
          }
        }

        RawBlockParser.RawBlockType.PGP_PRIVATE_KEY -> {
          msgBlocks.add(
            GenericMsgBlock(
              type = MsgBlock.Type.PRIVATE_KEY,
              content = String(rawBlock.content),
              isOpenPGPMimeSigned = rawBlock.isOpenPGPMimeSigned
            )
          )
        }
        else -> {
          //skip for now
        }
      }
    }

    return msgBlocks
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
        blocks.add(EncryptedAttLinkMsgBlock(attMeta = AttMeta(attr), isOpenPGPMimeSigned = false))
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
    val normalizedTextAndBlocks = RawBlockParser.detectBlocks(decryptedContent)
    for (block in normalizedTextAndBlocks) {
      if (block.type == RawBlockParser.RawBlockType.PGP_PUBLIC_KEY && block.content.isNotEmpty()) {
        val content = block.content
        foundPublicKeys.add(String(content))
      }
    }
    return decryptedContent
  }

  private fun processExtractedMsgBlocks(msgBlocks: Collection<MsgBlock>): ProcessedMimeMessageResult {
    var isEncrypted = false
    val contentBlocks = mutableListOf<MsgBlock>()
    val resultBlocks = mutableListOf<MsgBlock>()

    var hasMixedSignatures = false
    var hasBadSignatures = false
    var signedBlockCount = 0
    var isPartialSigned = false
    val verifiedSignatures = mutableMapOf<SubkeyIdentifier, PGPSignature>()
    val keyIdOfSigningKeys = mutableListOf<Long>()

    for (block in msgBlocks) {
      // We don't need Base64 correction here, fromAttachment() does this for us
      // We also seem to don't need to make correction between raw and utf8
      // But I'd prefer MsgBlock.content to be ByteArray
      // So, at least meanwhile, not porting this:
      // block.content = isContentBlock(block.type)
      //     ? block.content.toUtfStr() : block.content.toRawBytesStr();
      if (block.type in MsgBlock.Type.SIGNED_BLOCK_TYPES) {
        val openPgpMetadata = when (block) {
          is DecryptedAndOrSignedContentMsgBlock -> {
            block.openPgpMetadata
          }

          is SignedMsgBlock -> {
            block.openPgpMetadata
          }

          else -> null
        }

        if (!isEncrypted) {
          isEncrypted = openPgpMetadata?.isEncrypted ?: false
        }

        if (openPgpMetadata?.isSigned == true) {
          signedBlockCount++

          if (openPgpMetadata.invalidInbandSignatures.isNotEmpty()
            || openPgpMetadata.invalidDetachedSignatures.isNotEmpty()
          ) {
            val invalidSignatureFailures = openPgpMetadata.invalidInbandSignatures +
                openPgpMetadata.invalidDetachedSignatures

            hasBadSignatures = invalidSignatureFailures.any {
              it.validationException.underlyingException != null
            }

            keyIdOfSigningKeys.addAll(invalidSignatureFailures.filter {
              it.validationException.message?.matches("Missing verification key.?".toRegex()) == true
            }.mapNotNull { it.signatureVerification.signature.keyID })
          }

          if (verifiedSignatures.isEmpty()) {
            verifiedSignatures.putAll(openPgpMetadata.verifiedSignatures)
          } else {
            val keyIdsOfAllVerifiedSignatures = verifiedSignatures.keys.map { it.keyId }
            val keyIdsOfCurrentVerifiedSignatures = openPgpMetadata.verifiedSignatures.keys.map {
              it.keyId
            }
            if (keyIdsOfAllVerifiedSignatures != keyIdsOfCurrentVerifiedSignatures) {
              hasMixedSignatures = true
              verifiedSignatures.putAll(openPgpMetadata.verifiedSignatures)
            }
          }
        }
      }

      when {
        block is DecryptedAndOrSignedContentMsgBlock -> {
          val innerBlocks = extractInnerBlocks(block)
          for (innerBlock in innerBlocks) {
            if (canBeAddedToCombinedContent(innerBlock)) {
              contentBlocks.add(innerBlock)
            } else {
              resultBlocks.add(innerBlock)
            }
          }
        }

        canBeAddedToCombinedContent(block) -> {
          contentBlocks.add(block)
        }

        else -> {
          resultBlocks.add(block)
        }
      }
    }

    val fmtRes = prepareFormattedContentBlock(contentBlocks)
    resultBlocks.add(0, fmtRes.contentBlock)

    if (signedBlockCount > 0 && signedBlockCount != msgBlocks.size) {
      isPartialSigned = true
    }

    return ProcessedMimeMessageResult(
      text = fmtRes.text,
      blocks = resultBlocks,
      verificationResult = VerificationResult(
        hasEncryptedParts = isEncrypted,
        hasSignedParts = signedBlockCount > 0,
        hasMixedSignatures = hasMixedSignatures,
        isPartialSigned = isPartialSigned,
        keyIdOfSigningKeys = keyIdOfSigningKeys,
        hasBadSignatures = hasBadSignatures
      )
    )
  }

  private fun extractInnerBlocks(
    decryptedAndOrSignedContentMsgBlock: DecryptedAndOrSignedContentMsgBlock
  ): Collection<MsgBlock> {
    val blocks = mutableListOf<MsgBlock>()
    for (block in decryptedAndOrSignedContentMsgBlock.blocks) {
      if (block is DecryptedAndOrSignedContentMsgBlock) {
        blocks.addAll(extractInnerBlocks(block))
      } else {
        blocks.add(block)
      }
    }

    return blocks
  }

  private fun canBeAddedToCombinedContent(block: MsgBlock): Boolean =
    (block.type.isContentBlockType() || MimeUtils.isPlainImgAtt(block)) && block.error == null

  private fun processPgpPublicKeyRawBlock(
    rawBlock: RawBlockParser.RawBlock
  ): MsgBlock {
    return if (rawBlock.content.isEmpty()) {
      PublicKeyMsgBlock(
        content = null,
        keyDetails = null,
        error = MsgBlockError("empty source"),
        isOpenPGPMimeSigned = rawBlock.isOpenPGPMimeSigned
      )
    } else {
      try {
        val keyDetails = PgpKey.parseKeys(rawBlock.content).pgpKeyDetailsList.firstOrNull()
        PublicKeyMsgBlock(
          content = rawBlock.content.decodeToString(),
          keyDetails = keyDetails,
          isOpenPGPMimeSigned = rawBlock.isOpenPGPMimeSigned
        )
      } catch (e: Exception) {
        e.printStackTrace()
        PublicKeyMsgBlock(
          content = rawBlock.content.decodeToString(),
          error = MsgBlockError("[" + e.javaClass.simpleName + "]: " + e.message),
          isOpenPGPMimeSigned = rawBlock.isOpenPGPMimeSigned
        )
      }
    }
  }

  private fun processPgpMsgRawBlock(
    rawBlock: RawBlockParser.RawBlock,
    verificationPublicKeys: PGPPublicKeyRingCollection,
    secretKeys: PGPSecretKeyRingCollection,
    protector: SecretKeyRingProtector
  ): DecryptedAndOrSignedContentMsgBlock {
    val decryptionResult = PgpDecryptAndOrVerify.decryptAndOrVerifyWithResult(
      srcInputStream = rawBlock.content.inputStream(),
      publicKeys = verificationPublicKeys,
      secretKeys = secretKeys,
      protector = protector,
      retryCount = 3
    )

    val blocks = mutableListOf<MsgBlock>()
    if (decryptionResult.exception == null) {
      if (decryptionResult.isEncrypted) {
        val decrypted = decryptionResult.content?.toByteArray()
        if (MimeUtils.resemblesMsg(decrypted)) {
          //it seems the decrypted text is MIME message.
          //We need to convert inner MsgBlock(s) to decrypted variants
          val innerMimeMessage =
            MimeMessage(Session.getDefaultInstance(Properties()), ByteArrayInputStream(decrypted))
          blocks.addAll(
            extractMsgBlocksFromPart(
              part = innerMimeMessage,
              verificationPublicKeys = verificationPublicKeys,
              secretKeys = secretKeys,
              protector = protector
            ).map {
              transformBlockToDecryptedVersion(it, rawBlock)
            }
          )
        } else {
          blocks.addAll(fmtDecryptedAsSanitizedHtmlBlocks(decrypted))
        }
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
            signature = decryptionResult.signature,
            isOpenPGPMimeSigned = rawBlock.isOpenPGPMimeSigned
          )
        )
      }
    } else {
      if (PgpDecryptAndOrVerify.DecryptionErrorType.NO_MDC == decryptionResult.exception.decryptionErrorType) {
        val resultWithIgnoredMDCErrors = PgpDecryptAndOrVerify.decryptAndOrVerifyWithResult(
          srcInputStream = rawBlock.content.inputStream(),
          publicKeys = verificationPublicKeys,
          secretKeys = secretKeys,
          protector = protector,
          ignoreMdcErrors = true,
          retryCount = 3
        )
        val decryptErrorMsgBlock = if (resultWithIgnoredMDCErrors.exception == null) {
          DecryptErrorMsgBlock(
            content = String(resultWithIgnoredMDCErrors.content?.toByteArray() ?: byteArrayOf()),
            decryptErr = decryptionResult.exception.toDecryptError(),
            isOpenPGPMimeSigned = rawBlock.isOpenPGPMimeSigned
          )
        } else {
          DecryptErrorMsgBlock(
            content = rawBlock.content.decodeToString(),
            decryptErr = decryptionResult.exception.toDecryptError(),
            isOpenPGPMimeSigned = rawBlock.isOpenPGPMimeSigned
          )
        }

        blocks.add(decryptErrorMsgBlock)
      } else {
        blocks.add(
          DecryptErrorMsgBlock(
            content = rawBlock.content.decodeToString(),
            decryptErr = decryptionResult.exception.toDecryptError(),
            isOpenPGPMimeSigned = rawBlock.isOpenPGPMimeSigned
          )
        )
      }
    }

    return DecryptedAndOrSignedContentMsgBlock(
      blocks = blocks,
      isOpenPGPMimeSigned = rawBlock.isOpenPGPMimeSigned
    ).apply {
      openPgpMetadata = decryptionResult.openPgpMetadata
    }
  }

  private fun transformBlockToDecryptedVersion(
    msgBlock: MsgBlock,
    rawBlock: RawBlockParser.RawBlock
  ): MsgBlock {
    return when (msgBlock) {
      is GenericMsgBlock -> {
        when (msgBlock.type) {
          MsgBlock.Type.PLAIN_HTML -> msgBlock.copy(type = MsgBlock.Type.DECRYPTED_HTML)
          MsgBlock.Type.PLAIN_TEXT -> msgBlock.copy(type = MsgBlock.Type.DECRYPTED_TEXT)
          else -> msgBlock
        }
      }

      is PlainAttMsgBlock -> {
        DecryptedAttMsgBlock(
          content = msgBlock.content,
          attMeta = msgBlock.attMeta,
          decryptErr = null,
          error = msgBlock.error,
          isOpenPGPMimeSigned = rawBlock.isOpenPGPMimeSigned
        )
      }

      is DecryptedAndOrSignedContentMsgBlock -> {
        msgBlock.copy(blocks = msgBlock.blocks.map { innerMsgBlock ->
          transformBlockToDecryptedVersion(
            innerMsgBlock,
            rawBlock
          )
        })
      }

      else -> msgBlock
    }
  }

  private fun processClearSignedRawBlock(
    rawBlock: RawBlockParser.RawBlock,
    verificationPublicKeys: PGPPublicKeyRingCollection
  ): MsgBlock? {
    return try {
      val clearTextVerificationResult = PgpSignature.verifyClearTextSignature(
        srcInputStream = requireNotNull(rawBlock.content.inputStream()),
        publicKeys = verificationPublicKeys
      )
      clearTextVerificationResult.exception?.let { throw it }
      SignedMsgBlock(
        content = clearTextVerificationResult.clearText,
        isOpenPGPMimeSigned = rawBlock.isOpenPGPMimeSigned
      ).apply { openPgpMetadata = clearTextVerificationResult.openPgpMetadata }
    } catch (e: Exception) {
      SignedMsgBlock(
        content = rawBlock.content.decodeToString(),
        error = MsgBlockError("[" + e.javaClass.simpleName + "]: " + e.message),
        isOpenPGPMimeSigned = rawBlock.isOpenPGPMimeSigned
      )
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
            val html = fmtMsgContentBlockAsHtml(
              content.toEscapedHtml(),
              if (block.isOpenPGPMimeSigned) FrameColor.GRAY else FrameColor.PLAIN
            )
            msgContentAsHtml.append(html)
            msgContentAsText.append(content).append('\n')
          }

          MsgBlock.Type.PLAIN_HTML -> {
            val stripped = content.stripHtmlRootTags()
            val dirtyHtmlWithImgs = fillInlineHtmlImages(stripped, inlineImagesByCid)
            msgContentAsHtml.append(
              fmtMsgContentBlockAsHtml(
                dirtyHtmlWithImgs,
                if (block.isOpenPGPMimeSigned) FrameColor.GRAY else FrameColor.PLAIN
              )
            )
            val text = sanitizeHtmlStripAllTags(dirtyHtmlWithImgs)?.unescapeHtml()
            msgContentAsText.append(text).append('\n')
          }

          MsgBlock.Type.VERIFIED_MSG, MsgBlock.Type.SIGNED_CONTENT -> {
            msgContentAsHtml.append(fmtMsgContentBlockAsHtml(content, FrameColor.GRAY))
            msgContentAsText.append(sanitizeHtmlStripAllTags(content)).append('\n')
          }

          else -> {
            msgContentAsHtml.append(
              fmtMsgContentBlockAsHtml(
                content,
                if (block.isOpenPGPMimeSigned) FrameColor.GRAY else FrameColor.PLAIN
              )
            )
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
        """.trimIndent(), isOpenPGPMimeSigned = false
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

  private fun fmtDecryptedAsSanitizedHtmlBlocks(decryptedContent: ByteArray?): Collection<MsgBlock> {
    if (decryptedContent == null) return emptyList()
    val blocks = mutableListOf<MsgBlock>()
    val armoredKeys = mutableListOf<String>()
    val content = stripPublicKeys(
      stripFcReplyToken(extractFcAttachments(String(decryptedContent), blocks)),
      armoredKeys
    ).toEscapedHtml()
    blocks.add(
      MsgBlockFactory.fromContent(
        MsgBlock.Type.DECRYPTED_HTML,
        content,
        isOpenPGPMimeSigned = false
      )
    )
    for (armoredKey in armoredKeys) {
      blocks.add(
        MsgBlockFactory.fromContent(
          MsgBlock.Type.PUBLIC_KEY, armoredKey,
          isOpenPGPMimeSigned = false
        )
      )
    }
    return blocks
  }

  data class ProcessedMimeMessageResult(
    val text: String,
    val blocks: List<MsgBlock>,
    val verificationResult: VerificationResult
  )

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
