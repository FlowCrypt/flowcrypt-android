/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.node

import android.content.Context
import androidx.annotation.WorkerThread
import androidx.preference.PreferenceManager
import com.flowcrypt.email.BuildConfig
import com.flowcrypt.email.Constants
import com.flowcrypt.email.api.retrofit.node.gson.NodeGson
import com.flowcrypt.email.node.NodeSecret
import com.flowcrypt.email.util.GeneralUtil
import com.flowcrypt.email.util.SharedPreferencesHelper
import com.google.gson.Gson
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.security.cert.X509Certificate
import java.util.concurrent.CountDownLatch
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
//todo-denbond7 We still receive ACRA reports that retrofit: Retrofit? = null, need to fix this issue
object NodeRetrofitHelper {
  private const val TIMEOUT = 90
  private var okHttpClient: OkHttpClient? = null
  private val countDownLatch: CountDownLatch = CountDownLatch(1)

  @Volatile
  private var retrofit: Retrofit? = null
  var gson: Gson = NodeGson.gson

  @WorkerThread
  fun init(context: Context, nodeSecret: NodeSecret) {
    okHttpClient = getOkHttpClientBuilder(context, nodeSecret).build()

    val retrofitBuilder = Retrofit.Builder()
      .baseUrl("https://localhost:" + nodeSecret.port + "/")
      .addConverterFactory(NodeConverterFactory.create(gson))
      .client(okHttpClient!!)

    retrofit = retrofitBuilder.build()
    countDownLatch.countDown()
  }

  @JvmStatic
  fun getRetrofit(): Retrofit? {
    if (retrofit == null) {
      countDownLatch.await(60, TimeUnit.SECONDS)
    }

    return retrofit
  }

  private fun getOkHttpClientBuilder(
    context: Context,
    nodeSecret: NodeSecret
  ): OkHttpClient.Builder {
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
      val isNodeHttpLogEnabled =
        SharedPreferencesHelper.getBoolean(
          PreferenceManager.getDefaultSharedPreferences(context),
          Constants.PREF_KEY_IS_NODE_HTTP_DEBUG_ENABLED, BuildConfig.IS_NODE_HTTP_DEBUG_ENABLED
        )

      if (isNodeHttpLogEnabled) {
        val levelString = SharedPreferencesHelper.getString(
          PreferenceManager
            .getDefaultSharedPreferences(context),
          Constants.PREF_KEY_NODE_HTTP_LOG_LEVEL,
          BuildConfig.NODE_HTTP_LOG_LEVEL
        )

        val loggingInterceptor = HttpLoggingInterceptor()
        loggingInterceptor.level = HttpLoggingInterceptor.Level.valueOf(
          levelString
            ?: HttpLoggingInterceptor.Level.NONE.name
        )
        builder.addInterceptor(loggingInterceptor)
      }
    }

    return builder
  }

  private fun headersInterceptor(nodeSecret: NodeSecret): Interceptor {
    return Interceptor { chain ->
      var request: okhttp3.Request = chain.request()
      val headers = request
        .headers
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
