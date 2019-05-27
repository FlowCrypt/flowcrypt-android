/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.response.node

import com.flowcrypt.email.api.retrofit.Status

/**
 * @author DenBond7
 */
class NodeResponseWrapper<T : BaseNodeResponse>(val requestCode: Int, val status: Status, val result: T?,
                                                val exception: Throwable?, val executionTime: Long) {
  companion object {
    fun <T : BaseNodeResponse> success(requestCode: Int, data: T?, executionTime: Long): NodeResponseWrapper<T> {
      return NodeResponseWrapper(requestCode, Status.SUCCESS, data, null, executionTime)
    }

    fun <T : BaseNodeResponse> error(requestCode: Int, data: T?, executionTime: Long): NodeResponseWrapper<T> {
      return NodeResponseWrapper(requestCode, Status.ERROR, data, null, executionTime)
    }

    fun <T : BaseNodeResponse> loading(requestCode: Int, data: T?, executionTime: Long): NodeResponseWrapper<T> {
      return NodeResponseWrapper(requestCode, Status.LOADING, data, null, executionTime)
    }

    fun <T : BaseNodeResponse> exception(requestCode: Int, throwable: Throwable, data: T?, executionTime: Long):
        NodeResponseWrapper<T> {
      return NodeResponseWrapper(requestCode, Status.EXCEPTION, data, throwable, executionTime)
    }
  }
}
