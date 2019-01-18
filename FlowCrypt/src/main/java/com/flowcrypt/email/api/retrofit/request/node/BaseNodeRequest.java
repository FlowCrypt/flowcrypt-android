package com.flowcrypt.email.api.retrofit.request.node;

import android.content.Context;
import android.net.Uri;

import com.flowcrypt.email.api.retrofit.node.NodeService;

import java.io.IOException;

import retrofit2.Response;

/**
 * It's a base request for the Node backend.
 *
 * @author Denis Bondarenko
 * Date: 1/10/19
 * Time: 5:43 PM
 * E-mail: DenBond7@gmail.com
 */
public class BaseNodeRequest implements NodeRequest {

  protected Context context;
  protected Uri uri;

  public BaseNodeRequest(Context context, Uri uri) {
    this.context = context != null ? context.getApplicationContext() : context;
    this.uri = uri;
  }

  public BaseNodeRequest() {
  }

  @Override
  public String getEndpoint() {
    throw new UnsupportedOperationException("not defined");
  }

  @Override
  public byte[] getData() {
    return new byte[0];
  }

  @Override
  public Uri getUri() {
    return uri;
  }

  @Override
  public Context getContext() {
    return context;
  }

  @Override
  public Response getResponse(NodeService nodeService) throws IOException {
    throw new UnsupportedOperationException("not defined");
  }
}
