/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.database.entity

import android.provider.BaseColumns
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.flowcrypt.email.service.actionqueue.actions.Action
import com.flowcrypt.email.util.google.gson.ActionJsonDeserializer
import com.google.gson.Gson
import com.google.gson.GsonBuilder

/**
 * @author Denys Bondarenko
 */
@Entity(tableName = "action_queue")
data class ActionQueueEntity(
  @PrimaryKey(autoGenerate = true) @ColumnInfo(name = BaseColumns._ID) val id: Long? = null,
  val email: String,
  @ColumnInfo(name = "action_type") val actionType: String,
  @ColumnInfo(name = "action_json") val actionJson: String
) {

  fun toAction(): Action? {
    val gson: Gson =
      GsonBuilder().registerTypeAdapter(Action::class.java, ActionJsonDeserializer()).create()
    val action = gson.fromJson(actionJson, Action::class.java)
    return if (action != null) {
      action.id = id ?: 0
      action
    } else {
      null
    }
  }

  companion object {
    fun fromAction(action: Action): ActionQueueEntity? {
      val email = action.email ?: return null
      val gson: Gson =
        GsonBuilder().registerTypeAdapter(Action::class.java, ActionJsonDeserializer()).create()
      return ActionQueueEntity(
        email = email.lowercase(),
        actionType = action.type.value,
        actionJson = gson.toJson(action)
      )
    }
  }
}
