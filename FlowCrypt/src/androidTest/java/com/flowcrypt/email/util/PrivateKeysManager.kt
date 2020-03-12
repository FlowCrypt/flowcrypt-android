/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.util

import androidx.test.internal.runner.junit4.statement.UiThreadStatement
import androidx.test.platform.app.InstrumentationRegistry
import com.flowcrypt.email.api.retrofit.node.gson.NodeGson
import com.flowcrypt.email.api.retrofit.response.model.node.NodeKeyDetails
import com.flowcrypt.email.database.dao.KeysDao
import com.flowcrypt.email.database.dao.source.KeysDaoSource
import com.flowcrypt.email.database.dao.source.UserIdEmailsKeysDao
import com.flowcrypt.email.model.KeyDetails
import com.flowcrypt.email.security.KeyStoreCryptoManager
import com.flowcrypt.email.security.KeysStorageImpl
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
    @JvmStatic
    fun saveKeyFromAssetsToDatabase(keyPath: String, passphrase: String, type: KeyDetails.Type) {
      val nodeKeyDetails = getNodeKeyDetailsFromAssets(keyPath)
      saveKeyToDatabase(nodeKeyDetails, passphrase, type)
    }

    @JvmStatic
    fun saveKeyToDatabase(nodeKeyDetails: NodeKeyDetails, passphrase: String, type: KeyDetails.Type) {
      val context = InstrumentationRegistry.getInstrumentation().targetContext
      val keyStoreCryptoManager = KeyStoreCryptoManager.getInstance(InstrumentationRegistry.getInstrumentation()
          .targetContext)

      KeysDaoSource().addRow(context, KeysDao.generateKeysDao(keyStoreCryptoManager, type, nodeKeyDetails, passphrase))
      UserIdEmailsKeysDao().addRow(context, nodeKeyDetails.longId!!, nodeKeyDetails.primaryPgpContact.email)

      UiThreadStatement.runOnUiThread { KeysStorageImpl.getInstance(context).refresh(context) }
      // Added timeout for a better sync between threads.
      Thread.sleep(3000)
    }

    @JvmStatic
    fun getNodeKeyDetailsFromAssets(assetsPath: String): NodeKeyDetails {
      val gson = NodeGson.gson
      val json =
          TestGeneralUtil.readFileFromAssetsAsString(InstrumentationRegistry.getInstrumentation().context, assetsPath)
      return gson.fromJson(json, NodeKeyDetails::class.java)
    }

    @JvmStatic
    fun getKeysFromAssets(keysPaths: Array<String>): ArrayList<NodeKeyDetails> {
      val privateKeys = ArrayList<NodeKeyDetails>()
      keysPaths.forEach { path ->
        privateKeys.add(getNodeKeyDetailsFromAssets(path))
      }
      return privateKeys
    }

    @JvmStatic
    fun deleteKey(keyPath: String) {
      val nodeKeyDetails = getNodeKeyDetailsFromAssets(keyPath)
      val context = InstrumentationRegistry.getInstrumentation().targetContext

      KeysDaoSource().removeKey(context, nodeKeyDetails.longId!!)
      UserIdEmailsKeysDao().removeKey(context, nodeKeyDetails.longId!!)

      UiThreadStatement.runOnUiThread { KeysStorageImpl.getInstance(context).refresh(context) }
      // Added timeout for a better sync between threads.
      Thread.sleep(3000)
    }
  }
}
