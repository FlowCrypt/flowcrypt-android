/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit;

import android.content.Context;

import com.flowcrypt.email.Constants;
import com.flowcrypt.email.api.retrofit.okhttp.ApiVersionInterceptor;
import com.flowcrypt.email.api.retrofit.okhttp.LoggingInFileInterceptor;
import com.flowcrypt.email.util.GeneralUtil;
import com.flowcrypt.email.util.SharedPreferencesHelper;
import com.google.gson.GsonBuilder;

import java.util.concurrent.TimeUnit;

import androidx.preference.PreferenceManager;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;


/**
 * This class will be used to perform network requests.
 * This class has instance of OkHttpClient and Retrofit.
 *
 * @author Denis Bondarenko
 * Date: 08.07.2015
 * Time: 13:06
 * E-mail: DenBond7@gmail.com
 */
public final class ApiHelper {
  private static final int TIMEOUT = 10;
  private OkHttpClient okHttpClient;
  private Retrofit retrofit;

  private ApiHelper(Context context) {
    OkHttpClient.Builder okHttpClientBuilder = new OkHttpClient.Builder()
        .connectTimeout(TIMEOUT, TimeUnit.SECONDS)
        .readTimeout(TIMEOUT, TimeUnit.SECONDS)
        .writeTimeout(TIMEOUT, TimeUnit.SECONDS);

    okHttpClientBuilder.addInterceptor(new ApiVersionInterceptor());

    if (GeneralUtil.isDebugBuild()) {
      boolean isWriteLogsEnabled = SharedPreferencesHelper.getBoolean(PreferenceManager.getDefaultSharedPreferences
          (context), Constants.PREFERENCES_KEY_IS_WRITE_LOGS_TO_FILE_ENABLE, false);

      if (isWriteLogsEnabled) {
        LoggingInFileInterceptor loggingInFileInterceptor = new LoggingInFileInterceptor();
        loggingInFileInterceptor.setLevel(LoggingInFileInterceptor.Level.BODY);
        okHttpClientBuilder.addInterceptor(loggingInFileInterceptor);
      }

      HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
      loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
      okHttpClientBuilder.addInterceptor(loggingInterceptor);
    }

    okHttpClient = okHttpClientBuilder.build();

    Retrofit.Builder retrofitBuilder = new Retrofit.Builder()
        .baseUrl(Constants.ATTESTER_URL)
        .addConverterFactory(GsonConverterFactory
            .create(new GsonBuilder()
                .excludeFieldsWithoutExposeAnnotation()
                .serializeNulls()
                .create()))
        .client(okHttpClient);

    retrofit = retrofitBuilder.build();
  }

  public static ApiHelper getInstance(Context context) {
    return new ApiHelper(context);
  }

  public OkHttpClient getOkHttpClient() {
    return okHttpClient;
  }

  public Retrofit getRetrofit() {
    return retrofit;
  }
}
