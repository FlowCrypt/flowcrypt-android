/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit

import android.content.Context
import androidx.preference.PreferenceManager
import com.flowcrypt.email.BuildConfig
import com.flowcrypt.email.Constants
import com.flowcrypt.email.api.retrofit.okhttp.ApiVersionInterceptor
import com.flowcrypt.email.api.retrofit.okhttp.LoggingInFileInterceptor
import com.flowcrypt.email.util.GeneralUtil
import com.flowcrypt.email.util.SharedPreferencesHelper
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.util.concurrent.TimeUnit


/**
 * This class will be used to perform network requests.
 * This class has instance of OkHttpClient and Retrofit.
 *
 * @author Denys Bondarenko
 */
class ApiHelper private constructor(context: Context) {
  private val okHttpClient: OkHttpClient
  val retrofit: Retrofit
  val gson: Gson

  init {
    val okHttpClientBuilder = OkHttpClient.Builder()
      .connectTimeout(TIMEOUT.toLong(), TimeUnit.SECONDS)
      .readTimeout(TIMEOUT.toLong(), TimeUnit.SECONDS)
      .writeTimeout(TIMEOUT.toLong(), TimeUnit.SECONDS)

    okHttpClientBuilder.addInterceptor(ApiVersionInterceptor())
    configureOkHttpClientForDebuggingIfAllowed(context, okHttpClientBuilder)

    okHttpClient = okHttpClientBuilder.build()
    gson = GsonBuilder()
      .excludeFieldsWithoutExposeAnnotation()
      .serializeNulls()
      .create()

    val retrofitBuilder = Retrofit.Builder()
      .baseUrl(BuildConfig.ATTESTER_URL)
      .addConverterFactory(ScalarsConverterFactory.create())
      .addConverterFactory(GsonConverterFactory.create(gson))
      .client(okHttpClient)

    retrofit = retrofitBuilder.build()
  }

  companion object {
    private const val TIMEOUT = 10

    @JvmStatic
    fun getInstance(context: Context): ApiHelper {
      return ApiHelper(context)
    }

    fun configureOkHttpClientForDebuggingIfAllowed(
      context: Context, okHttpClientBuilder: OkHttpClient.Builder
    ) {
      if (GeneralUtil.isDebugBuild()) {
        val isHttpLogEnabled =
          SharedPreferencesHelper.getBoolean(
            PreferenceManager.getDefaultSharedPreferences(context),
            Constants.PREF_KEY_IS_HTTP_LOG_ENABLED, BuildConfig.IS_HTTP_LOG_ENABLED
          )

        if (isHttpLogEnabled) {
          val levelString = SharedPreferencesHelper.getString(
            PreferenceManager
              .getDefaultSharedPreferences(context),
            Constants.PREF_KEY_HTTP_LOG_LEVEL,
            BuildConfig.HTTP_LOG_LEVEL
          )

          val isWriteLogsEnabled =
            SharedPreferencesHelper.getBoolean(
              PreferenceManager.getDefaultSharedPreferences(context),
              Constants.PREF_KEY_IS_WRITE_LOGS_TO_FILE_ENABLED, false
            )

          if (isWriteLogsEnabled) {
            val loggingInFileInterceptor = LoggingInFileInterceptor(context, "API")
            loggingInFileInterceptor.setLevel(
              LoggingInFileInterceptor.Level.valueOf(
                levelString
                  ?: LoggingInFileInterceptor.Level.NONE.name
              )
            )
            okHttpClientBuilder.addInterceptor(loggingInFileInterceptor)
          }

          val loggingInterceptor = HttpLoggingInterceptor()
          loggingInterceptor.level = HttpLoggingInterceptor.Level.valueOf(
            levelString
              ?: HttpLoggingInterceptor.Level.NONE.name
          )
          okHttpClientBuilder.addInterceptor(loggingInterceptor)
        }
      }
    }

    inline fun <reified T> parseAsError(context: Context, response: Response<T>): T? {
      val retrofit = getInstance(context).retrofit
      try {
        val converter =
          retrofit.responseBodyConverter<T>(T::class.java, arrayOfNulls<Annotation>(0))
        val errorBody = response.errorBody() ?: return null
        return converter.convert(errorBody)
      } catch (e: Exception) {
        return null
      }
    }
  }
}
