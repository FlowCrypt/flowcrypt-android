/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.node

import okhttp3.internal.Util
import okio.BufferedSource
import java.io.BufferedInputStream
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.Reader
import java.nio.charset.Charset
import java.util.*

/**
 * It's a realization of [Reader] which will be used by [NodeResponseBodyConverter] to parse JSON from
 * the buffered input stream.
 *
 * @author Denis Bondarenko
 * Date: 1/11/19
 * Time: 11:54 AM
 * E-mail: DenBond7@gmail.com
 */
class NodeJsonReader constructor(private val bufferedInputStream: BufferedInputStream,
                                 private val source: BufferedSource,
                                 private val charset: Charset) : Reader() {

  private var closed: Boolean = false
  private var delegate: Reader? = null
  private var isStopped: Boolean = false

  override fun read(cbuf: CharArray, off: Int, len: Int): Int {
    if (closed) throw IOException("Stream closed")

    if (isStopped) {
      return -1
    }

    var delegate = this.delegate
    if (delegate == null) {
      val charset = Util.bomAwareCharset(source, this.charset)
      this.delegate = BufferedReader(InputStreamReader(bufferedInputStream, charset))
      delegate = this.delegate
    }

    bufferedInputStream.mark(0)
    delegate!!.mark(1)
    var count = delegate.read(cbuf, off, len)

    if (count != -1) {

      var position = 0
      for (i in cbuf.indices) {
        val c = cbuf[i]

        if (c == '\n') {
          position = i
          isStopped = true
          break
        }
      }

      if (position > 0) {
        delegate.reset()
        bufferedInputStream.reset()

        var c: Int
        while (true) {
          c = bufferedInputStream.read()
          if (c == -1 || c == '\n'.toInt()) {
            break
          }
        }

        Arrays.fill(cbuf, Character.MIN_VALUE)
        count = delegate.read(cbuf, off, position)
        return count
      } else {
        return count
      }
    } else
      return count
  }

  override fun close() {
    closed = true
    if (delegate != null) {
      delegate!!.close()
    } else {
      source.close()
    }
  }
}
