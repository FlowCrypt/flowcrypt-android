package com.yourorg.sample.api.retrofit.request;

import android.os.AsyncTask;

import com.flowcrypt.email.api.retrofit.node.NodeRetrofitHelper;
import com.flowcrypt.email.api.retrofit.node.NodeService;
import com.flowcrypt.email.api.retrofit.request.node.BaseNodeRequest;
import com.flowcrypt.email.api.retrofit.request.node.VersionRequest;
import com.flowcrypt.email.api.retrofit.response.node.BaseNodeResult;
import com.yourorg.sample.api.retrofit.response.base.NodeResponseWrapper;
import com.yourorg.sample.core.livedata.SingleLiveEvent;
import com.yourorg.sample.node.NodeSecret;

import androidx.lifecycle.LiveData;
import retrofit2.Response;

/**
 * @author DenBond7
 */
public class RequestsManager {
  private SingleLiveEvent<NodeResponseWrapper> data;
  private NodeService nodeService;

  public RequestsManager(NodeSecret nodeSecret) {
    this.data = new SingleLiveEvent<>();
    this.nodeService = NodeRetrofitHelper.getInstance(nodeSecret).getRetrofit().create(NodeService.class);
  }

  public LiveData<NodeResponseWrapper> getData() {
    return data;
  }

  public void getVersion(int requestCode) {
    load(requestCode, new VersionRequest());
  }

  /*public void encryptMsg(int requestCode, String msg) {
    load(requestCode, new NodeRequestBody<>("encryptMsg", new Pubkeys(TestData.getMixedPubKeys()), msg.getBytes()));
  }

  public void decryptMsg(int requestCode, PgpKeyInfo[] prvKeys, String msg) {
    load(requestCode, new NodeRequestBody<>("decryptMsg", new DecryptModel(prvKeys, TestData.passphrases(), null),
        msg.getBytes()));
  }

  public void encryptFile(int requestCode, byte[] data) {
    load(requestCode, new NodeRequestBody<>("encryptFile", new FileModel(TestData.getMixedPubKeys(), "file.txt"),
        data));
  }

  public void encryptFile(int requestCode, Context context, Uri fileUri) {
    load(requestCode, new NodeRequestBody<>(context, "encryptFile", new FileModel(TestData.getMixedPubKeys(), "file" +
        ".txt"), fileUri));
  }

  public void decryptFile(int requestCode, byte[] encryptedData, PgpKeyInfo[] prvKeys) {
    load(requestCode, new NodeRequestBody<>("decryptFile", new DecryptModel(prvKeys, TestData.passphrases(), null),
        encryptedData));
  }*/

  private void load(final int requestCode, BaseNodeRequest baseNodeRequest) {
    new Worker(data, nodeService).execute(new NodeRequestWrapper<>(requestCode, baseNodeRequest));
  }

  private static class Worker extends AsyncTask<NodeRequestWrapper, Void, NodeResponseWrapper> {
    private SingleLiveEvent<NodeResponseWrapper> data;
    private NodeService nodeService;

    public Worker(SingleLiveEvent<NodeResponseWrapper> data, NodeService nodeService) {
      this.data = data;
      this.nodeService = nodeService;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected NodeResponseWrapper doInBackground(NodeRequestWrapper... nodeRequestWrappers) {
      NodeRequestWrapper nodeRequestWrapper = nodeRequestWrappers[0];
      BaseNodeResult baseNodeResult;

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
        } else {
          throw new NullPointerException("The response is null!");
        }

      } catch (Exception e) {
        e.printStackTrace();
        return new NodeResponseWrapper(nodeRequestWrapper.getRequestCode(), e, null);
      }

      return new NodeResponseWrapper(nodeRequestWrapper.getRequestCode(), null, baseNodeResult);
    }

    @Override
    protected void onPostExecute(NodeResponseWrapper nodeResponseWrapper) {
      super.onPostExecute(nodeResponseWrapper);
      data.setValue(nodeResponseWrapper);
    }
  }
}
