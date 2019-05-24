/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.node.gson;

import com.flowcrypt.email.api.retrofit.response.model.node.MsgBlock;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * This class describes creating {@link Gson} for Node.
 *
 * @author Denis Bondarenko
 * Date: 1/15/19
 * Time: 2:08 PM
 * E-mail: DenBond7@gmail.com
 */
public final class NodeGson {
  private static final NodeGson INSTANCE = new NodeGson();
  private Gson gson;

  private NodeGson() {
    gson = new GsonBuilder()
        .registerTypeAdapter(MsgBlock.class, new MsgBlockDeserializer())
        .excludeFieldsWithoutExposeAnnotation()
        .create();
  }

  public static NodeGson getInstance() {
    return INSTANCE;
  }

  public Gson getGson() {
    return gson;
  }
}
