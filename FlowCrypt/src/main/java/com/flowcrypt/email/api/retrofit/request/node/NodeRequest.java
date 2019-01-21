/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.request.node;

import android.content.Context;
import android.net.Uri;

import com.flowcrypt.email.api.retrofit.node.NodeService;

import java.io.IOException;

import retrofit2.Response;

/**
 * @author Denis Bondarenko
 * Date: 1/17/19
 * Time: 5:40 PM
 * E-mail: DenBond7@gmail.com
 */
public interface NodeRequest {
  String getEndpoint();

  byte[] getData();

  Uri getUri();

  Context getContext();

  Response getResponse(NodeService nodeService) throws IOException;
}
