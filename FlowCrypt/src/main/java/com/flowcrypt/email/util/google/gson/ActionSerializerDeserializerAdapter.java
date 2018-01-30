/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (human@flowcrypt.com).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.util.google.gson;

import com.flowcrypt.email.service.actionqueue.actions.Action;
import com.flowcrypt.email.service.actionqueue.actions.BackupPrivateKeyToInboxAction;
import com.google.gson.Gson;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;

/**
 * This class describes information how serialize and deserialize {@link Action} objects using {@link Gson} framework.
 *
 * @author Denis Bondarenko
 *         Date: 30.01.2018
 *         Time: 11:58
 *         E-mail: DenBond7@gmail.com
 */

public class ActionSerializerDeserializerAdapter implements JsonDeserializer<Action> {

    @Override
    public Action deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws
            JsonParseException {
        JsonObject jsonObject = json.getAsJsonObject();
        Action.ActionType type = Action.ActionType.valueOf(jsonObject.get(Action.TAG_NAME_ACTION_TYPE).getAsString());

        switch (type) {
            case BACKUP_PRIVATE_KEY_TO_INBOX:
                return context.deserialize(json, BackupPrivateKeyToInboxAction.class);

            default:
                throw new IllegalArgumentException("Unknown action type");
        }
    }
}
