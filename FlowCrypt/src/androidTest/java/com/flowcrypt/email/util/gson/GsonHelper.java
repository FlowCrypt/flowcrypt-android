/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.util.gson;

import com.flowcrypt.email.api.retrofit.response.model.node.MsgBlock;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * @author Denis Bondarenko
 * Date: 3/19/19
 * Time: 2:55 PM
 * E-mail: DenBond7@gmail.com
 */
public class GsonHelper {
  private static final GsonHelper ourInstance = new GsonHelper();
  private Gson gson;

  private GsonHelper() {
    GsonBuilder gsonBuilder = new GsonBuilder();
    gsonBuilder.registerTypeAdapter(MsgBlock.class, new MsgBlockAdapter());
    gson = gsonBuilder.create();
  }

  public static GsonHelper getInstance() {
    return ourInstance;
  }

  public Gson getGson() {
    return gson;
  }
}
