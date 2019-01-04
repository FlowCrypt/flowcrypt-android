/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.yourorg.sample.api.retrofit;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.yourorg.sample.node.NodeSecret;

import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;

import androidx.annotation.NonNull;
import okhttp3.Headers;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;


/**
 * @author DenBond7
 */
public final class RetrofitHelper {
  private static final int TIMEOUT = 30;
  private static RetrofitHelper ourInstance;
  private OkHttpClient okHttpClient;
  private Retrofit retrofit;
  private Gson gson;

  private RetrofitHelper(final NodeSecret nodeSecret) {
    okHttpClient = getOkHttpClientBuilder(nodeSecret).build();
    gson = new GsonBuilder()
        .excludeFieldsWithoutExposeAnnotation()
        .serializeNulls()
        .create();

    Retrofit.Builder retrofitBuilder = new Retrofit.Builder()
        .baseUrl("https://localhost:" + nodeSecret.port + "/")
        .addConverterFactory(GsonConverterFactory.create(gson))
        .client(okHttpClient);

    retrofit = retrofitBuilder.build();
  }

  public static RetrofitHelper getInstance(NodeSecret nodeSecret) {
    if (ourInstance == null) {
      ourInstance = new RetrofitHelper(nodeSecret);
    }

    return ourInstance;
  }

  public OkHttpClient getOkHttpClient() {
    return okHttpClient;
  }

  public Retrofit getRetrofit() {
    return retrofit;
  }

  public Gson getGson() {
    return gson;
  }

  @NonNull
  private OkHttpClient.Builder getOkHttpClientBuilder(NodeSecret nodeSecret) {
    return new OkHttpClient.Builder()
        .connectTimeout(TIMEOUT, TimeUnit.SECONDS)
        .readTimeout(TIMEOUT, TimeUnit.SECONDS)
        .writeTimeout(TIMEOUT, TimeUnit.SECONDS)
        .addInterceptor(headersInterceptor(nodeSecret))
        .addInterceptor(getHttpLoggingInterceptor())
        .sslSocketFactory(nodeSecret.getSslSocketFactory(), nodeSecret.getSslTrustManager())
        .followRedirects(false)
        .followSslRedirects(false)
        .hostnameVerifier(trustOurOwnCrtHostnameVerifier(nodeSecret));
  }

  @NonNull
  private HttpLoggingInterceptor getHttpLoggingInterceptor() {
    return new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY);
  }

  private Interceptor headersInterceptor(final NodeSecret nodeSecret) {
    return new Interceptor() {
      @Override
      public okhttp3.Response intercept(Chain chain) throws IOException {
        okhttp3.Request request = chain.request();
        Headers headers = request
            .headers()
            .newBuilder()
            .add("Authorization", nodeSecret.authHeader)
            .add("Connection", "Keep-Alive")
            .build();
        request = request.newBuilder().headers(headers).build();
        return chain.proceed(request);
      }
    };
  }

  private HostnameVerifier trustOurOwnCrtHostnameVerifier(final NodeSecret nodeSecret) {
    return new HostnameVerifier() {
      @Override
      public boolean verify(String host, SSLSession session) {
        try {
          X509Certificate crt = (X509Certificate) session.getPeerCertificates()[0];
          if (!NodeSecret.HOSTNAME.equals(host) || !NodeSecret.CRT_SUBJECT.equals(crt.getSubjectDN().getName())) {
            return false;
          }
          return crt.getSerialNumber().equals(nodeSecret.getSslCrtSerialNumber());
        } catch (Exception e) {
          return false;
        }
      }
    };
  }
}
