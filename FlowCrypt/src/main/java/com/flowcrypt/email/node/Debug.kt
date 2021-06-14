/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.node

import java.util.Arrays

class Debug {
  companion object {
    private const val MAX_LENGTH = 20

    @JvmStatic
    fun printChunk(name: String, string: String) {
      print(name, string.substring(0, Math.min(string.length, MAX_LENGTH)).toByteArray())
    }

    @JvmStatic
    fun printChunk(name: String, data: ByteArray) {
      print(name, Arrays.copyOfRange(data, 0, Math.min(data.size, MAX_LENGTH)))
    }

    @JvmStatic
    private fun print(name: String, bytes: ByteArray) {
      val unsignedBytes = IntArray(bytes.size)
      for (i in bytes.indices) {
        unsignedBytes[i] = bytes[i].toInt() and 0xFF
      }
      println("Debug.printChunk[" + name + "]: " + Arrays.toString(unsignedBytes))
    }
  }
}
