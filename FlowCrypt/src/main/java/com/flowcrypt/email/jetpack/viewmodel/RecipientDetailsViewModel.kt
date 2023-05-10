/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.jetpack.viewmodel

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.flowcrypt.email.database.entity.PublicKeyEntity
import com.flowcrypt.email.database.entity.RecipientEntity
import com.flowcrypt.email.security.pgp.PgpKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext

/**
 * @author Denys Bondarenko
 */
class RecipientDetailsViewModel(
  recipientEntity: RecipientEntity, application: Application
) : AccountViewModel(application) {
  private val pureRecipientPubKeysFlow =
    roomDatabase.pubKeyDao().getPublicKeysByRecipientFlow(recipientEntity.email)

  @ExperimentalCoroutinesApi
  val recipientPubKeysFlow: StateFlow<List<PublicKeyEntity>?> =
    pureRecipientPubKeysFlow.mapLatest { fullList ->
      fullList.forEach {
        withContext(Dispatchers.IO) {
          it.pgpKeyDetails = try {
            PgpKey.parseKeys(
              source = it.publicKey,
              throwExceptionIfUnknownSource = false
            ).pgpKeyDetailsList.firstOrNull()
          } catch (e: Exception) {
            e.printStackTrace()
            null
          }
        }
      }

      fullList
    }.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = null
    )
}
