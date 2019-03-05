/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.node;

import android.os.AsyncTask;

import com.flowcrypt.email.api.retrofit.request.node.NodeRequest;
import com.flowcrypt.email.api.retrofit.request.node.NodeRequestWrapper;
import com.flowcrypt.email.api.retrofit.request.node.ParseKeysRequest;
import com.flowcrypt.email.api.retrofit.response.node.BaseNodeResult;
import com.flowcrypt.email.api.retrofit.response.node.NodeResponseWrapper;

import androidx.lifecycle.MutableLiveData;
import retrofit2.Response;

/**
 * It's an entry point of all requests to work with Node.js server.
 *
 * @author Denis Bondarenko
 * Date: 2/15/19
 * Time: 10:26 AM
 * E-mail: DenBond7@gmail.com
 */
public final class NodeRepository implements PgpApiRepository {

  @Override
  public void fetchKeyDetails(int requestCode, MutableLiveData<NodeResponseWrapper> liveData, String raw) {
    load(requestCode, liveData, new ParseKeysRequest(raw));
  }

  /**
   * It's a base method for all requests to Node.js
   *
   * @param requestCode The unique request code for identify the current action.
   * @param data        An instance of {@link MutableLiveData} which will be used for result delivering.
   * @param nodeRequest An instance of {@link NodeRequest}
   */
  private void load(int requestCode, MutableLiveData<NodeResponseWrapper> data, NodeRequest nodeRequest) {
    data.setValue(NodeResponseWrapper.loading(requestCode));
    new Worker(data).execute(new NodeRequestWrapper<>(requestCode, nodeRequest));
  }

  /**
   * Here we describe a logic of making requests to Node.js using {@link retrofit2.Retrofit}
   */
  private static class Worker extends AsyncTask<NodeRequestWrapper, Void, NodeResponseWrapper> {
    private MutableLiveData<NodeResponseWrapper> liveData;
    private NodeRetrofitHelper retrofitHelper;

    Worker(MutableLiveData<NodeResponseWrapper> data) {
      this.liveData = data;
      this.retrofitHelper = NodeRetrofitHelper.getInstance();
    }

    @Override
    protected NodeResponseWrapper doInBackground(NodeRequestWrapper... nodeRequestWrappers) {
      NodeRequestWrapper nodeRequestWrapper = nodeRequestWrappers[0];
      BaseNodeResult baseNodeResult;

      NodeService nodeService = retrofitHelper.getRetrofit().create(NodeService.class);
      try {
        Response response = nodeRequestWrapper.getRequest().getResponse(nodeService);
        if (response != null) {
          long time = response.raw().receivedResponseAtMillis() - response.raw().sentRequestAtMillis();
          if (response.body() != null) {
            baseNodeResult = (BaseNodeResult) response.body();
            baseNodeResult.setTime(time);

            if (baseNodeResult.getError() != null) {
              return NodeResponseWrapper.error(nodeRequestWrapper.getRequestCode(), baseNodeResult);
            }
          } else {
            return NodeResponseWrapper.exception(nodeRequestWrapper.getRequestCode(),
                new NullPointerException("The response body is null!"));
          }
        } else {
          return NodeResponseWrapper.exception(nodeRequestWrapper.getRequestCode(),
              new NullPointerException("The response is null!"));
        }
      } catch (Exception e) {
        e.printStackTrace();
        return NodeResponseWrapper.exception(nodeRequestWrapper.getRequestCode(), e);
      }

      return NodeResponseWrapper.success(nodeRequestWrapper.getRequestCode(), baseNodeResult);
    }

    @Override
    protected void onPostExecute(NodeResponseWrapper nodeResponseWrapper) {
      super.onPostExecute(nodeResponseWrapper);
      liveData.setValue(nodeResponseWrapper);
    }
  }
}
