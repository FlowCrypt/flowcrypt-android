package com.flowcrypt.email.api.retrofit.node;

import com.flowcrypt.email.api.retrofit.request.node.DecryptMsgRequest;
import com.flowcrypt.email.api.retrofit.request.node.EncryptMsgRequest;
import com.flowcrypt.email.api.retrofit.request.node.VersionRequest;
import com.flowcrypt.email.api.retrofit.response.node.DecryptedMsgResult;
import com.flowcrypt.email.api.retrofit.response.node.EncryptedMsgResult;
import com.flowcrypt.email.api.retrofit.response.node.VersionResult;

import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

/**
 * @author Denis Bondarenko
 * Date: 1/13/19
 * Time: 3:25 PM
 * E-mail: DenBond7@gmail.com
 */
public interface NodeService {
  @POST("/")
  Call<VersionResult> getVersion(@Body VersionRequest versionRequest);

  @POST("/")
  Call<ResponseBody> rawRequest(@Body RequestBody body);

  @POST("/")
  Call<EncryptedMsgResult> encryptMsg(@Body EncryptMsgRequest encryptMsgRequest);

  @POST("/")
  Call<DecryptedMsgResult> decryptMsg(@Body DecryptMsgRequest decryptMsgRequest);
}
