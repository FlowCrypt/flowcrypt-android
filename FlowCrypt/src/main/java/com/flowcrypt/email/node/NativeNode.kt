/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.node

import android.content.Context
import com.flowcrypt.email.BuildConfig
import com.flowcrypt.email.util.GeneralUtil
import com.flowcrypt.email.util.exception.ExceptionUtil
import org.apache.commons.io.IOUtils
import java.nio.charset.StandardCharsets

/**
 * This class describes a logic of running Node.js using the native code. Here we run Node.js server with given
 * parameters. This is a singleton. Because we need to be sure we have only one instance of Node.js which is run.
 *
 * @see [Node.js for Mobile Apps](https://code.janeasystems.com/nodejs-mobile/getting-started-android)
 */
internal class NativeNode private constructor(private val isDebugEnabled: Boolean, private val nodeSecret: NodeSecret) {

  private var isRunning: Boolean = false

  /**
   * A native method that is implemented by the 'native-lib' native library
   */
  external fun startNodeWithArguments(arguments: Array<String>): Int?

  /**
   * A native method that is implemented by the 'native-lib' native library
   */
  external fun sendNativeMessageToNode(msg: String)

  /**
   * Run the Node.js using input parameters.
   *
   * @param context Interface to global information about an application environment.
   */
  fun start(context: Context) { // takes just a few ms
    if (!isRunning) {
      isRunning = true
      Thread(Runnable {
        Thread.currentThread().name = NativeNode::class.java.simpleName
        startSynchronously(context)
        isRunning = false // if it ever stops running, set isRunning back to false
        isReady = false
      }).start()
    }
  }

  fun isReady(): Boolean {
    return isReady
  }

  private fun startSynchronously(context: Context) {
    try {
      // slow!
      // takes 1750ms to start node with no scripts - using node-chakracore v8.6.0
      // startNodeWithArguments(new String[]{"node", "-e", "console.log('NODE: ' + Date.now())"});
      // about 3500ms with scripts
      startNodeWithArguments(arrayOf("node", "-e", getJsSrc(context)))
    } catch (e: Exception) {
      e.printStackTrace()
      ExceptionUtil.handleError(e)
    }

  }

  private fun getJsSrc(context: Context): String {
    var src = ""
    src += genConst("NODE_UNIX_SOCKET", nodeSecret.unixSocketFilePath) // not used yet
    src += genConst("NODE_PORT", nodeSecret.port.toString())
    src += genConst("NODE_SSL_CA", nodeSecret.ca!!)
    src += genConst("NODE_SSL_CRT", nodeSecret.crt!!)
    src += genConst("NODE_SSL_KEY", nodeSecret.key!!)
    src += genConst("NODE_AUTH_HEADER", nodeSecret.authHeader)
    src += genConst("NODE_DEBUG", isDebugEnabled.toString())
    src += genConst("APP_ENV", "prod")
    src += genConst("APP_VERSION", BuildConfig.VERSION_NAME.split("_")[0])
    src += genConst("APP_PROFILE", "false")
    src += genConst("NODE_PRINT_REPLAY", "false")
    src += IOUtils.toString(context.assets.open("js/flowcrypt-android.js"), StandardCharsets.UTF_8)
    return src
  }

  private fun genConst(name: String, value: String): String {
    return "const $name = `$value`;\n"
  }

  companion object {

    private val ASYNC_REQUEST_HEADER = "ASYNC_REQUEST|"
    private val ASYNC_RESPONSE_SUCCESS_HEADER = "ASYNC_RESPONSE|SUCCESS|"
    private val ASYNC_RESPONSE_ERROR_HEADER = "ASYNC_RESPONSE|ERROR|"
    private val ASYNC_REQUEST_ID_LEN = 10
    private val nodeHost = NodeHost()

    @Volatile
    private var INSTANCE: NativeNode? = null

    @Volatile
    private var isReady: Boolean = false

    init {
      // Used to load the 'native-lib' library on application startup
      System.loadLibrary("native-lib")
      System.loadLibrary("node") // takes about 100ms
    }

    @JvmStatic
    fun getInstance(isDebugEnabled: Boolean, nodeSecret: NodeSecret): NativeNode {
      return INSTANCE ?: synchronized(this) {
        INSTANCE ?: NativeNode(isDebugEnabled, nodeSecret).also { INSTANCE = it }
      }
    }

    /**
     * Will be called by native code
     */
    @JvmStatic
    fun receiveNativeMessageFromNode(msg: String) {
      if (msg.startsWith("listening on ")) {
        isReady = true
      }
      if (msg.startsWith(ASYNC_REQUEST_HEADER)) {
        if (GeneralUtil.isDebugBuild()) {
//          println(msg)
        }
        val idLen = 10
        val id = msg.substring(ASYNC_REQUEST_HEADER.length, ASYNC_REQUEST_HEADER.length + idLen)
        val nameEndSeparator = msg.indexOf('|', ASYNC_REQUEST_HEADER.length + ASYNC_REQUEST_ID_LEN + 2)
        val name = msg.substring(ASYNC_REQUEST_HEADER.length + ASYNC_REQUEST_ID_LEN + 1, nameEndSeparator)
        val reqBody = msg.subSequence(nameEndSeparator + 1, msg.length)
        val responseData = nodeHost.nodeReqHandler(name, reqBody)
        var response: String
        try {
          response = "$ASYNC_RESPONSE_SUCCESS_HEADER$id|$responseData"
        } catch (e: Exception) {
          response = "$ASYNC_RESPONSE_ERROR_HEADER$id|${e.stackTrace}"
        }
        if (GeneralUtil.isDebugBuild()) {
//          println(response)
        }
        INSTANCE!!.sendNativeMessageToNode(response)
      } else if (GeneralUtil.isDebugBuild()) {
        println("NODEJS-NATIVE-MSG[$msg]")
      }
    }
  }
}
