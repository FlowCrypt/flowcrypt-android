/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (tom@cryptup.org).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit;

import android.content.Context;
import android.support.v7.preference.PreferenceManager;

import com.flowcrypt.email.BuildConfig;
import com.flowcrypt.email.Constants;
import com.flowcrypt.email.api.gson.deserializer.MessagePrototypeResponseJsonDeserializer;
import com.flowcrypt.email.api.retrofit.okhttp.ApiVersionInterceptor;
import com.flowcrypt.email.api.retrofit.okhttp.LoggingInFileInterceptor;
import com.flowcrypt.email.api.retrofit.response.MessagePrototypeResponse;
import com.flowcrypt.email.util.SharedPreferencesHelper;
import com.google.gson.GsonBuilder;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;


/**
 * This class will be used to perform network requests.
 * This class has instance of OkHttpClient and Retrofit.
 *
 * @author Denis Bondarenko
 *         Date: 08.07.2015
 *         Time: 13:06
 *         E-mail: DenBond7@gmail.com
 */
public class ApiHelper {
    private OkHttpClient okHttpClient;
    private Retrofit retrofit;

    private ApiHelper(Context context) {
        OkHttpClient.Builder okHttpClientBuilder = new OkHttpClient.Builder()
                .connectTimeout(2, TimeUnit.MINUTES)
                .readTimeout(2, TimeUnit.MINUTES)
                .writeTimeout(2, TimeUnit.MINUTES);

        okHttpClientBuilder.addInterceptor(new ApiVersionInterceptor());

        if (BuildConfig.DEBUG) {
            if (SharedPreferencesHelper.getBoolean(PreferenceManager.getDefaultSharedPreferences
                    (context), Constants.PREFERENCES_KEY_IS_WRITE_LOGS_TO_FILE_ENABLE, false)) {
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
                .baseUrl("https://attester.cryptup.io")
                .addConverterFactory(GsonConverterFactory
                        .create(new GsonBuilder()
                                .excludeFieldsWithoutExposeAnnotation()
                                .serializeNulls()
                                .registerTypeAdapter(MessagePrototypeResponse.class, new
                                        MessagePrototypeResponseJsonDeserializer())
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
