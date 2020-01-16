/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.node

import com.flowcrypt.email.api.retrofit.response.node.BaseNodeResponse
import com.google.gson.Gson
import com.google.gson.JsonIOException
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonToken
import okhttp3.ResponseBody
import retrofit2.Converter
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.StringReader
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

/**
 * This class will be used by [NodeConverterFactory] for creating responses.
 *
 *
 * Every response body will have the next structure: "json\nbytes":
 *
 *  * UTF8-encoded request JSON metadata before the first LF (ASCII code 10)
 *  * binary data afterwards until the end of stream
 *
 *
 * @author Denis Bondarenko
 * Date: 1/10/19
 * Time: 5:21 PM
 * E-mail: DenBond7@gmail.com
 */
class NodeResponseBodyConverter<T> internal constructor(
    private val gson: Gson,
    private val adapter: TypeAdapter<T>) : Converter<ResponseBody, T> {

  override fun convert(value: ResponseBody): T? {
    BufferedInputStream(value.source().inputStream()).use { bufferedInputStream ->
      ByteArrayOutputStream().use { outputStream ->
        BufferedOutputStream(outputStream).use { bufferedOutputStream ->
          var c: Int

          //find UTF8-encoded request JSON metadata
          while (true) {
            c = bufferedInputStream.read()

            if (c == -1 || c == '\n'.toInt()) {
              break
            }

            bufferedOutputStream.write(c.toByte().toInt())
          }

          bufferedOutputStream.flush()

          val jsonReader = gson.newJsonReader(StringReader(outputStream.toString(getCharset(value)!!.name())))

          value.use {
            val result = adapter.read(jsonReader)
            if (jsonReader.peek() != JsonToken.END_DOCUMENT) {
              throw JsonIOException("JSON document was not fully consumed.")
            }

            if (result is BaseNodeResponse) {
              val baseNodeResponse = result as BaseNodeResponse
              baseNodeResponse.handleRawData(bufferedInputStream)
            }

            return result
          }
        }
      }
    }
  }

  private fun forceFirstData(bufferedInputStream: BufferedInputStream) {
    bufferedInputStream.mark(0)
    val b = bufferedInputStream.read()
    if (b != -1) {
      bufferedInputStream.reset()
    }
  }

  private fun getCharset(responseBody: ResponseBody): Charset? {
    val contentType = responseBody.contentType()
    return if (contentType != null) contentType.charset(StandardCharsets.UTF_8) else StandardCharsets.UTF_8
  }
}
