package com.cryptup.api.retrofit;

import com.cryptup.BuildConfig;
import com.cryptup.api.gson.deserializer.MessagePrototypeResponseJsonDeserializer;
import com.cryptup.api.retrofit.response.MessagePrototypeResponse;
import com.google.gson.GsonBuilder;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;


/**
 * This is a singleton in which objects are created to perform network requests.
 * This class has instance of OkHttpClient and Retrofit.
 *
 * @author Denis Bondarenko
 *         Date: 08.07.2015
 *         Time: 13:06
 *         E-mail: DenBond7@gmail.com
 */
public class ApiHelper {
    private static ApiHelper ourInstance = new ApiHelper();
    private OkHttpClient okHttpClient;
    private Retrofit retrofit;

    private ApiHelper() {
        OkHttpClient.Builder okHttpClientBuilder = new OkHttpClient.Builder()
                .connectTimeout(2, TimeUnit.MINUTES)
                .readTimeout(2, TimeUnit.MINUTES)
                .writeTimeout(2, TimeUnit.MINUTES);

        if (BuildConfig.DEBUG) {
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

    public static ApiHelper getInstance() {
        return ourInstance;
    }

    public OkHttpClient getOkHttpClient() {
        return okHttpClient;
    }

    public Retrofit getRetrofit() {
        return retrofit;
    }
}
