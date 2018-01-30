/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (human@flowcrypt.com).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.util.google.gson;

import com.flowcrypt.email.service.actionqueue.actions.Action;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;

/**
 * //todo-DenBond7 I'll describe details later.
 *
 * @author Denis Bondarenko
 *         Date: 30.01.2018
 *         Time: 11:58
 *         E-mail: DenBond7@gmail.com
 */

public class ActionSerializerDeserializerAdapter implements JsonSerializer<Action>, JsonDeserializer<Action> {
    @Override
    public Action deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws
            JsonParseException {
        return null;
    }

    @Override
    public JsonElement serialize(Action src, Type typeOfSrc, JsonSerializationContext context) {
        return null;
    }
}
