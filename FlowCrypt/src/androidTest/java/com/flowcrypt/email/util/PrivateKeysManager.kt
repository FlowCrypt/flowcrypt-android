/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.util

import androidx.test.platform.app.InstrumentationRegistry
import com.flowcrypt.email.database.FlowCryptRoomDatabase
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.model.KeyImportDetails
import com.flowcrypt.email.security.KeyStoreCryptoManager
import com.flowcrypt.email.security.model.NodeKeyDetails
import com.flowcrypt.email.security.pgp.PgpKey
import org.pgpainless.key.collection.PGPKeyRingCollection
import java.util.*

/**
 * This tool can help manage private keys in the database. For testing purposes only.
 *
 * @author Denis Bondarenko
 * Date: 27.12.2017
 * Time: 17:44
 * E-mail: DenBond7@gmail.com
 */
class PrivateKeysManager {
  companion object {
    fun saveKeyFromAssetsToDatabase(accountEntity: AccountEntity, keyPath: String,
                                    passphrase: String, sourceType: KeyImportDetails.SourceType) {
      val nodeKeyDetails = getNodeKeyDetailsFromAssets(keyPath)
      saveKeyToDatabase(accountEntity, nodeKeyDetails, passphrase, sourceType)
    }

    fun saveKeyToDatabase(accountEntity: AccountEntity, nodeKeyDetails: NodeKeyDetails,
                          passphrase: String, sourceType: KeyImportDetails.SourceType) {
      val context = InstrumentationRegistry.getInstrumentation().targetContext
      val roomDatabase = FlowCryptRoomDatabase.getDatabase(context)
      val keyEntity = nodeKeyDetails.toKeyEntity(accountEntity).copy(
          source = sourceType.toString(),
          privateKey = KeyStoreCryptoManager.encrypt(nodeKeyDetails.privateKey).toByteArray(),
          passphrase = KeyStoreCryptoManager.encrypt(passphrase))
      roomDatabase.keysDao().insertWithReplace(keyEntity)
      // Added timeout for a better sync between threads.
      Thread.sleep(3000)
    }

    fun getNodeKeyDetailsFromAssets(assetsPath: String, onlyPrivate: Boolean = false): NodeKeyDetails {
      return getNodeKeyDetailsListFromAssets(assetsPath, onlyPrivate).first()
    }

    fun getNodeKeyDetailsListFromAssets(assetsPath: String, onlyPrivate: Boolean = false): List<NodeKeyDetails> {
      val parsedCollections =
          PgpKey.parseKeys(TestGeneralUtil.readFileFromAssetsAsStream(assetsPath))

      if (onlyPrivate) {
        val onlyPrivateKeysCollection = PgpKey.ParseKeyResult(
            PGPKeyRingCollection(parsedCollections.pgpKeyRingCollection
                .pgpSecretKeyRingCollection.keyRings.asSequence().toList(), false))

        return onlyPrivateKeysCollection.toNodeKeyDetailsList()
      } else {
        return parsedCollections.toNodeKeyDetailsList()
      }
    }

    fun getKeysFromAssets(keysPaths: Array<String>, onlyPrivate: Boolean = false): ArrayList<NodeKeyDetails> {
      val privateKeys = ArrayList<NodeKeyDetails>()
      keysPaths.forEach { path ->
        privateKeys.addAll(getNodeKeyDetailsListFromAssets(path, onlyPrivate))
      }
      return privateKeys
    }

    fun deleteKey(accountEntity: AccountEntity, keyPath: String) {
      val nodeKeyDetails = getNodeKeyDetailsFromAssets(keyPath)
      val context = InstrumentationRegistry.getInstrumentation().targetContext
      val roomDatabase = FlowCryptRoomDatabase.getDatabase(context)
      nodeKeyDetails.fingerprint?.let {
        roomDatabase.keysDao().deleteByAccountAndFingerprint(accountEntity.email, it)
      }

      // Added timeout for a better sync between threads.
      Thread.sleep(3000)
    }
  }
}
