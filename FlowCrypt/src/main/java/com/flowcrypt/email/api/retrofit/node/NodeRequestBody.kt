/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.node

import android.util.Base64InputStream
import com.flowcrypt.email.api.retrofit.request.node.NodeRequest
import com.flowcrypt.email.security.KeyStoreCryptoManager
import okhttp3.MediaType
import okhttp3.RequestBody
import okhttp3.internal.closeQuietly
import okio.Buffer
import okio.BufferedSink
import okio.ByteString
import okio.Source
import okio.source
import java.io.BufferedInputStream
import java.io.ByteArrayInputStream
import javax.crypto.CipherInputStream

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

  override fun writeTo(sink: BufferedSink) {
    sink.writeUtf8(nodeRequest.endpoint)
    sink.writeByte('\n'.toInt())
    sink.write(json)
    sink.writeByte('\n'.toInt())
    var dataSource: Source? = null

    try {
      dataSource = ByteArrayInputStream(nodeRequest.data).source()
      sink.writeAll(dataSource)
    } finally {
      dataSource?.closeQuietly()
    }

    nodeRequest.uri?.let { uri ->
      var uriSource: Source? = null
      try {
        nodeRequest.context?.contentResolver?.openInputStream(uri)?.let { inputStream ->
          if (nodeRequest.hasEncryptedDataInUri) {
            val bufferedInputStream = BufferedInputStream(inputStream)
            val buffer = Buffer()

            val bufferedInputStreamSource = bufferedInputStream.source()
            while (true) {
              if (bufferedInputStreamSource.read(buffer, 1) != -1L) {
                val b = buffer[buffer.size - 1]
                if (b == (-1).toByte() || b == '\n'.toByte()) {
                  break
                }
              } else break
            }

            val iv = String(buffer.readByteArray(buffer.size - 1))

            val cipherForDecryption = KeyStoreCryptoManager.getCipherForDecryption(iv)
            val base64InputStream = Base64InputStream(bufferedInputStream, KeyStoreCryptoManager.BASE64_FLAGS)
            uriSource = CipherInputStream(base64InputStream, cipherForDecryption).source()
          } else {
            uriSource = BufferedInputStream(inputStream).source()
          }

          uriSource?.let {
            sink.writeAll(it)
          }
        }
      } finally {
        uriSource?.closeQuietly()
      }
    }
  }
}
