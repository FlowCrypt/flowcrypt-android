/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.node

import com.flowcrypt.email.api.retrofit.request.node.NodeRequest
import com.google.gson.Gson
import com.google.gson.TypeAdapter
import okhttp3.RequestBody
import okio.Buffer
import retrofit2.Converter
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets

/**
 * This class will be used by [NodeConverterFactory] for creating requests.
 *
 * @author Denis Bondarenko
 * Date: 1/10/19
 * Time: 5:24 PM
 * E-mail: DenBond7@gmail.com
 */
class NodeRequestBodyConverter<F> internal constructor(
  private val gson: Gson,
  private val adapter: TypeAdapter<F>
) : Converter<F, RequestBody> {

  override fun convert(value: F): RequestBody {
    if (value !is NodeRequest) {
      throw IllegalArgumentException("Support only classes that extend " + NodeRequest::class.java.simpleName)
    }

    val buffer = Buffer()
    val writer = OutputStreamWriter(buffer.outputStream(), StandardCharsets.UTF_8)
    val jsonWriter = gson.newJsonWriter(writer)
    adapter.write(jsonWriter, value)
    jsonWriter.close()
    val json = buffer.readByteString()

    return NodeRequestBody(value as NodeRequest, json)
  }
}
