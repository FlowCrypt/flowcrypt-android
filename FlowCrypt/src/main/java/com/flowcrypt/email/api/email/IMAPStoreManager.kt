/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.switchMap
import com.flowcrypt.email.database.FlowCryptRoomDatabase
import com.flowcrypt.email.database.entity.AccountEntity
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * @author Denys Bondarenko
 */
object IMAPStoreManager {
  private val RETRY_TIMEOUT = TimeUnit.MILLISECONDS.toMillis(200)
  private val activeConnections = ConcurrentHashMap<Long, IMAPStoreConnection>()

  @OptIn(DelicateCoroutinesApi::class)
  fun init(context: Context) {
    val applicationContext = context.applicationContext
    val roomDatabase = FlowCryptRoomDatabase.getDatabase(applicationContext)

    val pureActiveAccountLiveData: LiveData<AccountEntity?> =
      roomDatabase.accountDao().getActiveAccountLD()
    val activeAccountLiveData: LiveData<AccountEntity?> =
      pureActiveAccountLiveData.switchMap { accountEntity ->
        liveData {
          emit(accountEntity?.withDecryptedInfo())
        }
      }

    activeAccountLiveData.observeForever { activeAccount ->
      GlobalScope.launch(Dispatchers.IO) {
        EmailUtil.patchingSecurityProviderSuspend(context)
        //check if we have not-registered connections and close them
        val iterator = activeConnections.iterator()
        while (iterator.hasNext()) {
          val connection = iterator.next().value
          if (roomDatabase.accountDao().getAccount(connection.accountEntity.email) == null) {
            try {
              connection.disconnect()
            } catch (e: Exception) {
              e.printStackTrace()
            }
          }
        }

        activeAccount?.let {
          val key = it.id ?: -1
          //stop and create a new one
          val existedActiveAccountIMAPStoreConnection = activeConnections[key]
          try {
            existedActiveAccountIMAPStoreConnection?.disconnect()
          } catch (e: Exception) {
            e.printStackTrace()
          }
          activeConnections.remove(key, existedActiveAccountIMAPStoreConnection)

          if (!activeAccount.useAPI) {
            val newActiveAccountIMAPStoreConnection = IMAPStoreConnection(applicationContext, it)
            activeConnections[key] = newActiveAccountIMAPStoreConnection
            try {
              newActiveAccountIMAPStoreConnection.connect()
            } catch (e: Exception) {
              e.printStackTrace()
            }
          }
        }
      }
    }
  }

  suspend fun getConnection(id: Long?, retryAttempts: Int = 5) = withContext(Dispatchers.IO) {
    id ?: return@withContext null
    var attempts = retryAttempts
    if (attempts > 0) {
      while (attempts != 0 && activeConnections[id] == null) {
        attempts--
        delay(RETRY_TIMEOUT)
      }
    }
    return@withContext activeConnections[id]
  }
}
