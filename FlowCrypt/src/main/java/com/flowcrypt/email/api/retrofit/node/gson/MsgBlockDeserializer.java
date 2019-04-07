/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.node.gson;

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
 * This realization helps parse a right variant of {@link MsgBlock}
 *
 * @author Denis Bondarenko
 * Date: 3/26/19
 * Time: 9:36 AM
 * E-mail: DenBond7@gmail.com
 */
public class MsgBlockDeserializer implements JsonDeserializer<MsgBlock> {
  @Override
  public MsgBlock deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
      throws JsonParseException {
    JsonObject jsonObject = json.getAsJsonObject();

    MsgBlock.Type type = context.deserialize(jsonObject.get(MsgBlock.TAG_TYPE), MsgBlock.Type.class);

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
