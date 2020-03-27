/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.node

import android.app.Application
import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.preference.PreferenceManager
import com.flowcrypt.email.Constants
import com.flowcrypt.email.api.retrofit.node.NodeRetrofitHelper
import com.flowcrypt.email.api.retrofit.node.RequestsManager
import com.flowcrypt.email.api.retrofit.node.gson.NodeGson
import com.flowcrypt.email.node.exception.NodeNotReady
import com.flowcrypt.email.security.KeyStoreCryptoManager
import com.flowcrypt.email.security.KeysStorageImpl
import com.flowcrypt.email.util.GeneralUtil
import com.flowcrypt.email.util.SharedPreferencesHelper
import com.flowcrypt.email.util.exception.ExceptionUtil
import org.apache.commons.io.IOUtils
import java.io.FileNotFoundException
import java.nio.charset.StandardCharsets

/**
 * This is node.js manager.
 */
class Node private constructor(app: Application) {
  @Volatile
  private var nativeNode: NativeNode? = null

  @Volatile
  private var nodeSecret: NodeSecret? = null

  @Volatile
  var requestsManager: RequestsManager? = null
    private set

  init {
    init(app)
  }

  val liveData: MutableLiveData<Boolean> = MutableLiveData()

  private fun init(context: Context) {
    Thread(Runnable {
      KeysStorageImpl.getInstance(context.applicationContext).fetchKeysManually(context.applicationContext)
      Thread.currentThread().name = "Node"
      try {
        val certs = getCachedNodeSecretCerts(context)
        if (certs == null) {
          nodeSecret = NodeSecret(context.filesDir.absolutePath)
          saveNodeSecretCertsToCache(context, nodeSecret!!.cache)
        } else {
          nodeSecret = NodeSecret(context.filesDir.absolutePath, certs)
        }

        requestsManager = RequestsManager
        start(context, nodeSecret)
        waitUntilReady()
        NodeRetrofitHelper.init(context, nodeSecret!!)

        liveData.postValue(true)
      } catch (e: Exception) {
        e.printStackTrace()
        ExceptionUtil.handleError(e)
        liveData.postValue(false)
      }
    }).start()
  }

  private fun start(context: Context, nodeSecret: NodeSecret?) {
    if (nativeNode == null) {
      val isDebugEnabled = GeneralUtil.isDebugBuild() && SharedPreferencesHelper.getBoolean(PreferenceManager
          .getDefaultSharedPreferences(context), Constants.PREF_KEY_IS_NATIVE_NODE_DEBUG_ENABLED, false)

      nativeNode = NativeNode.getInstance(isDebugEnabled, nodeSecret!!) // takes about 100ms due to static native loads
    }
    nativeNode!!.start(context)
  }

  private fun waitUntilReady() {
    if (nativeNode == null) {
      throw NodeNotReady("NativeNode not started. Call Node.start first")
    }
    while (!nativeNode!!.isReady()) {
      try {
        Thread.sleep(50)
      } catch (e: InterruptedException) {
        throw NodeNotReady("Was interrupted while waiting for node to become ready", e)
      }
    }
  }

  private fun saveNodeSecretCertsToCache(context: Context, nodeSecretCerts: NodeSecretCerts) {
    val gson = NodeGson.gson
    val data = gson.toJson(nodeSecretCerts)
    try {
      context.openFileOutput(NODE_SECRETS_CACHE_FILENAME, Context.MODE_PRIVATE).use { outputStream ->
        outputStream.write(KeyStoreCryptoManager.encrypt(data).toByteArray())
      }
    } catch (e: Exception) {
      throw RuntimeException("Could not save certs cache", e)
    }
  }

  private fun getCachedNodeSecretCerts(context: Context): NodeSecretCerts? {
    try {
      context.openFileInput(NODE_SECRETS_CACHE_FILENAME).use { inputStream ->
        val gson = NodeGson.gson
        val rawData = IOUtils.toString(inputStream, StandardCharsets.UTF_8)

        val splitPosition = rawData.indexOf('\n')

        if (splitPosition == -1) {
          throw IllegalArgumentException("wrong rawData")
        }

        val decryptedData = try {
          KeyStoreCryptoManager.decrypt(rawData.substring(splitPosition + 1))
        } catch (e: Exception) {
          KeyStoreCryptoManager.decrypt(rawData)
        }
        return gson.fromJson(decryptedData, NodeSecretCerts::class.java)
      }
    } catch (e: FileNotFoundException) {
      e.printStackTrace()
      return null
    } catch (e: Exception) {
      throw RuntimeException("Could not load certs cache", e)
    }
  }

  companion object {
    private const val NODE_SECRETS_CACHE_FILENAME = "flowcrypt-node-secrets-cache"

    @Volatile
    private var INSTANCE: Node? = null

    @JvmStatic
    fun getInstance(app: Application): Node {
      return INSTANCE ?: synchronized(this) {
        INSTANCE ?: Node(app).also { INSTANCE = it }
      }
    }
  }
}
