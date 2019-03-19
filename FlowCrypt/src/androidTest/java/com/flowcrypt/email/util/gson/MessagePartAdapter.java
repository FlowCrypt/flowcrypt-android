/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.util.gson;

import com.flowcrypt.email.model.messages.MessagePart;
import com.flowcrypt.email.model.messages.MessagePartAttestPacket;
import com.flowcrypt.email.model.messages.MessagePartPgpMessage;
import com.flowcrypt.email.model.messages.MessagePartPgpPasswordMessage;
import com.flowcrypt.email.model.messages.MessagePartPgpPublicKey;
import com.flowcrypt.email.model.messages.MessagePartSignedMessage;
import com.flowcrypt.email.model.messages.MessagePartText;
import com.flowcrypt.email.model.messages.MessagePartType;
import com.flowcrypt.email.model.messages.MessagePartVerification;
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
public class MessagePartAdapter implements JsonDeserializer<MessagePart> {
  @Override
  public MessagePart deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
    JsonObject jsonObject = json.getAsJsonObject();


    MessagePartType messagePartType = context.deserialize(jsonObject.get("msgPartType"), MessagePartType.class);

    switch (messagePartType) {
      case TEXT:
        return context.deserialize(json, MessagePartText.class);

      case PGP_MESSAGE:
        return context.deserialize(json, MessagePartPgpMessage.class);

      case PGP_PUBLIC_KEY:
        return context.deserialize(json, MessagePartPgpPublicKey.class);

      case PGP_SIGNED_MESSAGE:
        return context.deserialize(json, MessagePartSignedMessage.class);

      case VERIFICATION:
        return context.deserialize(json, MessagePartVerification.class);

      case ATTEST_PACKET:
        return context.deserialize(json, MessagePartAttestPacket.class);

      case PGP_PASSWORD_MESSAGE:
        return context.deserialize(json, MessagePartPgpPasswordMessage.class);

      default:
        throw new AssertionError("An unknown " + MessagePart.class.getSimpleName());
    }
  }
}
