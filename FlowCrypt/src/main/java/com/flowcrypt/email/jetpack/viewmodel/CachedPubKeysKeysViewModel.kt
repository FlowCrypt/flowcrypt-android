/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.jetpack.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.viewModelScope
import com.flowcrypt.email.R
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.database.entity.PublicKeyEntity
import com.flowcrypt.email.database.entity.RecipientEntity
import com.flowcrypt.email.security.model.PgpKeyDetails
import com.flowcrypt.email.security.pgp.PgpKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * @author Denys Bondarenko
 */
class CachedPubKeysKeysViewModel(application: Application) : AccountViewModel(application) {
  private val allPubKeysFlow = roomDatabase.pubKeyDao().getAllPublicKeysFlow()
  private val setOfRecipientsAndFingerprintsMutableStateFlow =
    MutableStateFlow<Set<String>>(emptySet())
  private val setOfRecipientsAndFingerprintsStateFlow =
    setOfRecipientsAndFingerprintsMutableStateFlow.asStateFlow()

  @OptIn(ExperimentalCoroutinesApi::class)
  private val automaticallyFetchedFromDatabasePubKeysStateFlow =
    allPubKeysFlow.flatMapLatest { fullList ->
      flow {
        val filteredList = fullList.filter {
          (it.recipient + it.fingerprint) in setOfRecipientsAndFingerprintsStateFlow.value
        }
        filteredList.forEach {
          withContext(Dispatchers.IO) {
            val result = PgpKey.parseKeys(
              source = it.publicKey,
              throwExceptionIfUnknownSource = false
            ).pgpKeyDetailsList
            it.pgpKeyDetails = result.firstOrNull()
          }
        }
        emit(filteredList.associateBy({ it.recipient + it.fingerprint }, { it }))
      }
    }.stateIn(
      scope = viewModelScope,
      started = WhileSubscribed(5000),
      initialValue = emptyMap()
    )

  @OptIn(ExperimentalCoroutinesApi::class)
  private val manuallyFetchedFromDatabasePubKeysStateFlow =
    setOfRecipientsAndFingerprintsStateFlow.flatMapLatest { set ->
      allPubKeysFlow.flatMapLatest { fullList ->
        flow {
          val filteredList = fullList.filter {
            (it.recipient + it.fingerprint) in set
          }
          filteredList.forEach {
            withContext(Dispatchers.IO) {
              val result = PgpKey.parseKeys(
                source = it.publicKey,
                throwExceptionIfUnknownSource = false
              ).pgpKeyDetailsList
              it.pgpKeyDetails = result.firstOrNull()
            }
          }
          emit(filteredList.associateBy({ it.recipient + it.fingerprint }, { it }))
        }
      }
    }.stateIn(
      scope = viewModelScope,
      started = WhileSubscribed(5000),
      initialValue = emptyMap()
    )

  val filteredPubKeysStateFlow = combine(
    automaticallyFetchedFromDatabasePubKeysStateFlow,
    manuallyFetchedFromDatabasePubKeysStateFlow
  ) { a, b -> a + b }

  private val addPubKeysMutableStateFlow =
    MutableStateFlow<Result<Boolean?>>(Result.none())
  val addPubKeysStateFlow =
    addPubKeysMutableStateFlow.asStateFlow()

  private val updateExistingPubKeyMutableStateFlow =
    MutableStateFlow<Result<Boolean?>>(Result.none())
  val updateExistingPubKeyStateFlow =
    updateExistingPubKeyMutableStateFlow.asStateFlow()

  private val importAllPubKeysMutableStateFlow =
    MutableStateFlow<Result<Boolean?>>(Result.none())
  val importAllPubKeysPubKeyStateFlow =
    importAllPubKeysMutableStateFlow.asStateFlow()

  fun specifyFilter(list: Collection<PgpKeyDetails>) {
    val keys = mutableSetOf<String>()
    for (pgpKeyDetails in list) {
      val email = pgpKeyDetails.getPrimaryInternetAddress()?.address?.lowercase() ?: continue
      val fingerprint = pgpKeyDetails.fingerprint.uppercase()
      keys.add(email + fingerprint)
    }
    setOfRecipientsAndFingerprintsMutableStateFlow.value = keys
  }

  fun addPubKeysBasedOnPgpKeyDetails(pgpKeyDetails: PgpKeyDetails) {
    viewModelScope.launch {
      addPubKeysMutableStateFlow.value = Result.loading()
      val context: Context = getApplication()

      val primaryAddress = pgpKeyDetails.mimeAddresses.firstOrNull()?.address?.lowercase()
      if (primaryAddress == null) {
        addPubKeysMutableStateFlow.value = Result.exception(
          IllegalStateException(context.getString(R.string.primary_address_not_defined))
        )
        return@launch
      }
      val contact = roomDatabase.recipientDao().getRecipientByEmailSuspend(primaryAddress)
      if (contact == null) {
        val isInserted =
          roomDatabase.recipientDao().insertSuspend(RecipientEntity(email = primaryAddress)) > 0
        if (!isInserted) {
          addPubKeysMutableStateFlow.value = Result.exception(
            IllegalStateException(context.getString(R.string.could_not_save_new_recipient))
          )
          return@launch
        }
      }

      val publicKeyEntity = pgpKeyDetails.toPublicKeyEntity(primaryAddress)

      val existingPublicKeyEntity = roomDatabase.pubKeyDao().getPublicKeyByRecipientAndFingerprint(
        publicKeyEntity.recipient, publicKeyEntity.fingerprint
      )

      if (existingPublicKeyEntity != null) {
        addPubKeysMutableStateFlow.value = Result.exception(
          IllegalStateException(context.getString(R.string.key_has_already_been_added))
        )
        return@launch
      }

      val isPubKeySaved = roomDatabase.pubKeyDao().insertSuspend(publicKeyEntity) > 0
      if (isPubKeySaved) {
        addPubKeysMutableStateFlow.value = Result.success(true)
      } else {
        addPubKeysMutableStateFlow.value = Result.exception(
          IllegalStateException(
            context.getString(
              R.string.could_not_save_pub_key_for_recipient,
              publicKeyEntity.fingerprint,
              publicKeyEntity.recipient
            )
          )
        )
      }
    }
  }

  fun updateExistingPubKey(pgpKeyDetails: PgpKeyDetails, existingPublicKeyEntity: PublicKeyEntity) {
    viewModelScope.launch {
      updateExistingPubKeyMutableStateFlow.value = Result.loading()
      val context: Context = getApplication()
      try {
        if (existingPublicKeyEntity.pgpKeyDetails == null) {
          existingPublicKeyEntity.pgpKeyDetails =
            PgpKey.parseKeys(source = existingPublicKeyEntity.publicKey)
              .pgpKeyDetailsList.firstOrNull()
        }

        when {
          existingPublicKeyEntity.pgpKeyDetails?.isRevoked == true -> {
            updateExistingPubKeyMutableStateFlow.value = Result.exception(
              IllegalStateException(context.getString(R.string.key_is_revoked_unable_to_update))
            )
          }

          pgpKeyDetails.isNewerThan(existingPublicKeyEntity.pgpKeyDetails) -> {
            val publicKeyEntity =
              existingPublicKeyEntity.copy(publicKey = pgpKeyDetails.publicKey.toByteArray())

            val isPubKeyUpdated = roomDatabase.pubKeyDao().updateSuspend(publicKeyEntity) > 0
            if (isPubKeyUpdated) {
              updateExistingPubKeyMutableStateFlow.value = Result.success(true)
            } else {
              updateExistingPubKeyMutableStateFlow.value = Result.exception(
                IllegalStateException(
                  context.getString(
                    R.string.could_not_update_pub_key_for_recipient,
                    publicKeyEntity.recipient
                  )
                )
              )
            }
          }

          else -> {
            updateExistingPubKeyMutableStateFlow.value = Result.exception(
              IllegalStateException(
                context.getString(R.string.you_trying_replace_pub_key_with_older_version)
              )
            )
          }
        }
      } catch (e: Exception) {
        updateExistingPubKeyMutableStateFlow.value = Result.exception(e)
      }
    }
  }

  fun importAllPubKeysWithConflictResolution(list: Collection<PgpKeyDetails>) {
    viewModelScope.launch {
      val context: Context = getApplication()
      importAllPubKeysMutableStateFlow.value =
        Result.loading(progressMsg = context.getString(R.string.importing_public_keys))

      var progress: Float
      var lastProgress = 0f
      val totalOperationsCount = list.size

      for ((index, pgpKeyDetails) in list.withIndex()) {
        try {
          val primaryAddress =
            pgpKeyDetails.mimeAddresses.firstOrNull()?.address?.lowercase() ?: continue
          val fingerprint = pgpKeyDetails.fingerprint

          val existingPublicKeyEntity = roomDatabase.pubKeyDao()
            .getPublicKeyByRecipientAndFingerprint(primaryAddress, fingerprint)

          if (existingPublicKeyEntity == null) {
            val isNewRecipientAdded =
              if (roomDatabase.recipientDao().getRecipientByEmailSuspend(primaryAddress) == null) {
                roomDatabase.recipientDao()
                  .insertSuspend(RecipientEntity(email = primaryAddress)) > 0
              } else true
            if (isNewRecipientAdded) {
              roomDatabase.pubKeyDao()
                .insertSuspend(pgpKeyDetails.toPublicKeyEntity(primaryAddress))
            }
          } else {
            val existingPgpKeyDetails = withContext(Dispatchers.IO) {
              val result = PgpKey.parseKeys(
                source = pgpKeyDetails.publicKey,
                throwExceptionIfUnknownSource = false
              ).pgpKeyDetailsList
              result.firstOrNull()
            } ?: continue
            val isExistingKeyRevoked = existingPgpKeyDetails.isRevoked
            if (!isExistingKeyRevoked && pgpKeyDetails.isNewerThan(existingPgpKeyDetails)) {
              roomDatabase.pubKeyDao().updateSuspend(
                existingPublicKeyEntity.copy(publicKey = pgpKeyDetails.publicKey.toByteArray())
              )
            }
          }
        } catch (e: Exception) {
          //skip errors for now
        }
        progress = index * 100f / totalOperationsCount
        if (progress - lastProgress >= 1) {
          importAllPubKeysMutableStateFlow.value = Result.loading(
            progressMsg = context.getString(R.string.processing),
            progress = progress.toDouble()
          )
          lastProgress = progress
        }
      }

      importAllPubKeysMutableStateFlow.value = Result.loading(progress = 100.0)
      importAllPubKeysMutableStateFlow.value = Result.success(true)
    }
  }
}
