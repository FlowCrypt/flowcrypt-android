/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors:
 *     Ivan Pizhenko
 */

package com.flowcrypt.email.extensions.java.io

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

@Throws(IOException::class)
fun InputStream.readIntoString(charset: Charset = StandardCharsets.UTF_8): String {
  // See https://stackoverflow.com/a/65265664/1540501
  ByteArrayOutputStream().use {
    this.copyTo(it, 4096)
    return it.toString(charset.name())
  }
}
