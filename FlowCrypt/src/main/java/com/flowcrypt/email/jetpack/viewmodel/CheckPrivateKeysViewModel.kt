/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.jetpack.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.flowcrypt.email.R
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.database.entity.AccountSettingsEntity
import com.flowcrypt.email.extensions.org.pgpainless.util.asString
import com.flowcrypt.email.security.model.PgpKeyDetails
import com.flowcrypt.email.security.pgp.PgpKey
import com.flowcrypt.email.util.GeneralUtil
import com.flowcrypt.email.util.coroutines.runners.ControlledRunner
import com.flowcrypt.email.util.exception.WrongPassPhraseException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.pgpainless.exception.KeyIntegrityException
import org.pgpainless.util.Passphrase
import kotlin.math.min

/**
 * @author Denys Bondarenko
 */
class CheckPrivateKeysViewModel(
  application: Application,
  private val useAntiBruteforceProtection: Boolean = false,
) : AccountViewModel(application) {
  private val controlledRunnerForChecking = ControlledRunner<Result<List<CheckResult>>>()
  val checkPrvKeysLiveData: MutableLiveData<Result<List<CheckResult>>> = MutableLiveData()

  val antiBruteforceProtectionCountdownStateFlow = flow {
    while (useAntiBruteforceProtection && viewModelScope.isActive) {
      val accountSettings = getActiveAccountSettings() ?: continue
      val attemptsLimit = AccountSettingsEntity.ANTI_BRUTE_FORCE_PROTECTION_ATTEMPTS_MAX_VALUE

      if (accountSettings.checkPassPhraseAttemptsCount < attemptsLimit) {
        if (accountSettings.checkPassPhraseAttemptsCount > 0 &&
          System.currentTimeMillis() - accountSettings.lastUnsuccessfulCheckPassPhraseAttemptTime
          >= AccountSettingsEntity.RESET_COUNT_TIME_IN_MILLISECONDS
        ) {
          //reset the attempts count after 5 minutes of inactivity
          roomDatabase.accountSettingsDao()
            .updateSuspend(accountSettings.copy(checkPassPhraseAttemptsCount = 0))
          emit(Pair(0, 0L))
        } else {
          emit(Pair(accountSettings.checkPassPhraseAttemptsCount, 0L))
        }
      } else {
        val endTime = accountSettings.lastUnsuccessfulCheckPassPhraseAttemptTime +
            AccountSettingsEntity.BLOCKING_TIME_IN_MILLISECONDS
        val timeLeftToUnblocking = maxOf(0, endTime - System.currentTimeMillis())
        if (timeLeftToUnblocking == 0L && accountSettings.checkPassPhraseAttemptsCount ==
          attemptsLimit
        ) {
          //reset the attempts count after the blocking time
          roomDatabase.accountSettingsDao()
            .updateSuspend(accountSettings.copy(checkPassPhraseAttemptsCount = 0))
          emit(Pair(0, timeLeftToUnblocking))
        } else {
          emit(Pair(attemptsLimit, timeLeftToUnblocking))
        }
      }

      delay(500)
    }
  }.distinctUntilChanged().stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = Pair(0, 0L)
  )

  fun checkKeys(keys: List<PgpKeyDetails>, passphrase: Passphrase) {
    viewModelScope.launch {
      if (useAntiBruteforceProtection) {
        if (AccountSettingsEntity.ANTI_BRUTE_FORCE_PROTECTION_ATTEMPTS_MAX_VALUE ==
          getActiveAccountSettings()?.checkPassPhraseAttemptsCount
        ) {
          return@launch
        }
      }

      if (checkPrvKeysLiveData.value?.status != Result.Status.LOADING) {
        checkPrvKeysLiveData.value = Result.loading()
      }
      if (passphrase.isEmpty) {
        checkPrvKeysLiveData.value = Result.success(emptyList())
        return@launch
      }
      checkPrvKeysLiveData.value = controlledRunnerForChecking.cancelPreviousThenRun {
        return@cancelPreviousThenRun Result.success(checkKeysInternal(keys, passphrase))
      }
    }
  }

  private suspend fun checkKeysInternal(keys: List<PgpKeyDetails>, passphrase: Passphrase):
      List<CheckResult> =
    withContext(Dispatchers.IO) {
      val context: Context = getApplication()
      val resultList = mutableListOf<CheckResult>()
      for (keyDetails in keys) {
        val copy = keyDetails.copy()
        var e: Throwable? = null
        if (copy.isPrivate) {
          if (copy.passphraseType == null) {
            e = IllegalArgumentException(context.getString(R.string.passphrase_type_undefined))
          } else {
            val prvKey = copy.privateKey
            if (prvKey.isNullOrEmpty()) {
              e = IllegalArgumentException("Empty source")
            } else {
              if (copy.isFullyDecrypted) {
                copy.tempPassphrase = passphrase.chars
              } else {
                try {
                  PgpKey.decryptKey(prvKey, passphrase)
                  //https://github.com/FlowCrypt/flowcrypt-android/issues/1669
                  PgpKey.checkSecretKeyIntegrity(prvKey, passphrase)
                  copy.tempPassphrase = passphrase.chars
                } catch (ex: Throwable) {
                  //to prevent leak sensitive info we skip printing stack trace for release builds
                  if (GeneralUtil.isDebugBuild()) {
                    ex.printStackTrace()
                  }

                  e = when (ex) {
                    is KeyIntegrityException -> ex

                    else -> WrongPassPhraseException(
                      message = context.getString(R.string.password_is_incorrect), cause = ex
                    )
                  }
                }
              }
            }
          }
        } else {
          e = IllegalArgumentException(context.getString(R.string.not_private_key))
        }

        resultList.add(
          CheckResult(
            pgpKeyDetails = copy,
            passphrase = passphrase.asString ?: throw IllegalArgumentException(),
            e = e
          )
        )

        if (useAntiBruteforceProtection) {
          val accountSettings = getActiveAccountSettings() ?: return@withContext resultList
          updateAccountSettingsIfNeeded(resultList, accountSettings)
        }
      }
      return@withContext resultList
    }

  private suspend fun updateAccountSettingsIfNeeded(
    resultList: MutableList<CheckResult>,
    accountSettings: AccountSettingsEntity
  ): Unit = withContext(Dispatchers.IO) {
    val hasCorrectPassPhrase = resultList.any { it.e == null }
    if (hasCorrectPassPhrase) {
      roomDatabase.accountSettingsDao().updateSuspend(
        accountSettings.copy(checkPassPhraseAttemptsCount = 0)
      )
    } else {
      val currentCheckPassPhraseAttemptsCount = accountSettings.checkPassPhraseAttemptsCount
      if (currentCheckPassPhraseAttemptsCount !=
        AccountSettingsEntity.ANTI_BRUTE_FORCE_PROTECTION_ATTEMPTS_MAX_VALUE
      ) {
        roomDatabase.accountSettingsDao().updateSuspend(
          accountSettings.copy(
            checkPassPhraseAttemptsCount = min(
              currentCheckPassPhraseAttemptsCount + 1,
              AccountSettingsEntity.ANTI_BRUTE_FORCE_PROTECTION_ATTEMPTS_MAX_VALUE
            ),
            lastUnsuccessfulCheckPassPhraseAttemptTime = System.currentTimeMillis()
          )
        )
      }
    }
  }

  private suspend fun getActiveAccountSettings() = withContext(Dispatchers.IO) {
    val activeAccount = getActiveAccountSuspend() ?: return@withContext null
    val existingAccountSettings = roomDatabase.accountSettingsDao()
      .getAccountSettings(
        account = activeAccount.email,
        accountType = activeAccount.accountType
      )

    return@withContext existingAccountSettings ?: run {
      val newAccountSettings = AccountSettingsEntity(
        account = activeAccount.email,
        accountType = activeAccount.accountType
      )
      newAccountSettings.copy(
        id = roomDatabase.accountSettingsDao().insertSuspend(newAccountSettings)
      )
    }
  }

  data class CheckResult(
    val pgpKeyDetails: PgpKeyDetails,
    val passphrase: String, val e: Throwable? = null
  )
}
