/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.node;

import com.flowcrypt.email.api.retrofit.request.node.DecryptFileRequest;
import com.flowcrypt.email.api.retrofit.request.node.DecryptKeyRequest;
import com.flowcrypt.email.api.retrofit.request.node.DecryptMsgRequest;
import com.flowcrypt.email.api.retrofit.request.node.EncryptFileRequest;
import com.flowcrypt.email.api.retrofit.request.node.EncryptMsgRequest;
import com.flowcrypt.email.api.retrofit.request.node.GmailBackupSearchRequest;
import com.flowcrypt.email.api.retrofit.request.node.ParseKeysRequest;
import com.flowcrypt.email.api.retrofit.request.node.VersionRequest;
import com.flowcrypt.email.api.retrofit.response.node.DecryptKeyResult;
import com.flowcrypt.email.api.retrofit.response.node.DecryptedFileResult;
import com.flowcrypt.email.api.retrofit.response.node.DecryptedMsgResult;
import com.flowcrypt.email.api.retrofit.response.node.EncryptedFileResult;
import com.flowcrypt.email.api.retrofit.response.node.EncryptedMsgResult;
import com.flowcrypt.email.api.retrofit.response.node.GmailBackupSearchResult;
import com.flowcrypt.email.api.retrofit.response.node.ParseKeysResult;
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
  Call<VersionResult> getVersion(@Body VersionRequest request);

  @POST("/")
  Call<ResponseBody> rawRequest(@Body RequestBody body);

  @POST("/")
  Call<EncryptedMsgResult> encryptMsg(@Body EncryptMsgRequest request);

  @POST("/")
  Call<DecryptedMsgResult> decryptMsg(@Body DecryptMsgRequest request);

  @POST("/")
  Call<GmailBackupSearchResult> gmailBackupSearch(@Body GmailBackupSearchRequest request);

  @POST("/")
  Call<ParseKeysResult> parseKeys(@Body ParseKeysRequest request);

  @POST("/")
  Call<DecryptKeyResult> decryptKey(@Body DecryptKeyRequest request);

  @POST("/")
  @Streaming
  Call<EncryptedFileResult> encryptFile(@Body EncryptFileRequest request);

  @POST("/")
  @Streaming
  Call<DecryptedFileResult> decryptFile(@Body DecryptFileRequest request);
}
