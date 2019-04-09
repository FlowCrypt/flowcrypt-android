/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.util.gson;

import com.flowcrypt.email.api.retrofit.response.model.node.BaseMsgBlock;
import com.flowcrypt.email.api.retrofit.response.model.node.DecryptErrorMsgBlock;
import com.flowcrypt.email.api.retrofit.response.model.node.MsgBlock;
import com.flowcrypt.email.api.retrofit.response.model.node.PublicKeyMsgBlock;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;

/**
 * @author Denis Bondarenko
 * Date: 3/19/19
 * Time: 2:58 PM
 * E-mail: DenBond7@gmail.com
 */
public class MsgBlockAdapter implements JsonDeserializer<MsgBlock> {
  @Override
  public MsgBlock deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
      throws JsonParseException {
    JsonObject jsonObject = json.getAsJsonObject();

    MsgBlock.Type type = context.deserialize(jsonObject.get("type"), MsgBlock.Type.class);

    if (type == null) {
      return null;
    }

    switch (type) {
      case PUBLIC_KEY:
        return context.deserialize(json, PublicKeyMsgBlock.class);

      case DECRYPT_ERROR:
        return context.deserialize(json, DecryptErrorMsgBlock.class);

      default:
        return context.deserialize(json, BaseMsgBlock.class);
    }
  }
}
