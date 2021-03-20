/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: Ivan Pizhenko
 */

package com.flowcrypt.email.security.pgp

import com.flowcrypt.email.api.retrofit.response.model.node.MsgBlock
import com.flowcrypt.email.core.msg.MsgBlockParser
import java.nio.charset.StandardCharsets
import kotlin.experimental.and
import org.bouncycastle.bcpg.PacketTags

object PgpMsg {
  /**
   * @return Pair.first  indicates armored (true) or binary (false) format
   *         Pair.second block type
   */
  fun detectBlockType(msg: ByteArray) : Pair<Boolean, MsgBlock.Type> {
    if (msg.isNotEmpty()) {
      val firstByte = msg[0]
      if (firstByte and (0x80.toByte()) == 0x80.toByte()) {
        // 11XX XXXX - potential new pgp packet tag
        val tagNumber = if (firstByte and (0xC0.toByte()) == 0xC0.toByte()) {
          (firstByte and 0x3f).toInt()  // 11TTTTTT where T is tag number bit
        } else { // 10XX XXXX - potential old pgp packet tag
          (firstByte and 0x3c).toInt() ushr (2) // 10TTTTLL where T is tag number bit.
        }
        if (tagNumber <= maxTagNumber) {
          // Indeed a valid OpenPGP packet tag number
          // This does not 100% mean it's OpenPGP message
          // But it's a good indication that it may be
          return Pair(
              false,
              if (messageTypes.contains(tagNumber)) {
                MsgBlock.Type.ENCRYPTED_MSG
              } else {
                MsgBlock.Type.PUBLIC_KEY
              }
          )
        }
      }

      val blocks = MsgBlockParser.detectBlocks(
          // only interested in the first 50 bytes
          // use ASCII, it never fails
          String(msg.copyOfRange(0, 50), StandardCharsets.US_ASCII).trim()
      )
      if (blocks.size == 1 && !blocks[0].complete
          && MsgBlock.Type.fourBlockTypes.contains(blocks[0].type)) {
        return Pair(true, blocks[0].type)
      }
    }
    return Pair(false, MsgBlock.Type.UNKNOWN);
  }

  private val messageTypes = intArrayOf(
      PacketTags.SYM_ENC_INTEGRITY_PRO,
      PacketTags.MOD_DETECTION_CODE,
      20, // SymEncryptedAEADProtected - no BouncyCastle constant for this one
      PacketTags.SYMMETRIC_KEY_ENC,
      PacketTags.COMPRESSED_DATA
  )

  private val maxTagNumber = 20;
}
