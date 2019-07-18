/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.node

import com.flowcrypt.email.BuildConfig
import com.flowcrypt.email.util.GeneralUtil
import com.flowcrypt.email.util.exception.ExceptionUtil

/**
 * This class describes a logic of running Node.js using the native code. Here we run Node.js server with given
 * parameters. This is a singleton. Because we need to be sure we have only one instance of Node.js which is run.
 *
 * @see [Node.js for Mobile Apps](https://code.janeasystems.com/nodejs-mobile/getting-started-android)
 */
internal class NativeNode private constructor(private val nodeSecret: NodeSecret) {

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
   * @param jsCode An input js code
   */
  fun start(jsCode: String) { // takes just a few ms
    if (!isRunning) {
      isRunning = true
      Thread(Runnable {
        Thread.currentThread().name = NativeNode::class.java.simpleName
        startSynchronously(jsCode)
        isRunning = false // if it ever stops running, set isRunning back to false
        isReady = false
      }).start()
    }
  }

  fun isReady(): Boolean {
    return isReady
  }

  private fun startSynchronously(jsCode: String) {
    try {
      // slow!
      // takes 1750ms to start node with no scripts - using node-chakracore v8.6.0
      // startNodeWithArguments(new String[]{"node", "-e", "console.log('NODE: ' + Date.now())"});
      // about 3500ms with scripts
      startNodeWithArguments(arrayOf("node", "-e", getJsSrc(jsCode)))
    } catch (e: Exception) {
      e.printStackTrace()
      ExceptionUtil.handleError(e)
    }

  }

  private fun getJsSrc(jsCode: String): String {
    var src = ""
    src += genConst("NODE_UNIX_SOCKET", nodeSecret.unixSocketFilePath) // not used yet
    src += genConst("NODE_PORT", nodeSecret.port.toString())
    src += genConst("NODE_SSL_CA", nodeSecret.ca!!)
    src += genConst("NODE_SSL_CRT", nodeSecret.crt!!)
    src += genConst("NODE_SSL_KEY", nodeSecret.key!!)
    src += genConst("NODE_AUTH_HEADER", nodeSecret.authHeader)
    src += genConst("NODE_DEBUG", "false")
    src += genConst("APP_ENV", "prod")
    src += genConst("APP_VERSION", BuildConfig.VERSION_NAME.split("_")[0])
    src += genConst("APP_PROFILE", "false")
    src += genConst("NODE_PRINT_REPLAY", "false")
    src += jsCode
    return src
  }

  private fun genConst(name: String, value: String): String {
    return "const $name = `$value`;\n"
  }

  companion object {

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
    fun getInstance(nodeSecret: NodeSecret): NativeNode {
      return INSTANCE ?: synchronized(this) {
        INSTANCE ?: NativeNode(nodeSecret).also { INSTANCE = it }
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

      if (GeneralUtil.isDebugBuild()) {
        println("NODEJS-NATIVE-MSG[$msg]")
      }
    }
  }
}
