/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.util.google.gson

import com.flowcrypt.email.service.actionqueue.actions.Action
import com.flowcrypt.email.service.actionqueue.actions.BackupPrivateKeyToInboxAction
import com.flowcrypt.email.service.actionqueue.actions.EncryptPrivateKeysIfNeededAction
import com.flowcrypt.email.service.actionqueue.actions.FillUserIdEmailsKeysTableAction
import com.flowcrypt.email.service.actionqueue.actions.LoadGmailAliasesAction
import com.flowcrypt.email.service.actionqueue.actions.RegisterUserPublicKeyAction
import com.flowcrypt.email.service.actionqueue.actions.SendWelcomeTestEmailAction
import com.google.gson.Gson
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import java.lang.reflect.Type

/**
 * This class describes information how serialize and deserialize [Action] objects using [Gson] framework.
 *
 * @author Denis Bondarenko
 * Date: 30.01.2018
 * Time: 11:58
 * E-mail: DenBond7@gmail.com
 */
class ActionJsonDeserializer : JsonDeserializer<Action> {

  override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): Action {
    val jsonObject = json.asJsonObject
    val type = Action.Type.valueOf(jsonObject.get(Action.TAG_NAME_ACTION_TYPE).asString)

    return when (type) {
      Action.Type.BACKUP_PRIVATE_KEY_TO_INBOX -> context.deserialize(json, BackupPrivateKeyToInboxAction::class.java)

      Action.Type.REGISTER_USER_PUBLIC_KEY -> context.deserialize(json, RegisterUserPublicKeyAction::class.java)

      Action.Type.SEND_WELCOME_TEST_EMAIL -> context.deserialize(json, SendWelcomeTestEmailAction::class.java)

      Action.Type.FILL_USER_ID_EMAILS_KEYS_TABLE ->
        context.deserialize(json, FillUserIdEmailsKeysTableAction::class.java)

      Action.Type.ENCRYPT_PRIVATE_KEYS -> context.deserialize(json, EncryptPrivateKeysIfNeededAction::class.java)

      Action.Type.LOAD_GMAIL_ALIASES -> context.deserialize(json, LoadGmailAliasesAction::class.java)
    }
  }
}
