/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors:
 *   Ivan Pizhenko
 */

package com.flowcrypt.email.extensions.java.io

import java.io.InputStream
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

fun InputStream.readText(charset: Charset = StandardCharsets.UTF_8): String =
  String(readBytes(), charset)
