package com.flowcrypt.email.api.retrofit.request.node;

import android.content.Context;
import android.net.Uri;

import com.flowcrypt.email.api.retrofit.node.NodeService;
import com.google.gson.annotations.Expose;

import java.io.IOException;
import java.util.List;

import retrofit2.Response;

/**
 * Using this class we can create a request to encrypt an input data using the given public keys.
 *
 * @author Denis Bondarenko
 * Date: 1/15/19
 * Time: 9:07 AM
 * E-mail: DenBond7@gmail.com
 */
public class EncryptFileRequest extends BaseNodeRequest {

  private byte[] data;

  @Expose
  private String name;

  @Expose
  private List<String> pubKeys;

  public EncryptFileRequest(byte[] data, String name, List<String> pubKeys) {
    this.data = data;
    this.name = name;
    this.pubKeys = pubKeys;
  }

  public EncryptFileRequest(Context context, Uri uri, String name, List<String> pubKeys) {
    super(context, uri);
    this.name = name;
    this.pubKeys = pubKeys;
  }

  @Override
  public String getEndpoint() {
    return "encryptFile";
  }

  @Override
  public byte[] getData() {
    return data;
  }

  @Override
  public Response getResponse(NodeService nodeService) throws IOException {
    if (nodeService != null) {
      return nodeService.encryptFile(this).execute();
    } else return null;
  }
}
