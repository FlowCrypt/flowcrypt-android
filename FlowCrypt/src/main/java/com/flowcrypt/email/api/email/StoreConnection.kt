/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email

import android.content.Context
import androidx.annotation.WorkerThread
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.database.entity.AccountEntity
import javax.mail.Session

/**
 * That's an interface which helps to manage an instance of [javax.mail.Store]. All of methods
 * should be suspended and should be run out of the main thread.
 *
 * @author Denis Bondarenko
 *         Date: 11/20/20
 *         Time: 11:38 AM
 *         E-mail: DenBond7@gmail.com
 */
interface StoreConnection {
  val context: Context
  val session: Session
  val accountEntity: AccountEntity

  @WorkerThread
  suspend fun connect(): Boolean

  @WorkerThread
  suspend fun reconnect(): Boolean

  @WorkerThread
  suspend fun disconnect(): Boolean

  @WorkerThread
  suspend fun isConnected(): Boolean

  @WorkerThread
  /**
   * That's a helper method that helps execute some code and returns [Result]
   */
  suspend fun <T> execute(action: suspend () -> Result<T>): Result<T>
}