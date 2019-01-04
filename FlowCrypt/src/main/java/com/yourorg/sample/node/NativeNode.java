package com.yourorg.sample.node;

/**
 * This class describes a logic of running Node.js using the native code.
 * Here we run Node.js server with given parameters.
 */
class NativeNode {

  private static volatile boolean isReady = false;

  static {
    // Used to load the 'native-lib' library on application startup
    System.loadLibrary("native-lib");
    System.loadLibrary("node"); // takes about 100ms
  }

  private boolean isRunning;
  private NodeSecret nodeSecret;

  NativeNode(NodeSecret nodeSecret) {
    this.nodeSecret = nodeSecret;
  }

  /**
   * Will be called by native code
   */
  public static void receiveNativeMessageFromNode(String msg) {
    if (msg.startsWith("listening on ")) {
      isReady = true;
    }
    System.out.println("NODEJS-NATIVE-MSG[" + msg + "]");
  }

  /**
   * A native method that is implemented by the 'native-lib' native library
   */
  @SuppressWarnings("JniMissingFunction")
  public native Integer startNodeWithArguments(String[] arguments);

  /**
   * A native method that is implemented by the 'native-lib' native library
   */
  @SuppressWarnings("JniMissingFunction")
  public native void sendNativeMessageToNode(String msg);

  /**
   * Run the Node.js using input parameters.
   *
   * @param jsCode An input js code
   */
  void start(final String jsCode) { // takes just a few ms
    if (!isRunning) {
      isRunning = true;
      new Thread(new Runnable() {
        @Override
        public void run() {
          Thread.currentThread().setName("NativeNode");
          startSynchronously(jsCode);
          isRunning = false; // if it ever stops running, set isRunning back to false
          isReady = false;
        }
      }).start();
    }
  }

  Boolean isReady() {
    return isReady;
  }

  private void startSynchronously(String jsCode) {
    try {
      // slow!
      // takes 1750ms to start node with no scripts - using node-chakracore v8.6.0
      // startNodeWithArguments(new String[]{"node", "-e", "console.log('NODE: ' + Date.now())"});
      // about 3500ms with scripts
      startNodeWithArguments(new String[]{"node", "-e", getJsSrc(jsCode)});
    } catch (Exception e) {
      e.printStackTrace(); // todo - add acra
    }
  }

  private String getJsSrc(String jsCode) {
    String src = "";
    src += genConst("NODE_UNIX_SOCKET", String.valueOf(nodeSecret.unixSocketFilePath)); // not used yet
    src += genConst("NODE_PORT", String.valueOf(nodeSecret.port));
    src += genConst("NODE_SSL_CA", nodeSecret.ca);
    src += genConst("NODE_SSL_CRT", nodeSecret.crt);
    src += genConst("NODE_SSL_KEY", nodeSecret.key);
    src += genConst("NODE_AUTH_HEADER", nodeSecret.authHeader);
    src += jsCode;
    return src;
  }

  private String genConst(String name, String value) {
    return "const " + name + " = `" + value + "`;\n";
  }
}
