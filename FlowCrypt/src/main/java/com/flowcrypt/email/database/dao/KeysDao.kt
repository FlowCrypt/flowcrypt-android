/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.database.dao

import android.text.TextUtils

import com.flowcrypt.email.api.retrofit.response.model.node.NodeKeyDetails
import com.flowcrypt.email.model.KeyDetails
import com.flowcrypt.email.security.KeyStoreCryptoManager
import com.flowcrypt.email.security.model.PrivateKeySourceType

/**
 * This class describe a key information object.
 *
 * @author DenBond7
 * Date: 13.05.2017
 * Time: 12:54
 * E-mail: DenBond7@gmail.com
 */

data class KeysDao constructor(var longId: String? = null,
                               var privateKeySourceType: PrivateKeySourceType? = null,
                               var publicKey: String? = null,
                               var privateKey: String? = null,
                               var passphrase: String? = null) {

  companion object {
    /**
     * Generate [KeysDao] using input parameters.
     * This method use [NodeKeyDetails.getLongId] for generate an algorithm parameter spec String and
     * [KeyStoreCryptoManager] for generate encrypted version of the private key and password.
     *
     * @param keyStoreCryptoManager A [KeyStoreCryptoManager] which will bu used to encrypt
     * information about a key;
     * @param type                  The private key born type
     * @param nodeKeyDetails        Key details;
     * @param passphrase            A passphrase which user provided;
     */
    @Throws(Exception::class)
    @JvmStatic
    fun generateKeysDao(keyStoreCryptoManager: KeyStoreCryptoManager, type: KeyDetails.Type,
                        nodeKeyDetails: NodeKeyDetails, passphrase: String): KeysDao {
      val keysDao = generateKeysDao(keyStoreCryptoManager, nodeKeyDetails, passphrase)

      when (type) {
        KeyDetails.Type.EMAIL -> keysDao.privateKeySourceType = PrivateKeySourceType.BACKUP

        KeyDetails.Type.FILE, KeyDetails.Type.CLIPBOARD -> keysDao.privateKeySourceType = PrivateKeySourceType.IMPORT

        KeyDetails.Type.NEW -> keysDao.privateKeySourceType = PrivateKeySourceType.NEW
      }

      return keysDao
    }

    /**
     * Generate [KeysDao] using input parameters.
     * This method use [NodeKeyDetails.getLongId] for generate an algorithm parameter spec String and
     * [KeyStoreCryptoManager] for generate encrypted version of the private key and password.
     *
     * @param keyStoreCryptoManager A [KeyStoreCryptoManager] which will bu used to encrypt
     * information about a key;
     * @param nodeKeyDetails        Key details;
     * @param passphrase            A passphrase which user provided;
     */
    @Throws(Exception::class)
    @JvmStatic
    fun generateKeysDao(keyStoreCryptoManager: KeyStoreCryptoManager, nodeKeyDetails: NodeKeyDetails,
                        passphrase: String): KeysDao {
      if (nodeKeyDetails.isDecrypted!!) {
        throw IllegalArgumentException("Error. The key is decrypted!")
      }

      val keysDao = KeysDao()
      val randomVector: String

      if (TextUtils.isEmpty(nodeKeyDetails.longId)) {
        throw IllegalArgumentException("longid == null")
      } else {
        randomVector = KeyStoreCryptoManager.normalizeAlgorithmParameterSpecString(nodeKeyDetails.longId!!)
      }

      keysDao.longId = nodeKeyDetails.longId
      keysDao.privateKey = keyStoreCryptoManager.encrypt(nodeKeyDetails.privateKey!!, randomVector)
      keysDao.publicKey = nodeKeyDetails.publicKey
      keysDao.passphrase = keyStoreCryptoManager.encrypt(passphrase, randomVector)
      return keysDao
    }
  }
}
