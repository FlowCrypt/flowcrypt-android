/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.database.entity

import android.provider.BaseColumns
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * @author Denis Bondarenko
 *         Date: 12/5/19
 *         Time: 6:27 PM
 *         E-mail: DenBond7@gmail.com
 */
@Entity(tableName = "action_queue")
data class ActionQueueEntity(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = BaseColumns._ID) val id: Long?,
    val email: String,
    @ColumnInfo(name = "action_type") val actionType: String,
    @ColumnInfo(name = "action_json") val actionJson: String)