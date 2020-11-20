/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email

import android.content.Context
import androidx.annotation.WorkerThread
import com.flowcrypt.email.database.entity.AccountEntity

/**
 * @author Denis Bondarenko
 *         Date: 11/20/20
 *         Time: 11:38 AM
 *         E-mail: DenBond7@gmail.com
 */
interface StoreConnection {
  val context: Context
  val accountEntity: AccountEntity

  @WorkerThread
  suspend fun connect(): Boolean

  @WorkerThread
  suspend fun reconnect(): Boolean

  @WorkerThread
  suspend fun disconnect(): Boolean

  @WorkerThread
  suspend fun isConnected(): Boolean
}