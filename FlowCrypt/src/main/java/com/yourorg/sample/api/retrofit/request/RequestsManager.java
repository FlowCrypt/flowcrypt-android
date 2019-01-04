package com.yourorg.sample.api.retrofit.request;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;

import com.flowcrypt.email.R;
import com.yourorg.sample.TestData;
import com.yourorg.sample.api.retrofit.NodeRequestBody;
import com.yourorg.sample.api.retrofit.RequestService;
import com.yourorg.sample.api.retrofit.RetrofitHelper;
import com.yourorg.sample.api.retrofit.request.model.DecryptModel;
import com.yourorg.sample.api.retrofit.request.model.FileModel;
import com.yourorg.sample.api.retrofit.request.model.Pubkeys;
import com.yourorg.sample.api.retrofit.response.base.NodeResponse;
import com.yourorg.sample.core.livedata.SingleLiveEvent;
import com.yourorg.sample.node.NodeSecret;
import com.yourorg.sample.node.results.DecryptFileResult;
import com.yourorg.sample.node.results.DecryptMsgResult;
import com.yourorg.sample.node.results.EncryptFileResult;
import com.yourorg.sample.node.results.EncryptMsgResult;
import com.yourorg.sample.node.results.PgpKeyInfo;
import com.yourorg.sample.node.results.RawNodeResult;
import com.yourorg.sample.node.results.VersionResult;

import androidx.lifecycle.LiveData;
import okhttp3.ResponseBody;
import retrofit2.Response;

/**
 * @author DenBond7
 */
public class RequestsManager {
  private SingleLiveEvent<NodeResponse> data;
  private RequestService requestService;

  public RequestsManager(NodeSecret nodeSecret) {
    this.data = new SingleLiveEvent<>();
    this.requestService = RetrofitHelper.getInstance(nodeSecret).getRetrofit().create(RequestService.class);
  }

  public LiveData<NodeResponse> getData() {
    return data;
  }

  public void getVersion(int requestCode) {
    load(requestCode, new NodeRequestBody<>("version", null, "abc".getBytes()));
  }

  public void encryptMsg(int requestCode, String msg) {
    load(requestCode, new NodeRequestBody<>("encryptMsg", new Pubkeys(TestData.getMixedPubKeys()), msg.getBytes()));
  }

  public void decryptMsg(int requestCode, PgpKeyInfo[] prvKeys, String msg) {
    load(requestCode, new NodeRequestBody<>("decryptMsg", new DecryptModel(prvKeys, TestData.passphrases(), null), msg.getBytes()));
  }

  public void encryptFile(int requestCode, byte[] data) {
    load(requestCode, new NodeRequestBody<>("encryptFile", new FileModel(TestData.getMixedPubKeys(), "file.txt"), data));
  }

  public void encryptFile(int requestCode, Context context, Uri fileUri) {
    load(requestCode, new NodeRequestBody<>(context, "encryptFile", new FileModel(TestData.getMixedPubKeys(), "file.txt"), fileUri));
  }

  public void decryptFile(int requestCode, byte[] encryptedData, PgpKeyInfo[] prvKeys) {
    load(requestCode, new NodeRequestBody<>("decryptFile", new DecryptModel(prvKeys, TestData.passphrases(), null), encryptedData));
  }

  private void load(final int requestCode, NodeRequestBody nodeRequestBody) {
    new Worker(data, requestService).execute(new NodeRequest(requestCode, nodeRequestBody));
  }

  private static class Worker extends AsyncTask<NodeRequest, Void, NodeResponse> {
    private SingleLiveEvent<NodeResponse> data;
    private RequestService requestService;

    public Worker(SingleLiveEvent<NodeResponse> data, RequestService requestService) {
      this.data = data;
      this.requestService = requestService;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected NodeResponse doInBackground(NodeRequest... nodeRequests) {
      NodeRequest nodeRequest = nodeRequests[0];
      RawNodeResult rawNodeResult;
      long time = 0;

      try {
        Response<ResponseBody> response = requestService.request(nodeRequest.getNodeRequestBody()).execute();
        time = response.raw().receivedResponseAtMillis() - response.raw().sentRequestAtMillis();
        if (response.body() != null) {
          switch (nodeRequest.getRequestCode()) {
            case R.id.req_id_get_version:
              rawNodeResult = new VersionResult(null, response.body().byteStream(), time);
              break;

            case R.id.req_id_encrypt_msg:
              rawNodeResult = new EncryptMsgResult(null, response.body().byteStream(), time);
              break;

            case R.id.req_id_decrypt_msg_ecc:
            case R.id.req_id_decrypt_msg_rsa_2048:
            case R.id.req_id_decrypt_msg_rsa_4096:
              rawNodeResult = new DecryptMsgResult(null, response.body().byteStream(), time);
              break;

            case R.id.req_id_encrypt_file:
            case R.id.req_id_encrypt_file_rsa_2048_1mb:
            case R.id.req_id_encrypt_file_rsa_2048_3mb:
            case R.id.req_id_encrypt_file_rsa_2048_5mb:
            case R.id.req_id_encrypt_file_from_uri:
              rawNodeResult = new EncryptFileResult(null, response.body().byteStream(), time);
              break;

            case R.id.req_id_decrypt_file_ecc:
            case R.id.req_id_decrypt_file_rsa_2048:
            case R.id.req_id_decrypt_file_rsa_4096:
            case R.id.req_id_decrypt_file_rsa_2048_1mb:
            case R.id.req_id_decrypt_file_rsa_2048_3mb:
            case R.id.req_id_decrypt_file_rsa_2048_5mb:
            case R.id.req_id_decrypt_file_rsa_2048_from_uri:
              rawNodeResult = new DecryptFileResult(null, response.body().byteStream(), time);
              break;

            default:
              rawNodeResult = new RawNodeResult(null, response.body().byteStream(), time);
              break;
          }

          if (rawNodeResult.getErr() != null) {

          }
        } else {
          throw new NullPointerException("The response body is null!");
        }
      } catch (Exception e) {
        e.printStackTrace();
        rawNodeResult = new RawNodeResult(e, null, time);
      }

      return new NodeResponse(nodeRequest.getRequestCode(), null, rawNodeResult);
    }

    @Override
    protected void onPostExecute(NodeResponse nodeResponse) {
      super.onPostExecute(nodeResponse);
      data.setValue(nodeResponse);
    }
  }
}
