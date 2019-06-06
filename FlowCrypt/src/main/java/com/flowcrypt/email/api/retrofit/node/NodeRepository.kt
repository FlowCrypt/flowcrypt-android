/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.node

import android.os.AsyncTask
import androidx.lifecycle.MutableLiveData
import com.flowcrypt.email.api.retrofit.request.node.NodeRequest
import com.flowcrypt.email.api.retrofit.request.node.NodeRequestWrapper
import com.flowcrypt.email.api.retrofit.request.node.ParseDecryptMsgRequest
import com.flowcrypt.email.api.retrofit.request.node.ParseKeysRequest
import com.flowcrypt.email.api.retrofit.request.node.ZxcvbnStrengthBarRequest
import com.flowcrypt.email.api.retrofit.response.node.BaseNodeResponse
import com.flowcrypt.email.api.retrofit.response.node.NodeResponseWrapper

/**
 * It's an entry point of all requests to work with Node.js server.
 *
 * @author Denis Bondarenko
 * Date: 2/15/19
 * Time: 10:26 AM
 * E-mail: DenBond7@gmail.com
 */
class NodeRepository : PgpApiRepository {

  override fun fetchKeyDetails(requestCode: Int, liveData: MutableLiveData<NodeResponseWrapper<*>>, raw: String?) {
    load(requestCode, liveData, ParseKeysRequest(raw))
  }

  override fun parseDecryptMsg(requestCode: Int, liveData: MutableLiveData<NodeResponseWrapper<*>>,
                               request: ParseDecryptMsgRequest) {
    load(requestCode, liveData, request)
  }

  override fun checkPassphraseStrength(requestCode: Int, liveData: MutableLiveData<NodeResponseWrapper<*>>,
                                       request: ZxcvbnStrengthBarRequest) {
    load(requestCode, liveData, request)
  }

  /**
   * It's a base method for all requests to Node.js
   *
   * @param requestCode The unique request code for identify the current action.
   * @param data        An instance of [MutableLiveData] which will be used for result delivering.
   * @param nodeRequest An instance of [NodeRequest]
   */
  private fun load(requestCode: Int, data: MutableLiveData<NodeResponseWrapper<*>>, nodeRequest: NodeRequest) {
    data.value = NodeResponseWrapper.loading(requestCode, null, 0)
    Worker(data).execute(NodeRequestWrapper(requestCode, nodeRequest))
  }

  /**
   * Here we describe a logic of making requests to Node.js using [retrofit2.Retrofit]
   */
  private class Worker internal constructor(private val liveData: MutableLiveData<NodeResponseWrapper<*>>) : AsyncTask<NodeRequestWrapper<*>, Void, NodeResponseWrapper<*>>() {

    override fun doInBackground(vararg nodeRequestWrappers: NodeRequestWrapper<*>): NodeResponseWrapper<*> {
      val nodeRequestWrapper = nodeRequestWrappers[0]
      val baseNodeResult: BaseNodeResponse

      val nodeService = NodeRetrofitHelper.getRetrofit()!!.create(NodeService::class.java)
      try {
        val response = nodeRequestWrapper.request.getResponse(nodeService)
        if (response != null) {
          val time = response.raw().receivedResponseAtMillis() - response.raw().sentRequestAtMillis()
          if (response.body() != null) {
            baseNodeResult = response.body() as BaseNodeResponse

            if (baseNodeResult.error != null) {
              return NodeResponseWrapper.error(nodeRequestWrapper.requestCode, baseNodeResult, time)
            }
          } else {
            return NodeResponseWrapper.exception(nodeRequestWrapper.requestCode,
                NullPointerException("The response body is null!"), null, time)
          }
        } else {
          return NodeResponseWrapper.exception(nodeRequestWrapper.requestCode,
              NullPointerException("The response is null!"), null, 0)
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
  }
}
