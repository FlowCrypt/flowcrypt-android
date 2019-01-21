package com.flowcrypt.email.api.retrofit.node;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;

import com.flowcrypt.email.api.retrofit.request.node.DecryptFileRequest;
import com.flowcrypt.email.api.retrofit.request.node.DecryptMsgRequest;
import com.flowcrypt.email.api.retrofit.request.node.EncryptFileRequest;
import com.flowcrypt.email.api.retrofit.request.node.EncryptMsgRequest;
import com.flowcrypt.email.api.retrofit.request.node.NodeRequest;
import com.flowcrypt.email.api.retrofit.request.node.NodeRequestWrapper;
import com.flowcrypt.email.api.retrofit.request.node.VersionRequest;
import com.flowcrypt.email.api.retrofit.response.node.BaseNodeResult;
import com.flowcrypt.email.api.retrofit.response.node.NodeResponseWrapper;
import com.flowcrypt.email.jetpack.livedata.SingleLiveEvent;
import com.flowcrypt.email.model.PgpKeyInfo;
import com.flowcrypt.email.node.NodeSecret;
import com.flowcrypt.email.node.TestData;

import java.util.Arrays;

import androidx.lifecycle.LiveData;
import retrofit2.Response;

/**
 * @author DenBond7
 */
public class RequestsManager {
  private SingleLiveEvent<NodeResponseWrapper> data;
  private NodeRetrofitHelper retrofitHelper;

  public RequestsManager(NodeSecret nodeSecret) {
    this.data = new SingleLiveEvent<>();
    this.retrofitHelper = NodeRetrofitHelper.getInstance(nodeSecret);
  }

  public LiveData<NodeResponseWrapper> getData() {
    return data;
  }

  public void getVersion(int requestCode) {
    load(requestCode, new VersionRequest());
  }

  public void encryptMsg(int requestCode, String msg) {
    load(requestCode, new EncryptMsgRequest(msg, Arrays.asList(TestData.getMixedPubKeys())));
  }

  public void decryptMsg(int requestCode, String msg, PgpKeyInfo[] prvKeys) {
    load(requestCode, new DecryptMsgRequest(msg, prvKeys, TestData.passphrases()));
  }

  public void encryptFile(int requestCode, byte[] data) {
    load(requestCode, new EncryptFileRequest(data, "file.txt", Arrays.asList(TestData.getMixedPubKeys())));
  }

  public void encryptFile(int requestCode, Context context, Uri fileUri) {
    load(requestCode, new EncryptFileRequest(context, fileUri, "file.txt", Arrays.asList(TestData.getMixedPubKeys())));
  }

  public void decryptFile(int requestCode, byte[] encryptedData, PgpKeyInfo[] prvKeys) {
    load(requestCode, new DecryptFileRequest(encryptedData, prvKeys, TestData.passphrases()));
  }

  private void load(final int requestCode, NodeRequest nodeRequest) {
    new Worker(data, retrofitHelper).execute(new NodeRequestWrapper<>(requestCode, nodeRequest));
  }

  private static class Worker extends AsyncTask<NodeRequestWrapper, Void, NodeResponseWrapper> {
    private SingleLiveEvent<NodeResponseWrapper> data;
    private NodeRetrofitHelper retrofitHelper;

    Worker(SingleLiveEvent<NodeResponseWrapper> data, NodeRetrofitHelper retrofitHelper) {
      this.data = data;
      this.retrofitHelper = retrofitHelper;
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
          } else {
            throw new NullPointerException("The response body is null!");
          }

          baseNodeResult.setTime(time);
        } else {
          throw new NullPointerException("The response is null!");
        }

      } catch (Exception e) {
        e.printStackTrace();
        return new NodeResponseWrapper<>(nodeRequestWrapper.getRequestCode(), e, (BaseNodeResult) null);
      }

      return new NodeResponseWrapper<>(nodeRequestWrapper.getRequestCode(), null, baseNodeResult);
    }

    @Override
    protected void onPostExecute(NodeResponseWrapper nodeResponseWrapper) {
      super.onPostExecute(nodeResponseWrapper);
      data.setValue(nodeResponseWrapper);
    }
  }
}
