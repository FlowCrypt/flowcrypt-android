/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.node

import android.content.Context
import android.net.Uri
import android.os.AsyncTask
import androidx.lifecycle.LiveData
import com.flowcrypt.email.api.retrofit.request.node.DecryptFileRequest
import com.flowcrypt.email.api.retrofit.request.node.EncryptFileRequest
import com.flowcrypt.email.api.retrofit.request.node.EncryptMsgRequest
import com.flowcrypt.email.api.retrofit.request.node.NodeRequest
import com.flowcrypt.email.api.retrofit.request.node.NodeRequestWrapper
import com.flowcrypt.email.api.retrofit.request.node.ParseDecryptMsgRequest
import com.flowcrypt.email.api.retrofit.request.node.VersionRequest
import com.flowcrypt.email.api.retrofit.response.node.BaseNodeResponse
import com.flowcrypt.email.api.retrofit.response.node.NodeResponseWrapper
import com.flowcrypt.email.jetpack.livedata.SingleLiveEvent
import com.flowcrypt.email.model.PgpKeyInfo
import com.flowcrypt.email.node.TestData
import java.util.*

/**
 * @author DenBond7
 */
object RequestsManager {
  private var data: SingleLiveEvent<NodeResponseWrapper<*>> = SingleLiveEvent()

  fun getData(): LiveData<NodeResponseWrapper<*>>? {
    return data
  }

  fun getVersion(requestCode: Int) {
    load(requestCode, VersionRequest())
  }

  fun encryptMsg(requestCode: Int, msg: String) {
    load(requestCode, EncryptMsgRequest(msg, Arrays.asList(*TestData.mixedPubKeys)))
  }

  fun decryptMsg(requestCode: Int, msg: ByteArray, prvKeys: Array<PgpKeyInfo>) {
    load(requestCode, ParseDecryptMsgRequest(msg, Arrays.asList(*prvKeys)))
  }

  fun encryptFile(requestCode: Int, data: ByteArray) {
    load(requestCode, EncryptFileRequest(data, "file.txt", Arrays.asList(*TestData.mixedPubKeys)))
  }

  fun encryptFile(requestCode: Int, context: Context, fileUri: Uri) {
    load(requestCode, EncryptFileRequest(context, fileUri, "file.txt", Arrays.asList(*TestData.mixedPubKeys)))
  }

  fun decryptFile(requestCode: Int, encryptedData: ByteArray, prvKeys: Array<PgpKeyInfo>) {
    load(requestCode, DecryptFileRequest(encryptedData, Arrays.asList(*prvKeys)))
  }

  private fun load(requestCode: Int, nodeRequest: NodeRequest) {
    Worker(data).execute(NodeRequestWrapper(requestCode, nodeRequest))
  }

  private class Worker internal constructor(
      private val data: SingleLiveEvent<NodeResponseWrapper<*>>) : AsyncTask<NodeRequestWrapper<*>, Void,
      NodeResponseWrapper<*>>() {

    override fun doInBackground(vararg nodeRequestWrappers: NodeRequestWrapper<*>): NodeResponseWrapper<*> {
      val nodeRequestWrapper = nodeRequestWrappers[0]
      val baseNodeResult: BaseNodeResponse

      val nodeService = NodeRetrofitHelper.getRetrofit()!!.create(NodeService::class.java)
      var time = 0L
      try {
        val response = nodeRequestWrapper.request.getResponse(nodeService)
        time = response.raw().receivedResponseAtMillis() - response.raw().sentRequestAtMillis()
        if (response.body() != null) {
          baseNodeResult = response.body() as BaseNodeResponse
        } else {
          throw NullPointerException("The response body is null!")
        }

      } catch (e: Exception) {
        e.printStackTrace()
        return NodeResponseWrapper.exception(nodeRequestWrapper.requestCode, e, null, time)
      }

      return NodeResponseWrapper.success(nodeRequestWrapper.requestCode, baseNodeResult, time)
    }

    override fun onPostExecute(nodeResponseWrapper: NodeResponseWrapper<*>) {
      super.onPostExecute(nodeResponseWrapper)
      data.value = nodeResponseWrapper
    }
  }
}
