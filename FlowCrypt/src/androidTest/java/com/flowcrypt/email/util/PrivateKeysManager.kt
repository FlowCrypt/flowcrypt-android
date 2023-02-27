/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.util

import androidx.test.platform.app.InstrumentationRegistry
import com.flowcrypt.email.database.FlowCryptRoomDatabase
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.database.entity.KeyEntity
import com.flowcrypt.email.model.KeyImportDetails
import com.flowcrypt.email.security.KeyStoreCryptoManager
import com.flowcrypt.email.security.model.PgpKeyDetails
import com.flowcrypt.email.security.pgp.PgpKey
import org.pgpainless.key.collection.PGPKeyRingCollection
import java.util.ArrayList

/**
 * This tool can help manage private keys in the database. For testing purposes only.
 *
 * @author Denys Bondarenko
 */
class PrivateKeysManager {
  companion object {
    fun saveKeyFromAssetsToDatabase(
      accountEntity: AccountEntity,
      keyPath: String,
      passphrase: String,
      sourceType: KeyImportDetails.SourceType,
      passphraseType: KeyEntity.PassphraseType = KeyEntity.PassphraseType.DATABASE
    ) {
      val pgpKeyDetails = getPgpKeyDetailsFromAssets(keyPath)
      saveKeyToDatabase(accountEntity, pgpKeyDetails, passphrase, sourceType, passphraseType)
    }

    fun saveKeyToDatabase(
      accountEntity: AccountEntity,
      pgpKeyDetails: PgpKeyDetails,
      passphrase: String?,
      sourceType: KeyImportDetails.SourceType = KeyImportDetails.SourceType.EMAIL,
      passphraseType: KeyEntity.PassphraseType = KeyEntity.PassphraseType.DATABASE
    ) {
      val context = InstrumentationRegistry.getInstrumentation().targetContext
      val roomDatabase = FlowCryptRoomDatabase.getDatabase(context)
      val keyEntity = pgpKeyDetails
        .copy(passphraseType = passphraseType)
        .toKeyEntity(accountEntity)
        .copy(
          source = sourceType.toString(),
          privateKey = KeyStoreCryptoManager.encrypt(pgpKeyDetails.privateKey).toByteArray(),
          storedPassphrase = KeyStoreCryptoManager.encrypt(passphrase)
        )
      val existingKey = roomDatabase.keysDao().getKeyByAccountAndFingerprint(
        accountEntity.email,
        pgpKeyDetails.fingerprint
      )
      existingKey?.let { roomDatabase.keysDao().delete(it) }
      roomDatabase.keysDao().insert(keyEntity)
      // Added timeout for a better sync between threads.
      Thread.sleep(3000)
    }

    fun savePubKeyToDatabase(assetsPath: String) {
      savePubKeyToDatabase(getPgpKeyDetailsFromAssets(assetsPath))
    }

    fun savePubKeyToDatabase(pgpKeyDetails: PgpKeyDetails) {
      val context = InstrumentationRegistry.getInstrumentation().targetContext
      val email = requireNotNull(pgpKeyDetails.getPrimaryInternetAddress()).address.lowercase()
      val roomDatabase = FlowCryptRoomDatabase.getDatabase(context)
      if (roomDatabase.recipientDao().getRecipientByEmail(email) == null) {
        roomDatabase.recipientDao().insert(requireNotNull(pgpKeyDetails.toRecipientEntity()))
      }
      roomDatabase.pubKeyDao().insert(pgpKeyDetails.toPublicKeyEntity(email))
      // Added timeout for a better sync between threads.
      Thread.sleep(500)
    }

    fun getPgpKeyDetailsFromAssets(
      assetsPath: String,
      onlyPrivate: Boolean = false
    ): PgpKeyDetails {
      return getPgpKeyDetailsListFromAssets(assetsPath, onlyPrivate).first()
    }

    fun getPgpKeyDetailsListFromAssets(
      assetsPath: String,
      onlyPrivate: Boolean = false
    ): List<PgpKeyDetails> {
      val parsedCollections =
        PgpKey.parseKeys(TestGeneralUtil.readFileFromAssetsAsStream(assetsPath))

      if (onlyPrivate) {
        val onlyPrivateKeysCollection = PgpKey.ParseKeyResult(
          PGPKeyRingCollection(
            parsedCollections.pgpKeyRingCollection
              .pgpSecretKeyRingCollection.keyRings.asSequence().toList(), false
          )
        )

        return onlyPrivateKeysCollection.pgpKeyDetailsList
      } else {
        return parsedCollections.pgpKeyDetailsList
      }
    }

    fun getKeysFromAssets(
      keysPaths: Array<String>,
      onlyPrivate: Boolean = false
    ): ArrayList<PgpKeyDetails> {
      val privateKeys = ArrayList<PgpKeyDetails>()
      keysPaths.forEach { path ->
        privateKeys.addAll(getPgpKeyDetailsListFromAssets(path, onlyPrivate))
      }
      return privateKeys
    }

    fun deleteKey(accountEntity: AccountEntity, keyPath: String) {
      val pgpKeyDetails = getPgpKeyDetailsFromAssets(keyPath)
      deleteKey(accountEntity, pgpKeyDetails)
    }

    fun deleteKey(accountEntity: AccountEntity, pgpKeyDetails: PgpKeyDetails) {
      val context = InstrumentationRegistry.getInstrumentation().targetContext
      val roomDatabase = FlowCryptRoomDatabase.getDatabase(context)
      pgpKeyDetails.fingerprint.let {
        roomDatabase.keysDao().deleteByAccountAndFingerprint(accountEntity.email, it)
      }

      // Added timeout for a better sync between threads.
      Thread.sleep(3000)
    }
  }
}
