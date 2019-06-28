/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.node

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Converter
import retrofit2.Retrofit
import java.lang.reflect.Type

/**
 * A [converter][Converter.Factory] which uses a custom realization for request and responses to communicate
 * with inner Node.js server.
 *
 * @author Denis Bondarenko
 * Date: 1/10/19
 * Time: 5:20 PM
 * E-mail: DenBond7@gmail.com
 */
class NodeConverterFactory private constructor(private val gson: Gson) : Converter.Factory() {

  override fun responseBodyConverter(type: Type?, annotations: Array<Annotation>?, retrofit: Retrofit?):
      Converter<ResponseBody, *> {
    val adapter = gson.getAdapter(TypeToken.get(type!!))
    return NodeResponseBodyConverter(gson, adapter)
  }

  override fun requestBodyConverter(type: Type?, parameterAnnotations: Array<Annotation>?,
                                    methodAnnotations: Array<Annotation>?, retrofit: Retrofit?):
      Converter<*, RequestBody> {
    val adapter = gson.getAdapter(TypeToken.get(type!!))
    return NodeRequestBodyConverter(gson, adapter)
  }

  companion object {

    /**
     * Create an instance using a default [Gson] instance for conversion. Encoding to JSON and
     * decoding from JSON (when no charset is specified by a header) will use UTF-8.
     */
    fun create(): NodeConverterFactory {
      return create(Gson())
    }

    /**
     * Create an instance using `gson` for conversion. Encoding to JSON and
     * decoding from JSON (when no charset is specified by a header) will use UTF-8.
     */
    // Guarding public API nullability.
    fun create(gson: Gson?): NodeConverterFactory {
      if (gson == null) throw NullPointerException("gson == null")
      return NodeConverterFactory(gson)
    }
  }
}
