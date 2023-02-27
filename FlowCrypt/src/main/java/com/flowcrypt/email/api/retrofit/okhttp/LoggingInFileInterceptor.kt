/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.okhttp

import android.content.Context
import com.flowcrypt.email.BuildConfig
import com.flowcrypt.email.util.DebugLogWriter
import okhttp3.Headers
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.internal.http.promisesBody
import okio.Buffer
import java.io.EOFException
import java.io.File
import java.nio.charset.Charset
import java.nio.charset.UnsupportedCharsetException
import java.util.concurrent.TimeUnit

/**
 * This interceptor writes logs to some file on the sd card.
 *
 *
 * An OkHttp interceptor which logs request and response information. Can be applied as an
 * [application interceptor][OkHttpClient.interceptors] or as a [ ][OkHttpClient.networkInterceptors].
 *
 * The format of the logs created by
 * this class should not be considered stable and may change slightly between releases. If you need
 * a stable logging format, use your own interceptor.
 *
 *
 * See https://github.com/square/okhttp/blob/master/okhttp-logging-interceptor/src/main/java
 * /okhttp3/logging/HttpLoggingInterceptor.java
 *
 * @author Denys Bondarenko
 */
class LoggingInFileInterceptor constructor(context: Context, fileName: String) : Interceptor {
  private val logger: Logger

  init {
    val file = File(context.filesDir, BuildConfig.APPLICATION_ID + "_" + fileName + ".log")
    logger = object : Logger {
      private val debugLogWriter = DebugLogWriter(file)
      override fun log(message: String) {
        debugLogWriter.writeLog(message)
      }
    }
  }

  @Volatile
  private var level = Level.NONE

  override fun intercept(chain: Interceptor.Chain): Response {
    val level = this.level

    val request = chain.request()
    if (level == Level.NONE) {
      return chain.proceed(request)
    }

    val logBody = level == Level.BODY
    val logHeaders = logBody || level == Level.HEADERS

    val requestBody = request.body
    val hasRequestBody = requestBody != null

    val connection = chain.connection()
    val protocol = connection?.protocol() ?: Protocol.HTTP_1_1
    var requestStartMsg =
      "--> " + request.method + ' '.toString() + request.url + ' '.toString() + protocol
    if (!logHeaders && hasRequestBody) {
      requestStartMsg += " (" + requestBody!!.contentLength() + "-byte body)"
    }
    logger.log(requestStartMsg)

    if (logHeaders) {
      if (hasRequestBody) {
        // Request body headers are only present when installed as a network interceptor.
        // Force
        // them to be included (when available) so there values are known.
        if (requestBody!!.contentType() != null) {
          logger.log("Content-Type: " + requestBody.contentType()!!)
        }
        if (requestBody.contentLength() != (-1).toLong()) {
          logger.log("Content-Length: " + requestBody.contentLength())
        }
      }

      val headers = request.headers
      var i = 0
      val count = headers.size
      while (i < count) {
        val name = headers.name(i)
        // Skip headers from the request body as they are explicitly logged above.
        if (!"Content-Type".equals(name, ignoreCase = true) && !"Content-Length".equals
            (name, ignoreCase = true)
        ) {
          logger.log(name + ": " + headers.value(i))
        }
        i++
      }

      if (!logBody || !hasRequestBody) {
        logger.log("--> END " + request.method)
      } else if (isBodyEncoded(request.headers)) {
        logger.log("--> END " + request.method + " (encoded body omitted)")
      } else {
        val buffer = Buffer()
        requestBody!!.writeTo(buffer)

        var charset: Charset? = UTF8
        val contentType = requestBody.contentType()
        if (contentType != null) {
          charset = contentType.charset(UTF8)
        }

        logger.log("")
        if (isPlainText(buffer)) {
          logger.log(buffer.readString(charset!!))
          logger.log(
            "--> END " + request.method
                + " (" + requestBody.contentLength() + "-byte body)"
          )
        } else {
          logger.log(
            "--> END " + request.method + " (binary "
                + requestBody.contentLength() + "-byte body omitted)"
          )
        }
      }
    }

    val startNs = System.nanoTime()
    val response: Response
    try {
      response = chain.proceed(request)
    } catch (e: Exception) {
      logger.log("<-- HTTP FAILED: $e")
      throw e
    }

    val tookMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNs)

    val responseBody = response.body
    val contentLength = responseBody?.contentLength() ?: 0
    val bodySize = if (contentLength != (-1).toLong()) "$contentLength-byte" else "unknown-length"
    logger.log(
      "<-- " + response.code + ' '.toString() + response.message
          + ' '.toString() + response.request.url + " (" + tookMs +
          "ms" + (if (!logHeaders) ", $bodySize body" else "") + ')'.toString()
    )

    if (logHeaders) {
      val headers = response.headers
      var i = 0
      val count = headers.size
      while (i < count) {
        logger.log(headers.name(i) + ": " + headers.value(i))
        i++
      }

      if (!logBody || !response.promisesBody()) {
        logger.log("<-- END HTTP")
      } else if (isBodyEncoded(response.headers)) {
        logger.log("<-- END HTTP (encoded body omitted)")
      } else {
        val source = responseBody!!.source()
        source.request(java.lang.Long.MAX_VALUE) // Buffer the entire body.
        val buffer = source.buffer

        var charset: Charset? = UTF8
        val contentType = responseBody.contentType()
        if (contentType != null) {
          try {
            charset = contentType.charset(UTF8)
          } catch (e: UnsupportedCharsetException) {
            logger.log("")
            logger.log("Couldn't decode the response body; charset is likely " + "malformed.")
            logger.log("<-- END HTTP")

            return response
          }

        }

        if (!isPlainText(buffer)) {
          logger.log("")
          logger.log("<-- END HTTP (binary " + buffer.size + "-byte body omitted)")
          return response
        }

        if (contentLength != 0L) {
          logger.log("")
          logger.log(buffer.clone().readString(charset!!))
        }

        logger.log("<-- END HTTP (" + buffer.size + "-byte body)")
      }
    }

    return response
  }

  /**
   * Change the level at which this interceptor logs.
   */
  fun setLevel(level: Level?): LoggingInFileInterceptor {
    if (level == null) throw NullPointerException("level == null. Use Level.NONE instead.")
    this.level = level
    return this
  }

  private fun isBodyEncoded(headers: Headers): Boolean {
    val contentEncoding = headers.get("Content-Encoding")
    return contentEncoding != null && !"identity".equals(contentEncoding, ignoreCase = true)
  }

  enum class Level {
    /**
     * No logs.
     */
    NONE,

    /**
     * Logs request and response lines.
     *
     *
     *
     * Example:
     * <pre>`--> POST /greeting http/1.1 (3-byte body)
     *
     * <-- 200 OK (22ms, 6-byte body)
    `</pre> *
     */
    BASIC,

    /**
     * Logs request and response lines and their respective headers.
     *
     *
     *
     * Example:
     * <pre>`--> POST /greeting http/1.1
     * Host: example.com
     * Content-Type: plain/text
     * Content-Length: 3
     * --> END POST
     *
     * <-- 200 OK (22ms)
     * Content-Type: plain/text
     * Content-Length: 6
     * <-- END HTTP
    `</pre> *
     */
    HEADERS,

    /**
     * Logs request and response lines and their respective headers and bodies (if present).
     *
     *
     *
     * Example:
     * <pre>`--> POST /greeting http/1.1
     * Host: example.com
     * Content-Type: plain/text
     * Content-Length: 3
     *
     * Hi?
     * --> END POST
     *
     * <-- 200 OK (22ms)
     * Content-Type: plain/text
     * Content-Length: 6
     *
     * Hello!
     * <-- END HTTP
    `</pre> *
     */
    BODY
  }

  interface Logger {
    fun log(message: String)
  }

  companion object {
    private val UTF8 = Charset.forName("UTF-8")

    /**
     * Returns true if the body in question probably contains human readable text. Uses a small sample
     * of code points to detect unicode control characters commonly used in binary file signatures.
     */
    private fun isPlainText(buffer: Buffer): Boolean {
      try {
        val prefix = Buffer()
        val byteCount = if (buffer.size < 64) buffer.size else 64
        buffer.copyTo(prefix, 0, byteCount)
        for (i in 0..15) {
          if (prefix.exhausted()) {
            break
          }
          val codePoint = prefix.readUtf8CodePoint()
          if (Character.isISOControl(codePoint) && !Character.isWhitespace(codePoint)) {
            return false
          }
        }
        return true
      } catch (e: EOFException) {
        return false // Truncated UTF-8 sequence.
      }
    }
  }
}
