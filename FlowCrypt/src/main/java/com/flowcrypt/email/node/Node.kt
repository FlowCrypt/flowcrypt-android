/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.node

import android.app.Application
import android.content.Context
import android.content.res.AssetManager
import androidx.lifecycle.MutableLiveData
import com.flowcrypt.email.api.retrofit.node.NodeRetrofitHelper
import com.flowcrypt.email.api.retrofit.node.RequestsManager
import com.flowcrypt.email.api.retrofit.node.gson.NodeGson
import com.flowcrypt.email.node.exception.NodeNotReady
import com.flowcrypt.email.security.KeyStoreCryptoManager
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
        start(context.assets, nodeSecret)
        waitUntilReady()
        NodeRetrofitHelper.init(nodeSecret!!)

        liveData.postValue(true)
      } catch (e: Exception) {
        e.printStackTrace()
        liveData.postValue(false)
      }
    }).start()
  }

  private fun start(am: AssetManager, nodeSecret: NodeSecret?) {
    if (nativeNode == null) {
      nativeNode = NativeNode.getInstance(nodeSecret!!) // takes about 100ms due to static native loads
    }
    nativeNode!!.start(IOUtils.toString(am.open("js/flowcrypt-android.js"), StandardCharsets.UTF_8))
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
        val keyStoreCryptoManager = KeyStoreCryptoManager.getInstance(context)
        val spec = KeyStoreCryptoManager.generateAlgorithmParameterSpecString()
        val encryptedData = keyStoreCryptoManager.encrypt(data, spec)
        outputStream.write(spec.toByteArray())
        outputStream.write('\n'.toInt())
        outputStream.write(encryptedData.toByteArray())
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

        val spec = rawData.substring(0, splitPosition)

        val keyStoreCryptoManager = KeyStoreCryptoManager.getInstance(context)
        val decryptedData = keyStoreCryptoManager.decrypt(rawData.substring(splitPosition + 1), spec)
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
