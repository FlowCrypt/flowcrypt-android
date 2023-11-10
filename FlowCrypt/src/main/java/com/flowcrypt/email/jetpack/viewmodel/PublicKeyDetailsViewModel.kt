/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.jetpack.viewmodel

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.database.entity.PublicKeyEntity
import com.flowcrypt.email.security.pgp.PgpKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import org.pgpainless.key.info.KeyRingInfo

/**
 * @author Denys Bondarenko
 */
class PublicKeyDetailsViewModel(
  publicKeyEntity: PublicKeyEntity, application: Application
) : AccountViewModel(application) {

  private val publicKeyEntityFlow =
    roomDatabase.pubKeyDao().getPublicKeyByIdFlow(publicKeyEntity.id ?: -1)

  @OptIn(ExperimentalCoroutinesApi::class)
  val publicKeyEntityStateFlow = publicKeyEntityFlow.mapLatest {
    Result.success(it)
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = Result.none()
  )

  @ExperimentalCoroutinesApi
  val keyRingInfoStateFlow: StateFlow<Result<KeyRingInfo?>> =
    publicKeyEntityFlow.flatMapLatest { publicKeyEntity ->
      flow {
        emit(Result.loading())
        try {
          val activeAccount = getActiveAccountSuspend()
          val keyRing = withContext(Dispatchers.IO) {
            PgpKey.parseKeys(
              source = publicKeyEntity?.publicKey ?: byteArrayOf(),
              throwExceptionIfUnknownSource = false,
              hideArmorMeta = activeAccount?.clientConfiguration?.shouldHideArmorMeta() ?: false
            ).getAllKeys().firstOrNull()
          }
          emit(Result.success(keyRing?.let { KeyRingInfo(keyRing) }))
        } catch (e: Exception) {
          emit(Result.exception(e))
        }
      }
    }.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = Result.none()
    )
}
