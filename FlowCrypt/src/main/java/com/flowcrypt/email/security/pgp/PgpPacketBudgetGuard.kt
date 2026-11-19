/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors:
 *   Ivan Pizhenko
 */

package com.flowcrypt.email.security.pgp

import org.bouncycastle.apache.bzip2.CBZip2InputStream
import org.bouncycastle.bcpg.ArmoredInputStream
import org.bouncycastle.bcpg.BCPGInputStream
import org.bouncycastle.bcpg.CompressionAlgorithmTags
import org.bouncycastle.bcpg.CompressedDataPacket
import org.bouncycastle.bcpg.InputStreamPacket
import org.bouncycastle.bcpg.PacketTags
import org.bouncycastle.bcpg.UnsupportedPacketVersionException
import org.bouncycastle.util.io.Streams
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.FilterInputStream
import java.io.IOException
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.util.zip.Inflater
import java.util.zip.InflaterInputStream

/**
 * Bounds the amount of OpenPGP state that may be allocated for a single untrusted message.
 *
 * OpenPGP parsers keep one object per packet. A tiny, highly compressible input can therefore
 * expand into hundreds of thousands of packets (typically prepended signature packets) and
 * exhaust the heap before any limit on emitted/decrypted output applies. This guard walks the
 * packet structure before the data reaches the parser and rejects inputs that exceed a cumulative
 * budget. Compressed layers are followed so that the budget applies to the decompressed packet
 * stream, not just the bytes on the wire. Encrypted layers cannot be inspected without keys;
 * a bomb hidden inside an encrypted message is a remaining limitation of this pre-scan.
 */
object PgpPacketBudgetGuard {
  /** Largest input accepted. Bigger inputs are rejected rather than parsed without a budget. */
  private const val MAX_INPUT_BYTES = 32 * 1024 * 1024

  /** Bounds memory used while following a compressed stream. */
  private const val MAX_DECOMPRESSED_BYTES = 8 * 1024 * 1024L

  /** Total packets accepted across all layers of one message. */
  private const val MAX_TOTAL_PACKETS = 10_000

  /** Prepended signatures are not useful in email and are the usual amplification vector. */
  private const val MAX_SIGNATURE_PACKETS = 1_000

  /** Session key packets are another per-packet state amplification vector. */
  private const val MAX_SESSION_KEY_PACKETS = 1_000

  private const val MAX_NESTING_DEPTH = 8

  private val ARMOR_HEADER_MARKER = "-----BEGIN PGP ".toByteArray(StandardCharsets.US_ASCII)

  class BudgetExceededException(message: String) : IOException(message)

  /**
   * Returns a stream that is safe to hand to an OpenPGP parser. The input is validated and
   * replayed from memory. Oversized input is rejected: skipping the scan would let a small
   * compressible bomb sit behind enough noise to cross any size threshold.
   */
  fun guard(srcInputStream: InputStream): InputStream {
    val buffered = ByteArrayOutputStream()
    val chunk = ByteArray(DEFAULT_BUFFER_SIZE)
    var total = 0

    while (true) {
      val read = srcInputStream.read(chunk)
      if (read == -1) break
      total += read
      if (total > MAX_INPUT_BYTES) {
        throw BudgetExceededException("OpenPGP message is too large")
      }
      buffered.write(chunk, 0, read)
    }

    val bytes = buffered.toByteArray()
    validate(bytes)
    return ByteArrayInputStream(bytes)
  }

  private fun validate(bytes: ByteArray) {
    val armorStart = indexOfArmorHeader(bytes)
    val offset = maxOf(0, armorStart)
    val source = ByteArrayInputStream(bytes, offset, bytes.size - offset)
    val input = if (armorStart >= 0) {
      BCPGInputStream(ArmoredInputStream(source))
    } else {
      BCPGInputStream(source)
    }
    walk(input, Counters(), depth = 0)
  }

  private fun indexOfArmorHeader(bytes: ByteArray): Int {
    // Armor is only recognized at the start of the stream, after leading whitespace.
    // Searching anywhere would let binary input hide a marker inside a packet body.
    var start = 0
    while (start < bytes.size && bytes[start].toInt().toChar().isWhitespace()) start++
    if (start + ARMOR_HEADER_MARKER.size > bytes.size) return -1
    for (j in ARMOR_HEADER_MARKER.indices) {
      if (bytes[start + j] != ARMOR_HEADER_MARKER[j]) return -1
    }
    return start
  }

  private class Counters(var total: Int = 0, var signatures: Int = 0, var sessionKeys: Int = 0)

  private fun walk(input: BCPGInputStream, counters: Counters, depth: Int) {
    if (depth > MAX_NESTING_DEPTH) {
      throw BudgetExceededException("OpenPGP message nests too deeply")
    }

    while (true) {
      val tag = input.nextPacketTag()
      if (tag == -1) return

      counters.total++
      if (counters.total > MAX_TOTAL_PACKETS) {
        throw BudgetExceededException("OpenPGP message has too many packets")
      }

      when (tag) {
        PacketTags.SIGNATURE -> {
          counters.signatures++
          if (counters.signatures > MAX_SIGNATURE_PACKETS) {
            throw BudgetExceededException("OpenPGP message has too many signature packets")
          }
          consumePacket(input, tag)
        }

        PacketTags.PUBLIC_KEY_ENC_SESSION, PacketTags.SYMMETRIC_KEY_ENC_SESSION -> {
          counters.sessionKeys++
          if (counters.sessionKeys > MAX_SESSION_KEY_PACKETS) {
            throw BudgetExceededException("OpenPGP message has too many session key packets")
          }
          consumePacket(input, tag)
        }

        PacketTags.COMPRESSED_DATA -> {
          val packet = input.readPacket() as? CompressedDataPacket
            ?: throw BudgetExceededException("Malformed compressed OpenPGP packet")
          val decompressed = LimitedInputStream(
            decompress(packet.algorithm, packet.inputStream), MAX_DECOMPRESSED_BYTES
          )
          walk(BCPGInputStream(decompressed), counters, depth + 1)
        }

        else -> consumePacket(input, tag)
      }
    }
  }

  private fun decompress(algorithm: Int, source: InputStream): InputStream = when (algorithm) {
    CompressionAlgorithmTags.UNCOMPRESSED -> source
    CompressionAlgorithmTags.ZIP -> InflaterInputStream(source, Inflater(true))
    CompressionAlgorithmTags.ZLIB -> InflaterInputStream(source)
    CompressionAlgorithmTags.BZIP2 -> CBZip2InputStream(source)
    else -> throw BudgetExceededException("Unsupported OpenPGP compression algorithm: $algorithm")
  }

  private fun consumePacket(input: BCPGInputStream, tag: Int) {
    try {
      val packet = input.readPacket() ?: return
      // Stream packets leave their body unread; skip it so the next header is aligned.
      if (packet is InputStreamPacket) {
        Streams.drain(packet.inputStream)
      }
    } catch (e: UnsupportedPacketVersionException) {
      // Only the signature parser drains the body before throwing. Other packet types would
      // leave the stream mid-packet, so reject instead of misreading later headers.
      if (tag != PacketTags.SIGNATURE) throw e
    }
  }

  private class LimitedInputStream(source: InputStream, private val maxBytes: Long) :
    FilterInputStream(source) {
    private var readBytes = 0L

    override fun read(): Int {
      val value = super.read()
      if (value >= 0) count(1)
      return value
    }

    override fun read(b: ByteArray, off: Int, len: Int): Int {
      val read = super.read(b, off, len)
      if (read > 0) count(read.toLong())
      return read
    }

    private fun count(count: Long) {
      readBytes += count
      if (readBytes > maxBytes) {
        throw BudgetExceededException("Decompressed OpenPGP data is too large")
      }
    }
  }
}
