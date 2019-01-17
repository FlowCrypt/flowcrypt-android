package com.flowcrypt.email.api.retrofit.node;

import com.flowcrypt.email.api.retrofit.request.node.DecryptFileRequest;
import com.flowcrypt.email.api.retrofit.request.node.DecryptMsgRequest;
import com.flowcrypt.email.api.retrofit.request.node.EncryptFileRequest;
import com.flowcrypt.email.api.retrofit.request.node.EncryptMsgRequest;
import com.flowcrypt.email.api.retrofit.request.node.VersionRequest;
import com.flowcrypt.email.api.retrofit.response.node.DecryptedFileResult;
import com.flowcrypt.email.api.retrofit.response.node.DecryptedMsgResult;
import com.flowcrypt.email.api.retrofit.response.node.EncryptedFileResult;
import com.flowcrypt.email.api.retrofit.response.node.EncryptedMsgResult;
import com.flowcrypt.email.api.retrofit.response.node.VersionResult;

import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.Streaming;

/**
 * This class describes all available calls to the Node.js server.
 *
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

  @POST("/")
  @Streaming
  Call<EncryptedFileResult> encryptFile(@Body EncryptFileRequest encryptFileRequest);

  @POST("/")
  @Streaming
  Call<DecryptedFileResult> decryptFile(@Body DecryptFileRequest decryptFileRequest);
}
