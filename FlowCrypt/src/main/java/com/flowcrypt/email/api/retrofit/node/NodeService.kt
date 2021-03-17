/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.node

import com.flowcrypt.email.api.retrofit.request.node.ComposeEmailRequest
import com.flowcrypt.email.api.retrofit.request.node.DecryptFileRequest
import com.flowcrypt.email.api.retrofit.request.node.DecryptKeyRequest
import com.flowcrypt.email.api.retrofit.request.node.EncryptKeyRequest
import com.flowcrypt.email.api.retrofit.request.node.EncryptMsgRequest
import com.flowcrypt.email.api.retrofit.request.node.GmailBackupSearchRequest
import com.flowcrypt.email.api.retrofit.request.node.ParseDecryptMsgRequest
import com.flowcrypt.email.api.retrofit.request.node.ParseKeysRequest
import com.flowcrypt.email.api.retrofit.request.node.VersionRequest
import com.flowcrypt.email.api.retrofit.request.node.ZxcvbnStrengthBarRequest
import com.flowcrypt.email.api.retrofit.response.node.ComposeEmailResult
import com.flowcrypt.email.api.retrofit.response.node.DecryptKeyResult
import com.flowcrypt.email.api.retrofit.response.node.DecryptedFileResult
import com.flowcrypt.email.api.retrofit.response.node.EncryptKeyResult
import com.flowcrypt.email.api.retrofit.response.node.EncryptedMsgResult
import com.flowcrypt.email.api.retrofit.response.node.GmailBackupSearchResult
import com.flowcrypt.email.api.retrofit.response.node.ParseDecryptedMsgResult
import com.flowcrypt.email.api.retrofit.response.node.ParseKeysResult
import com.flowcrypt.email.api.retrofit.response.node.VersionResult
import com.flowcrypt.email.api.retrofit.response.node.ZxcvbnStrengthBarResult
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Streaming

/**
 * This class describes all available calls to the Node.js server.
 *
 * @author Denis Bondarenko
 * Date: 1/13/19
 * Time: 3:25 PM
 * E-mail: DenBond7@gmail.com
 */
interface NodeService {
  @POST("/")
  fun getVersion(@Body request: VersionRequest): Call<VersionResult>

  @POST("/")
  fun rawRequest(@Body body: RequestBody): Call<ResponseBody>

  @POST("/")
  fun encryptMsg(@Body request: EncryptMsgRequest): Call<EncryptedMsgResult>

  @POST("/")
  fun composeEmail(@Body request: ComposeEmailRequest): Call<ComposeEmailResult>

  @POST("/")
  fun parseDecryptMsgOld(@Body request: ParseDecryptMsgRequest): Call<ParseDecryptedMsgResult>

  @POST("/")
  suspend fun parseDecryptMsg(@Body request: ParseDecryptMsgRequest): Response<ParseDecryptedMsgResult>

  @POST("/")
  fun gmailBackupSearch(@Body request: GmailBackupSearchRequest): Call<GmailBackupSearchResult>

  @POST("/")
  fun parseKeys(@Body request: ParseKeysRequest): Call<ParseKeysResult>

  @POST("/")
  suspend fun parseKeysSuspend(@Body request: ParseKeysRequest): Response<ParseKeysResult>

  @POST("/")
  fun decryptKey(@Body request: DecryptKeyRequest): Call<DecryptKeyResult>

  @POST("/")
  fun encryptKey(@Body request: EncryptKeyRequest): Call<EncryptKeyResult>

  @POST("/")
  @Streaming
  fun decryptFile(@Body request: DecryptFileRequest): Call<DecryptedFileResult>

  @POST("/")
  fun zxcvbnStrengthBar(@Body request: ZxcvbnStrengthBarRequest): Call<ZxcvbnStrengthBarResult>

  @POST("/")
  suspend fun zxcvbnStrengthBarSuspend(@Body request: ZxcvbnStrengthBarRequest): Response<ZxcvbnStrengthBarResult>
}
