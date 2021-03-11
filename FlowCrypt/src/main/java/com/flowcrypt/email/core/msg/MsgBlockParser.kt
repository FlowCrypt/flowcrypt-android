/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: ivan
 */

package com.flowcrypt.email.core.msg

import com.flowcrypt.email.api.retrofit.response.model.node.MsgBlock
import com.flowcrypt.email.api.retrofit.response.model.node.MsgBlockFactory
import com.flowcrypt.email.security.pgp.PgpArmor

@Suppress("unused")
object MsgBlockParser {

  const val ARMOR_HEADER_MAX_LENGTH = 50

  @JvmStatic
  fun detectBlocks(text: String): Pair<List<MsgBlock>, String> {
    val blocks = mutableListOf<MsgBlock>()
    val normalized = normalize(text)
    var startAt = 0
    while (true) {
      val continueAt = detectBlockNext(normalized, startAt, blocks)
      if (startAt >= continueAt) return Pair(blocks, normalized)
      startAt = continueAt
    }
  }

  @JvmStatic
  private fun detectBlockNext(text: String, startAt: Int, blocks: MutableList<MsgBlock>): Int {
    val initialBlockCount = blocks.size
    var continueAt = -1
    val begin = text.indexOf(PgpArmor.ARMOR_HEADER_DICT[MsgBlock.Type.UNKNOWN]!!.begin, startAt)
    if (begin != -1) { // found
      val potentialBeginHeader = text.substring(begin, begin + ARMOR_HEADER_MAX_LENGTH)
      for (blockHeaderKvp in PgpArmor.ARMOR_HEADER_DICT) {
        val blockHeaderDef = blockHeaderKvp.value
        if (!blockHeaderDef.replace) continue
        val indexOfConfirmedBegin = potentialBeginHeader.indexOf(blockHeaderDef.begin)
        if (indexOfConfirmedBegin != 0) continue
        if (begin > startAt) {
          // only replace blocks if they begin on their own line
          // contains deliberate block: `-----BEGIN PGP PUBLIC KEY BLOCK-----\n...`
          // contains deliberate block: `Hello\n-----BEGIN PGP PUBLIC KEY BLOCK-----\n...`
          // just plaintext (accidental block): `Hello -----BEGIN PGP PUBLIC KEY BLOCK-----\n...`
          // block treated as plaintext, not on dedicated line - considered accidental
          // this will actually cause potential deliberate blocks
          // that follow accidental block to be ignored
          // but if the message already contains accidental
          // (not on dedicated line) blocks, it's probably a good thing to ignore the rest
          var potentialTextBeforeBlockBegun = text.substring(startAt, begin)
          if (!potentialTextBeforeBlockBegun.endsWith('\n')) continue
          potentialTextBeforeBlockBegun = potentialTextBeforeBlockBegun.trim()
          if (potentialTextBeforeBlockBegun.isNotEmpty()) {
            blocks.add(MsgBlockFactory.fromContent(
                MsgBlock.Type.PLAIN_TEXT, potentialTextBeforeBlockBegun))
          }
        }

        val endIndex: Int
        val foundBlockEndHeaderLength: Int
        if (blockHeaderDef.endRegexp != null) {
          val found = blockHeaderDef.endRegexp.find(text.substring(begin))
          if (found != null) {
            endIndex = begin + found.range.first
            foundBlockEndHeaderLength = found.range.last - found.range.first
          } else {
            endIndex = -1
            foundBlockEndHeaderLength = 0
          }
        } else { // string
          val end = blockHeaderDef.end!!
          endIndex = text.indexOf(end, begin + blockHeaderDef.begin.length)
          foundBlockEndHeaderLength = if (endIndex == -1) 0 else end.length
        }

        if (endIndex != -1) {
          // identified end of the same block
          blocks.add(
              MsgBlockFactory.fromContent(
                  blockHeaderKvp.key,
                  text.substring(begin, endIndex + foundBlockEndHeaderLength).trim()
              )
          )
          continueAt = endIndex + foundBlockEndHeaderLength
        } else {
          // corresponding end not found
          blocks.add(
              MsgBlockFactory.fromContent(blockHeaderKvp.key, text.substring(begin), true)
          )
        }
        break
      }
    }

    if (text.isNotEmpty() && blocks.size == initialBlockCount) {
      // didn't find any blocks, but input is non-empty
      val potentialText = text.substring(startAt).trim()
      if (potentialText.isNotEmpty()) {
        blocks.add(MsgBlockFactory.fromContent(MsgBlock.Type.PLAIN_TEXT, potentialText))
      }
    }

    return continueAt
  }

  @JvmStatic
  private fun normalize(str: String): String {
    return normalizeSpaces(normalizeDashes(str))
  }

  @JvmStatic
  private fun normalizeSpaces(str: String): String {
    return str.replace(160.toChar(), ' ')
  }

  @JvmStatic
  private fun normalizeDashes(str: String): String {
    return str.replace(dashesRegex, "-----")
  }

  @JvmStatic
  private val dashesRegex = Regex("^—–|—–$")
}
