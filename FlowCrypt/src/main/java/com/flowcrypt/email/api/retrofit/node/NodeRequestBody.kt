/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.node

import com.flowcrypt.email.api.retrofit.request.node.NodeRequest
import okhttp3.MediaType
import okhttp3.RequestBody
import okhttp3.internal.Util
import okio.BufferedSink
import okio.ByteString
import okio.Okio
import okio.Source
import java.io.BufferedInputStream
import java.io.ByteArrayInputStream
import java.io.IOException

/**
 * This is a custom realization of [RequestBody] which will be used by [NodeRequestBodyConverter].
 *
 *
 * Every request body will have the next structure: "endpoint\njson\nbytes"( where "endpoint" is a required parameter):
 *
 *  * UTF8-encoded request name before the first LF (ASCII code 10)
 *  * UTF8-encoded request JSON metadata before the second LF (ASCII code 10)
 *  * binary data afterwards until the end of stream
 *
 *
 * @author Denis Bondarenko
 * Date: 1/10/19
 * Time: 5:54 PM
 * E-mail: DenBond7@gmail.com
 */
class NodeRequestBody constructor(private val nodeRequest: NodeRequest,
                                  private val json: ByteString) : RequestBody() {

  override fun contentType(): MediaType? {
    return null
  }

  @Throws(IOException::class)
  override fun writeTo(sink: BufferedSink) {
    sink.writeUtf8(nodeRequest.endpoint)
    sink.writeByte('\n'.toInt())
    sink.write(json)
    sink.writeByte('\n'.toInt())
    if (nodeRequest.data != null) {
      var source: Source? = null
      try {
        source = Okio.source(ByteArrayInputStream(nodeRequest.data))
        sink.writeAll(source!!)
      } finally {
        Util.closeQuietly(source)
      }
    }

    if (nodeRequest.uri != null) {
      var source: Source? = null
      try {
        val inputStream = nodeRequest.context.contentResolver.openInputStream(nodeRequest.uri)
        if (inputStream != null) {
          source = Okio.source(BufferedInputStream(inputStream))
          sink.writeAll(source!!)
        }
      } finally {
        Util.closeQuietly(source)
      }
    }
  }
}
