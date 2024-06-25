/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.jetpack.viewmodel

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.database.entity.KeyEntity
import com.flowcrypt.email.extensions.org.bouncycastle.openpgp.toPgpKeyRingDetails
import com.flowcrypt.email.model.KeyImportDetails
import com.flowcrypt.email.security.model.PgpKeyRingDetails
import com.flowcrypt.email.security.pgp.PgpKey
import com.flowcrypt.email.util.coroutines.runners.ControlledRunner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * @author Denys Bondarenko
 */
class CreatePrivateKeyViewModel(application: Application) : RoomBasicViewModel(application) {
  private val createPrivateKeyMutableStateFlow: MutableStateFlow<Result<PgpKeyRingDetails>> =
    MutableStateFlow(Result.none())
  val createPrivateKeyStateFlow: StateFlow<Result<PgpKeyRingDetails>> =
    createPrivateKeyMutableStateFlow.asStateFlow()
  private val controlledRunnerForKeyCreation = ControlledRunner<Result<PgpKeyRingDetails>>()

  fun createPrivateKey(
    accountEntity: AccountEntity,
    passphrase: String,
    passphraseType: KeyEntity.PassphraseType
  ) {
    viewModelScope.launch {
      createPrivateKeyMutableStateFlow.value = Result.loading()
      createPrivateKeyMutableStateFlow.value =
        controlledRunnerForKeyCreation.cancelPreviousThenRun {
          return@cancelPreviousThenRun withContext(Dispatchers.IO) {
            try {
              val pgpKeyRingDetails = PgpKey.create(
                email = accountEntity.email,
                name = accountEntity.displayName ?: accountEntity.email,
                passphrase = passphrase
              ).toPgpKeyRingDetails(
                accountEntity.clientConfiguration?.shouldHideArmorMeta() ?: false
              )

              Result.success(
                pgpKeyRingDetails.copy(
                  passphraseType = passphraseType,
                  tempPassphrase = passphrase.toCharArray(),
                  importInfo =
                  (pgpKeyRingDetails.importInfo ?: PgpKeyRingDetails.ImportInfo()).copy(
                    importSourceType = KeyImportDetails.SourceType.NEW
                  )
                )
              )
            } catch (e: Exception) {
              Result.exception(e)
            }
          }
        }
    }
  }
}
