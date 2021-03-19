/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors = Ivan Pizhenko
 */

package com.flowcrypt.email.security.pgp

import com.flowcrypt.email.BuildConfig
import com.flowcrypt.email.api.retrofit.response.model.node.MsgBlock
import com.flowcrypt.email.extensions.lang.countOfMatchesZeroOneOrMore
import com.flowcrypt.email.extensions.lang.normalize
import java.lang.IllegalArgumentException
import org.bouncycastle.bcpg.ArmoredOutputStream

@Suppress("unused")
object PgpArmor {
  data class CryptoArmorStringHeaderDefinition (
      val begin: String,
      val middle: String? = null,
      val end: String,
      val replace: Boolean
  )

  data class CryptoArmorRegexHeaderDefinition (
      val beginRegexp: Regex,
      val middleRegexp: Regex? = null,
      val endRegexp: Regex,
      val replace: Boolean
  )

  @JvmStatic
  val ARMOR_HEADER_DICT: Map<MsgBlock.Type, CryptoArmorStringHeaderDefinition> = mapOf(
      MsgBlock.Type.UNKNOWN to CryptoArmorStringHeaderDefinition(
          begin = "-----BEGIN",
          end = "-----END",
          replace = false
      ),
      MsgBlock.Type.PUBLIC_KEY to CryptoArmorStringHeaderDefinition(
          begin = "-----BEGIN PGP PUBLIC KEY BLOCK-----",
          end = "-----END PGP PUBLIC KEY BLOCK-----",
          replace = true
      ),
      MsgBlock.Type.PRIVATE_KEY to CryptoArmorStringHeaderDefinition(
          begin = "-----BEGIN PGP PRIVATE KEY BLOCK-----",
          end = "-----END PGP PRIVATE KEY BLOCK-----",
          replace = true
      ),
      MsgBlock.Type.CERTIFICATE to CryptoArmorStringHeaderDefinition(
          begin = "-----BEGIN CERTIFICATE-----",
          end = "-----END CERTIFICATE-----",
          replace = true
      ),
      MsgBlock.Type.SIGNED_MSG to CryptoArmorStringHeaderDefinition(
          begin = "-----BEGIN PGP SIGNED MESSAGE-----",
          middle = "-----BEGIN PGP SIGNATURE-----",
          end = "-----END PGP SIGNATURE-----",
          replace = true
      ),
      MsgBlock.Type.SIGNATURE to CryptoArmorStringHeaderDefinition(
          begin = "-----BEGIN PGP SIGNATURE-----",
          end = "-----END PGP SIGNATURE-----",
          replace = false
      ),
      MsgBlock.Type.ENCRYPTED_MSG to CryptoArmorStringHeaderDefinition(
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
  val ARMOR_HEADER_UNKNOWN = ARMOR_HEADER_DICT[MsgBlock.Type.UNKNOWN]!!

  @JvmStatic
  val FLOWCRYPT_HEADERS = listOf(
      Pair(ArmoredOutputStream.VERSION_HDR, "FlowCrypt ${BuildConfig.VERSION_NAME} Gmail Encryption"),
      Pair("Comment", "Seamlessly send and receive encrypted email")
  )

  // note: using MsgBlock.Type.UNKNOWN instead of "key" in Typescript
  @JvmStatic
  fun normalize(armored: String, blockType: MsgBlock.Type): String {
    if (blockType != MsgBlock.Type.UNKNOWN
        && !MsgBlock.Type.replaceableBlockTypes.contains(blockType)) {
      throw IllegalArgumentException("Can't normalize block of type '$blockType'")
    }

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
      } else if (nl2 > 1 && nl4== 1) {
        // newlines doubled. GPA on windows does this, and sometimes message
        // can get extracted this way from HTML
        result = result.replace("\n\n", "\n")
      }
    }

    // check for and fix missing a mandatory empty line
    val lines = result.split('\n')
    val h = ARMOR_HEADER_DICT[blockType]!!
    if (lines.size > 5 && lines[0].indexOf(h.begin)  > -1
        && lines[lines.size - 1].indexOf(h.end) > -1 && lines.indexOf("") == -1) {
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
      MsgBlock.Type.PUBLIC_KEY, MsgBlock.Type.PRIVATE_KEY,
      MsgBlock.Type.ENCRYPTED_MSG, MsgBlock.Type.UNKNOWN
  )

  @JvmStatic
  private val normalizeRegex1 = Regex("^[a-zA-Z0-9\\-_. ]+: .+\$")

  @JvmStatic
  private val normalizeRegex2 = Regex("^[a-zA-Z0-9/+]{32,77}\$")
}
