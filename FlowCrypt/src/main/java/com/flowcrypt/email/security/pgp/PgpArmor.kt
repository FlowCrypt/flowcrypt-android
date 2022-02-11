/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors = Ivan Pizhenko
 */

package com.flowcrypt.email.security.pgp

import com.flowcrypt.email.BuildConfig
import com.flowcrypt.email.core.msg.RawBlockParser
import com.flowcrypt.email.extensions.kotlin.countOfMatchesZeroOneOrMore
import com.flowcrypt.email.extensions.kotlin.isWhiteSpace
import com.flowcrypt.email.extensions.kotlin.normalize
import org.bouncycastle.bcpg.ArmoredInputStream
import org.bouncycastle.bcpg.ArmoredOutputStream
import org.bouncycastle.openpgp.PGPException
import org.bouncycastle.util.Strings
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream

@Suppress("unused")
object PgpArmor {
  data class CryptoArmorStringHeaderDefinition(
    val begin: String,
    val middle: String? = null,
    val end: String,
    val replace: Boolean
  )

  data class CryptoArmorRegexHeaderDefinition(
    val beginRegexp: Regex,
    val middleRegexp: Regex? = null,
    val endRegexp: Regex,
    val replace: Boolean
  )

  @JvmStatic
  val ARMOR_HEADER_DICT: Map<RawBlockParser.RawBlockType, CryptoArmorStringHeaderDefinition> =
    mapOf(
      RawBlockParser.RawBlockType.UNKNOWN to CryptoArmorStringHeaderDefinition(
        begin = "-----BEGIN",
        end = "-----END",
        replace = false
      ),
      RawBlockParser.RawBlockType.PGP_PUBLIC_KEY to CryptoArmorStringHeaderDefinition(
        begin = "-----BEGIN PGP PUBLIC KEY BLOCK-----",
        end = "-----END PGP PUBLIC KEY BLOCK-----",
        replace = true
      ),
      RawBlockParser.RawBlockType.PGP_PRIVATE_KEY to CryptoArmorStringHeaderDefinition(
        begin = "-----BEGIN PGP PRIVATE KEY BLOCK-----",
        end = "-----END PGP PRIVATE KEY BLOCK-----",
        replace = true
      ),
      RawBlockParser.RawBlockType.CERTIFICATE to CryptoArmorStringHeaderDefinition(
        begin = "-----BEGIN CERTIFICATE-----",
        end = "-----END CERTIFICATE-----",
        replace = true
      ),
      RawBlockParser.RawBlockType.PGP_CLEARSIGN_MSG to CryptoArmorStringHeaderDefinition(
        begin = "-----BEGIN PGP SIGNED MESSAGE-----",
        middle = "-----BEGIN PGP SIGNATURE-----",
        end = "-----END PGP SIGNATURE-----",
        replace = true
      ),
      RawBlockParser.RawBlockType.SIGNATURE to CryptoArmorStringHeaderDefinition(
        begin = "-----BEGIN PGP SIGNATURE-----",
        end = "-----END PGP SIGNATURE-----",
        replace = false
      ),
      RawBlockParser.RawBlockType.PGP_MSG to CryptoArmorStringHeaderDefinition(
        begin = "-----BEGIN PGP MESSAGE-----",
        end = "-----END PGP MESSAGE-----",
        replace = true
      )
  )

  @JvmStatic
  val ARMOR_HEADER_DICT_REGEX = ARMOR_HEADER_DICT.mapValues {
    val v = it.value
    CryptoArmorRegexHeaderDefinition(
      beginRegexp = Regex(v.begin.replace(" ", "\\s")),
      middleRegexp = if (v.middle != null) Regex(v.middle.replace(" ", "\\s")) else null,
      endRegexp = Regex(v.end.replace(" ", "\\s")),
      replace = v.replace
    )
  }

  @JvmStatic
  val ARMOR_HEADER_UNKNOWN = ARMOR_HEADER_DICT[RawBlockParser.RawBlockType.UNKNOWN]

  @JvmStatic
  val FLOWCRYPT_HEADERS = listOf(
    Pair(ArmoredOutputStream.VERSION_HDR, "FlowCrypt ${BuildConfig.VERSION_NAME} Gmail Encryption"),
    Pair("Comment", "Seamlessly send and receive encrypted email")
  )

  // note: using RawBlockParser.RawBlockType.UNKNOWN instead of "key" in Typescript
  @JvmStatic
  fun normalize(armored: String, blockType: RawBlockParser.RawBlockType): String {
    /*if (blockType != RawBlockParser.RawBlockType.UNKNOWN
      && !RawBlockParser.RawBlockType.REPLACEABLE_BLOCK_TYPES.contains(blockType)
    ) {
      throw IllegalArgumentException("Can't normalize block of type '$blockType'")
    }*/

    var result = armored.normalize()

    if (normalizeBlockTypeList1.indexOf(blockType) > -1) {
      result = result.replace("\r\n", "\n").trim()
      val nl2 = result.countOfMatchesZeroOneOrMore("\n\n")
      val nl3 = result.countOfMatchesZeroOneOrMore("\n\n\n")
      val nl4 = result.countOfMatchesZeroOneOrMore("\n\n\n\n")
      val nl6 = result.countOfMatchesZeroOneOrMore("\n\n\n\n\n\n")
      if (nl3 > 1 && nl6 == 1) {
        // newlines tripled: fix
        result = result.replace("\n\n\n", "\n")
      } else if (nl2 > 1 && nl4 == 1) {
        // newlines doubled. GPA on windows does this, and sometimes message
        // can get extracted this way from HTML
        result = result.replace("\n\n", "\n")
      }
    }

    // check for and fix missing a mandatory empty line
    val lines = result.split('\n')
    val h = ARMOR_HEADER_DICT[blockType]!!
    if (lines.size > 5 && lines[0].indexOf(h.begin) > -1
      && lines[lines.size - 1].indexOf(h.end) > -1 && lines.indexOf("") == -1
    ) {
      for (i in 1..5) {
        // skip comment lines, looking for the first data line
        if (normalizeRegex1.containsMatchIn(lines[i])) continue
        if (normalizeRegex2.containsMatchIn(lines[i])) {
          // insert empty line before the first data line
          val mutableLines = lines.toMutableList()
          mutableLines.add(i, "")
          result = mutableLines.joinToString("\n")
          break
        }
        break // don't do anything if the format is not as expected
      }
    }

    return result
  }

  @JvmStatic
  private val normalizeBlockTypeList1 = arrayOf(
    RawBlockParser.RawBlockType.PGP_PUBLIC_KEY,
    RawBlockParser.RawBlockType.PGP_PRIVATE_KEY,
    RawBlockParser.RawBlockType.PGP_MSG,
    RawBlockParser.RawBlockType.UNKNOWN
  )

  @JvmStatic
  private val normalizeRegex1 = Regex("^[a-zA-Z0-9\\-_. ]+: .+\$")

  @JvmStatic
  private val normalizeRegex2 = Regex("^[a-zA-Z0-9/+]{32,77}\$")

  @Suppress("ArrayInDataClass")
  data class CleartextSignedMessage(
    val content: ByteArrayOutputStream,
    val signature: String?
  )

  // Based on this example:
  // https://github.com/bcgit/bc-java/blob/bc3b92f1f0e78b82e2584c5fb4b226a13e7f8b3b/pg/src/main/java/org/bouncycastle/openpgp/examples/ClearSignedFileProcessor.java
  @JvmStatic
  fun readSignedClearTextMessage(input: InputStream): CleartextSignedMessage {
    try {
      ArmoredInputStream(input).use { armoredInput ->
        val out = ByteArrayOutputStream()
        out.use {
          val lineOut = ByteArrayOutputStream()
          var lookAhead = readInputLine(lineOut, armoredInput)
          if (lookAhead != -1 && armoredInput.isClearText) {
            var line = lineOut.toByteArray()
            out.write(line, 0, getLengthWithoutSeparatorOrTrailingWhitespace(line))
            out.write(lineSeparatorBytes)
            while (lookAhead != -1 && armoredInput.isClearText) {
              lookAhead = readInputLine(lineOut, lookAhead, armoredInput)
              line = lineOut.toByteArray()
              out.write(line, 0, getLengthWithoutSeparatorOrTrailingWhitespace(line))
              out.write(lineSeparatorBytes)
            }
          } else {
            // a single line file
            if (lookAhead != -1) {
              val line = lineOut.toByteArray()
              out.write(line, 0, getLengthWithoutSeparatorOrTrailingWhitespace(line))
              out.write(lineSeparatorBytes)
            }
          }
        }
        return CleartextSignedMessage(out, null)
      }
    } catch (ex: PGPException) {
      throw PGPException("Cleartext format error", ex)
    } catch (ex: IOException) {
      throw PGPException("Cleartext format error", ex)
    }
  }

  @Throws(IOException::class)
  @JvmStatic
  private fun readInputLine(output: ByteArrayOutputStream, input: InputStream): Int {
    output.reset()
    var lookAhead = -1
    var ch: Int
    while (input.read().also { ch = it } >= 0) {
      output.write(ch)
      if (ch == '\r'.code || ch == '\n'.code) {
        lookAhead = readPassedEOL(output, ch, input)
        break
      }
    }
    return lookAhead
  }

  @Throws(IOException::class)
  @JvmStatic
  private fun readInputLine(
    output: ByteArrayOutputStream,
    initialLookAhead: Int,
    input: InputStream
  ): Int {
    var lookAhead = initialLookAhead
    output.reset()
    var ch = lookAhead
    do {
      output.write(ch)
      if (ch == '\r'.code || ch == '\n'.code) {
        lookAhead = readPassedEOL(output, ch, input)
        break
      }
    } while (input.read().also { ch = it } >= 0)
    if (ch < 0) {
      lookAhead = -1
    }
    return lookAhead
  }

  @Throws(IOException::class)
  @JvmStatic
  private fun readPassedEOL(output: ByteArrayOutputStream, lastCh: Int, input: InputStream): Int {
    var lookAhead: Int = input.read()
    if (lastCh == '\r'.code && lookAhead == '\n'.code) {
      output.write(lookAhead)
      lookAhead = input.read()
    }
    return lookAhead
  }

  @JvmStatic
  private fun getLengthWithoutSeparatorOrTrailingWhitespace(line: ByteArray): Int {
    var end = line.size - 1
    while (end >= 0 && line[end].isWhiteSpace) {
      end--
    }
    return end + 1
  }

  private val lineSeparatorBytes = Strings.lineSeparator().toByteArray()

  @JvmStatic
  fun clip(text: String): String? {
    val unknown = ARMOR_HEADER_DICT[RawBlockParser.RawBlockType.UNKNOWN]!!
    if (text.contains(unknown.begin) && text.contains(unknown.end)) {
      val match = blockRegex.find(text)
      if (match != null) return text.substring(match.range)
    }
    return null
  }

  private val blockRegex = Regex(
    "(-----BEGIN PGP (MESSAGE|SIGNED MESSAGE|SIGNATURE|PUBLIC KEY BLOCK)-----[\\s\\S]+" +
        "-----END PGP (MESSAGE|SIGNATURE|PUBLIC KEY BLOCK)-----)"
  )
}
