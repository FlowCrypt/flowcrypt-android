package com.cryptup.api.gson.deserializer;

import com.cryptup.api.retrofit.response.MessagePrototypeResponse;
import com.cryptup.api.retrofit.response.model.MessagePrototypeError;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;

/**
 * This is a custom JsonDeserializer to deserialize MessagePrototypeResponse objects.
 *
 * @author Denis Bondarenko
 *         Date: 20.10.2016
 *         Time: 10:19
 *         E-mail: DenBond7@gmail.com
 */
public class MessagePrototypeResponseJsonDeserializer implements
        JsonDeserializer<MessagePrototypeResponse> {

    @Override
    public MessagePrototypeResponse deserialize(JsonElement json, Type typeOfT,
                                                JsonDeserializationContext context) throws
            JsonParseException {
        MessagePrototypeResponse messagePrototypeResponse = new MessagePrototypeResponse();

        JsonObject rootJsonObject = json.getAsJsonObject();

        if (rootJsonObject.has(MessagePrototypeResponse.GSON_KEY_SENT)
                && rootJsonObject.get(MessagePrototypeResponse.GSON_KEY_SENT).isJsonPrimitive()) {
            messagePrototypeResponse.setSent(rootJsonObject.getAsJsonPrimitive
                    (MessagePrototypeResponse.GSON_KEY_SENT).getAsBoolean());
        }

        if (rootJsonObject.has(MessagePrototypeResponse.GSON_KEY_ERROR)
                && rootJsonObject.get(MessagePrototypeResponse.GSON_KEY_ERROR)
                .isJsonPrimitive()) {
            messagePrototypeResponse.setError(rootJsonObject.getAsJsonPrimitive
                    (MessagePrototypeResponse.GSON_KEY_ERROR).getAsString());
        }

        if (rootJsonObject.has(MessagePrototypeResponse.GSON_KEY_ERROR)
                && rootJsonObject.get(MessagePrototypeResponse.GSON_KEY_ERROR).isJsonObject()) {
            JsonObject errorObject = rootJsonObject.getAsJsonObject(MessagePrototypeResponse
                    .GSON_KEY_ERROR);

            messagePrototypeResponse.setMessagePrototypeError((MessagePrototypeError) context
                    .deserialize(errorObject, MessagePrototypeError.class));
        }

        return messagePrototypeResponse;
    }
}
