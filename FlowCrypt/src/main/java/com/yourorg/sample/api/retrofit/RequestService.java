/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.yourorg.sample.api.retrofit;

import com.yourorg.sample.api.retrofit.response.models.Version;

import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

/**
 * @author DenBond7
 */
public interface RequestService {

  @POST("/")
  Call<Version> getVersion(@Body RequestBody body);

  @POST("/")
  Call<ResponseBody> request(@Body RequestBody body);
}
