/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.node

import android.content.Context
import android.os.AsyncTask
import androidx.lifecycle.MutableLiveData
import com.flowcrypt.email.api.retrofit.LoadingState
import com.flowcrypt.email.api.retrofit.request.node.DecryptKeyRequest
import com.flowcrypt.email.api.retrofit.request.node.GenerateKeyRequest
import com.flowcrypt.email.api.retrofit.request.node.NodeRequest
import com.flowcrypt.email.api.retrofit.request.node.NodeRequestWrapper
import com.flowcrypt.email.api.retrofit.request.node.ParseDecryptMsgRequest
import com.flowcrypt.email.api.retrofit.request.node.ParseKeysRequest
import com.flowcrypt.email.api.retrofit.request.node.ZxcvbnStrengthBarRequest
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.api.retrofit.response.node.BaseNodeResponse
import com.flowcrypt.email.api.retrofit.response.node.DecryptKeyResult
import com.flowcrypt.email.api.retrofit.response.node.GenerateKeyResult
import com.flowcrypt.email.api.retrofit.response.node.NodeResponseWrapper
import com.flowcrypt.email.api.retrofit.response.node.ParseDecryptedMsgResult
import com.flowcrypt.email.api.retrofit.response.node.ParseKeysResult
import com.flowcrypt.email.api.retrofit.response.node.ZxcvbnStrengthBarResult
import com.flowcrypt.email.model.PgpContact
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * It's an entry point of all requests to work with Node.js server.
 *
 * @author Denis Bondarenko
 * Date: 2/15/19
 * Time: 10:26 AM
 * E-mail: DenBond7@gmail.com
 */
class NodeRepository : PgpApiRepository {
  override suspend fun decryptKey(context: Context, armoredKey: String, passphrases: List<String>):
      Result<DecryptKeyResult> = withContext(Dispatchers.IO) {
    val apiService = NodeRetrofitHelper.getRetrofit()!!.create(NodeApiService::class.java)
    getResult { apiService.decryptKey(DecryptKeyRequest(armoredKey, passphrases)) }
  }

  override suspend fun fetchKeyDetails(request: ParseKeysRequest): Result<ParseKeysResult?> =
      withContext(Dispatchers.IO) {
        val apiService = NodeRetrofitHelper.getRetrofit()!!.create(NodeService::class.java)
        getResult(call = { apiService.parseKeysSuspend(request) })
      }

  override fun fetchKeyDetails(requestCode: Int, liveData: MutableLiveData<NodeResponseWrapper<*>>, raw: String?) {
    load(requestCode, liveData, ParseKeysRequest(raw))
  }

  override suspend fun parseDecryptMsg(requestCode: Int, request: ParseDecryptMsgRequest): Result<ParseDecryptedMsgResult?> =
      withContext(Dispatchers.IO) {
        val apiService = NodeRetrofitHelper.getRetrofit()!!.create(NodeService::class.java)
        getResult(call = { apiService.parseDecryptMsg(request) })
      }

  override fun checkPassphraseStrength(requestCode: Int, liveData: MutableLiveData<NodeResponseWrapper<*>>,
                                       request: ZxcvbnStrengthBarRequest) {
    load(requestCode, liveData, request)
  }

  override suspend fun createPrivateKey(context: Context, passphrase: String, pgpContacts: List<PgpContact>): Result<GenerateKeyResult?> =
      withContext(Dispatchers.IO) {
        val apiService = NodeRetrofitHelper.getRetrofit()!!.create(NodeService::class.java)
        getResult(call = { apiService.generateKeySuspend(GenerateKeyRequest(passphrase, pgpContacts)) })
      }

  override suspend fun zxcvbnStrengthBar(context: Context, guesses: Double): Result<ZxcvbnStrengthBarResult?> =
      withContext(Dispatchers.IO) {
        val apiService = NodeRetrofitHelper.getRetrofit()!!.create(NodeService::class.java)
        getResult(call = { apiService.zxcvbnStrengthBarSuspend(ZxcvbnStrengthBarRequest(guesses)) })
      }

  /**
   * It's a base method for all requests to Node.js
   *
   * @param requestCode The unique request code for identify the current action.
   * @param data        An instance of [MutableLiveData] which will be used for result delivering.
   * @param nodeRequest An instance of [NodeRequest]
   */
  private fun load(requestCode: Int, data: MutableLiveData<NodeResponseWrapper<*>>, nodeRequest: NodeRequest) {
    data.value = NodeResponseWrapper.loading(requestCode = requestCode, data = null, loadingState = LoadingState.PREPARE_REQUEST)
    Worker(data).execute(NodeRequestWrapper(requestCode, nodeRequest))
  }

  /**
   * Here we describe a logic of making requests to Node.js using [retrofit2.Retrofit]
   */
  private class Worker(private val liveData: MutableLiveData<NodeResponseWrapper<*>>)
    : AsyncTask<NodeRequestWrapper<*>, NodeResponseWrapper<*>, NodeResponseWrapper<*>>() {

    override fun doInBackground(vararg nodeRequestWrappers: NodeRequestWrapper<*>): NodeResponseWrapper<*> {
      val nodeRequestWrapper = nodeRequestWrappers[0]
      val baseNodeResult: BaseNodeResponse

      publishProgress(NodeResponseWrapper.loading(requestCode = nodeRequestWrapper.requestCode, data = null,
          loadingState = LoadingState.PREPARE_SERVICE))
      val nodeService = NodeRetrofitHelper.getRetrofit()!!.create(NodeService::class.java)
      try {
        publishProgress(NodeResponseWrapper.loading(requestCode = nodeRequestWrapper.requestCode, data = null,
            loadingState = LoadingState.RUN_REQUEST))
        val response = nodeRequestWrapper.request.getResponse(nodeService)
        publishProgress(NodeResponseWrapper.loading(requestCode = nodeRequestWrapper.requestCode, data = null,
            loadingState = LoadingState.RESPONSE_RECEIVED))
        val time = response.raw().receivedResponseAtMillis - response.raw().sentRequestAtMillis
        if (response.body() != null) {
          baseNodeResult = response.body() as BaseNodeResponse

          if (baseNodeResult.apiError != null) {
            return NodeResponseWrapper.error(nodeRequestWrapper.requestCode, baseNodeResult, time)
          }
        } else {
          return NodeResponseWrapper.exception(nodeRequestWrapper.requestCode,
              NullPointerException("The response body is null!"), null, time)
        }
      } catch (e: Exception) {
        e.printStackTrace()
        return NodeResponseWrapper.exception(nodeRequestWrapper.requestCode, e, null, 0)
      }

      return NodeResponseWrapper.success(nodeRequestWrapper.requestCode, baseNodeResult, 0)
    }

    override fun onPostExecute(nodeResponseWrapper: NodeResponseWrapper<*>) {
      super.onPostExecute(nodeResponseWrapper)
      liveData.value = nodeResponseWrapper
    }

    override fun onProgressUpdate(vararg values: NodeResponseWrapper<*>?) {
      super.onProgressUpdate(*values)
      liveData.value = values[0]
    }
  }
}
