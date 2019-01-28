/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.node;

import com.flowcrypt.email.node.NodeSecret;
import com.flowcrypt.email.util.GeneralUtil;
import com.google.gson.Gson;

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

/**
 * This class helps to receive instance of {@link Retrofit} for requests to the Node.js server.
 *
 * @author Denis Bondarenko
 * Date: 1/13/19
 * Time: 3:28 PM
 * E-mail: DenBond7@gmail.com
 */
public final class NodeRetrofitHelper {
  private static final int TIMEOUT = 30;
  private static NodeRetrofitHelper ourInstance = new NodeRetrofitHelper();
  private OkHttpClient okHttpClient;
  private Retrofit retrofit;
  private Gson gson;

  private NodeRetrofitHelper() {
  }

  public static NodeRetrofitHelper getInstance() {
    return ourInstance;
  }

  public void init(NodeSecret nodeSecret) {
    okHttpClient = getOkHttpClientBuilder(nodeSecret).build();
    gson = NodeGson.getInstance().getGson();

    Retrofit.Builder retrofitBuilder = new Retrofit.Builder()
        .baseUrl("https://localhost:" + nodeSecret.getPort() + "/")
        .addConverterFactory(NodeConverterFactory.create(gson))
        .client(okHttpClient);

    retrofit = retrofitBuilder.build();
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
    OkHttpClient.Builder builder = new OkHttpClient.Builder()
        .connectTimeout(TIMEOUT, TimeUnit.SECONDS)
        .readTimeout(TIMEOUT, TimeUnit.SECONDS)
        .writeTimeout(TIMEOUT, TimeUnit.SECONDS)
        .addInterceptor(headersInterceptor(nodeSecret))
        .sslSocketFactory(nodeSecret.getSslSocketFactory(), nodeSecret.getSslTrustManager())
        .followRedirects(false)
        .followSslRedirects(false)
        .hostnameVerifier(trustOurOwnCrtHostnameVerifier(nodeSecret));

    if (GeneralUtil.isDebugBuild()) {
      builder.addInterceptor(getHttpLoggingInterceptor());
    }

    return builder;
  }

  @NonNull
  private Interceptor getHttpLoggingInterceptor() {
    return new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY);
  }

  private Interceptor headersInterceptor(final NodeSecret nodeSecret) {
    return new Interceptor() {
      @Override
      public okhttp3.Response intercept(@NonNull Chain chain) throws IOException {
        okhttp3.Request request = chain.request();
        Headers headers = request
            .headers()
            .newBuilder()
            .add("Authorization", nodeSecret.getAuthHeader())
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
