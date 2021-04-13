/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: Ivan Pizhenko
 */

package com.flowcrypt.email.core.msg

import com.flowcrypt.email.api.retrofit.response.model.node.MsgBlock
import com.flowcrypt.email.api.retrofit.response.model.node.MsgBlockFactory
import com.flowcrypt.email.security.pgp.PgpArmor
import com.flowcrypt.email.extensions.lang.normalize

@Suppress("unused")
object MsgBlockParser {

  private const val ARMOR_HEADER_MAX_LENGTH = 50

  @JvmStatic
  fun detectBlocks(text: String): List<MsgBlock> {
    val normalized = text.normalize()
    val blocks = mutableListOf<MsgBlock>()
    var startAt = 0
    while (true) {
      val continueAt = detectNextBlock(normalized, startAt, blocks)
      if (startAt >= continueAt) return blocks
      startAt = continueAt
    }
  }

  @JvmStatic
  private fun detectNextBlock(text: String, startAt: Int, blocks: MutableList<MsgBlock>): Int {
    val initialBlockCount = blocks.size
    var continueAt = -1
    val beginIndex = text.indexOf(
        PgpArmor.ARMOR_HEADER_DICT[MsgBlock.Type.UNKNOWN]!!.begin, startAt
    )
    if (beginIndex != -1) { // found
      val potentialHeaderBegin = text.substring(
          beginIndex,
          (beginIndex + ARMOR_HEADER_MAX_LENGTH).coerceAtMost(text.length)
      )
      for (blockHeaderKvp in PgpArmor.ARMOR_HEADER_DICT) {
        val blockHeaderDef = blockHeaderKvp.value
        if (!blockHeaderDef.replace || potentialHeaderBegin.indexOf(blockHeaderDef.begin) != 0) {
          continue
        }

        if (beginIndex > startAt) {
          // only replace blocks if they begin on their own line
          // contains deliberate block: `-----BEGIN PGP PUBLIC KEY BLOCK-----\n...`
          // contains deliberate block: `Hello\n-----BEGIN PGP PUBLIC KEY BLOCK-----\n...`
          // just plaintext (accidental block): `Hello -----BEGIN PGP PUBLIC KEY BLOCK-----\n...`
          // block treated as plaintext, not on dedicated line - considered accidental
          // this will actually cause potential deliberate blocks
          // that follow accidental block to be ignored
          // but if the message already contains accidental
          // (not on dedicated line) blocks, it's probably a good thing to ignore the rest
          var textBeforeBlock = text.substring(startAt, beginIndex)
          if (!textBeforeBlock.endsWith('\n')) continue
          textBeforeBlock = textBeforeBlock.trim()
          if (textBeforeBlock.isNotEmpty()) {
            blocks.add(MsgBlockFactory.fromContent(MsgBlock.Type.PLAIN_TEXT, textBeforeBlock))
          }
        }

        val end = blockHeaderDef.end
        val endHeaderIndex = text.indexOf(end, beginIndex + blockHeaderDef.begin.length)
        val endHeaderLength = if (endHeaderIndex == -1) 0 else end.length

        if (endHeaderIndex != -1) {
          // identified end of the same block
          continueAt = endHeaderIndex + endHeaderLength
          blocks.add(
              MsgBlockFactory.fromContent(
                  blockHeaderKvp.key,
                  text.substring(beginIndex, continueAt).trim()
              )
          )
        } else {
          // corresponding end not found
          blocks.add(
              MsgBlockFactory.fromContent(blockHeaderKvp.key, text.substring(beginIndex), true)
          )
        }
        break
      }
    }

    if (text.isNotEmpty() && blocks.size == initialBlockCount) {
      // didn't find any blocks, but input is non-empty
      val remainingText = text.substring(startAt).trim()
      if (remainingText.isNotEmpty()) {
        blocks.add(MsgBlockFactory.fromContent(MsgBlock.Type.PLAIN_TEXT, remainingText))
      }
    }

    return continueAt
  }
}
