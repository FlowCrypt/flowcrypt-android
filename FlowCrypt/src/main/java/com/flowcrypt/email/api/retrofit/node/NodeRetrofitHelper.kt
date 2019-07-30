/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.node

import androidx.annotation.WorkerThread
import com.flowcrypt.email.api.retrofit.node.gson.NodeGson
import com.flowcrypt.email.node.NodeSecret
import com.flowcrypt.email.util.GeneralUtil
import com.flowcrypt.email.util.LogsUtil
import com.google.gson.Gson
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.HostnameVerifier

/**
 * This class helps to receive instance of [Retrofit] for requests to the Node.js server.
 *
 * @author Denis Bondarenko
 * Date: 1/13/19
 * Time: 3:28 PM
 * E-mail: DenBond7@gmail.com
 */
object NodeRetrofitHelper {
  private const val TIMEOUT = 300
  private var okHttpClient: OkHttpClient? = null
  @Volatile
  private var retrofit: Retrofit? = null
  var gson: Gson = NodeGson.gson

  private val httpLoggingInterceptor: Interceptor
    get() = HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BASIC)

  @WorkerThread
  fun init(nodeSecret: NodeSecret) {
    okHttpClient = getOkHttpClientBuilder(nodeSecret).build()

    val retrofitBuilder = Retrofit.Builder()
        .baseUrl("https://localhost:" + nodeSecret.port + "/")
        .addConverterFactory(NodeConverterFactory.create(gson))
        .client(okHttpClient!!)

    retrofit = retrofitBuilder.build()
  }

  @JvmStatic
  fun getRetrofit(): Retrofit? {
    checkAndWaitNode()
    return retrofit
  }

  private fun getOkHttpClientBuilder(nodeSecret: NodeSecret): OkHttpClient.Builder {
    val builder = OkHttpClient.Builder()
        .connectTimeout(TIMEOUT.toLong(), TimeUnit.SECONDS)
        .readTimeout(TIMEOUT.toLong(), TimeUnit.SECONDS)
        .writeTimeout(TIMEOUT.toLong(), TimeUnit.SECONDS)
        .addInterceptor(headersInterceptor(nodeSecret))
        .sslSocketFactory(nodeSecret.sslSocketFactory, nodeSecret.sslTrustManager)
        .followRedirects(false)
        .followSslRedirects(false)
        .hostnameVerifier(trustOurOwnCrtHostnameVerifier(nodeSecret))

    if (GeneralUtil.isDebugBuild()) {
      builder.addInterceptor(httpLoggingInterceptor)
    }

    return builder
  }

  /**
   * This method does 5 attempts to check is Node.js server started.
   */
  private fun checkAndWaitNode() {
    var attemptCount = 5

    while (attemptCount != 0) {
      if (retrofit == null) {
        attemptCount--
        try {
          LogsUtil.d(NodeRetrofitHelper::class.java.simpleName, "Node.js server is not run yet. Trying to wait...")
          Thread.sleep(1000)
        } catch (e: InterruptedException) {
          e.printStackTrace()
        }
      } else {
        return
      }
    }
  }

  private fun headersInterceptor(nodeSecret: NodeSecret): Interceptor {
    return Interceptor { chain ->
      var request: okhttp3.Request = chain.request()
      val headers = request
          .headers()
          .newBuilder()
          .add("Authorization", nodeSecret.authHeader)
          .add("Connection", "Keep-Alive")
          .build()
      request = request.newBuilder().headers(headers).build()
      chain.proceed(request)
    }
  }

  private fun trustOurOwnCrtHostnameVerifier(nodeSecret: NodeSecret): HostnameVerifier {
    return HostnameVerifier { host, session ->
      try {
        val crt = session.peerCertificates[0] as X509Certificate
        return@HostnameVerifier if (NodeSecret.HOSTNAME != host || NodeSecret.CRT_SUBJECT != crt.subjectDN.name) {
          false
        } else crt.serialNumber == nodeSecret.sslCrtSerialNumber
      } catch (e: Exception) {
        return@HostnameVerifier false
      }
    }
  }
}
