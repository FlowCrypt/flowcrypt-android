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
import com.flowcrypt.email.extensions.org.bouncycastle.openpgp.toPgpKeyDetails
import com.flowcrypt.email.model.KeyImportDetails
import com.flowcrypt.email.security.model.PgpKeyDetails
import com.flowcrypt.email.util.coroutines.runners.ControlledRunner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.pgpainless.PGPainless
import org.pgpainless.key.util.UserId

/**
 * @author Denys Bondarenko
 */
class CreatePrivateKeyViewModel(application: Application) : RoomBasicViewModel(application) {
  private val createPrivateKeyMutableStateFlow: MutableStateFlow<Result<PgpKeyDetails>> =
    MutableStateFlow(Result.none())
  val createPrivateKeyStateFlow: StateFlow<Result<PgpKeyDetails>> =
    createPrivateKeyMutableStateFlow.asStateFlow()
  private val controlledRunnerForKeyCreation = ControlledRunner<Result<PgpKeyDetails>>()

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
              val pgpKeyDetails = PGPainless.generateKeyRing().simpleEcKeyRing(
                UserId.nameAndEmail(
                  accountEntity.displayName
                    ?: accountEntity.email, accountEntity.email
                ), passphrase
              ).toPgpKeyDetails(accountEntity.clientConfiguration?.shouldHideArmorMeta() ?: false)
                .copy(
                  passphraseType = passphraseType,
                  tempPassphrase = passphrase.toCharArray(),
                  importSourceType = KeyImportDetails.SourceType.NEW
                )

              Result.success(pgpKeyDetails)
            } catch (e: Exception) {
              Result.exception(e)
            }
          }
        }
    }
  }
}
