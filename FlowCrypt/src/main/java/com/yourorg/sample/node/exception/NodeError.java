package com.yourorg.sample.node.exception;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.stream.Collectors;

import javax.net.ssl.HttpsURLConnection;

/**
 * @author DenBond7
 */
public class NodeError extends Exception {
  private NodeError(int httpErrCode, String errMsg, StackTraceElement addStackTraceElement) {
    super(Integer.valueOf(httpErrCode).toString() + " " + errMsg);
    StackTraceElement[] origStack = getStackTrace();
    StackTraceElement[] newStack = Arrays.copyOf(origStack, origStack.length + 1);
    newStack[origStack.length] = addStackTraceElement;
    setStackTrace(newStack);
  }

//  static NodeError fromResponse(okhttp3.Response response) { // this is needed if we want to use unix sockets
//    return NodeError.fromErrCodeAndInputStream(response.code(), response.body().byteStream());
//  }

  public static NodeError fromConnection(HttpsURLConnection conn) {
    int errCode;
    try {
      errCode = conn.getResponseCode();
    } catch (IOException e) {
      return new NodeError(0, e.getMessage(), null);
    }
    return NodeError.fromErrCodeAndInputStream(errCode, conn.getErrorStream());
  }

  private static NodeError fromErrCodeAndInputStream(int errCode, InputStream is) {
    String res = new BufferedReader(new InputStreamReader(is)).lines().collect(Collectors.joining("\n"));
    try {
      JSONObject obj = new JSONObject(res);
      JSONObject error = obj.getJSONObject("error");
      String stack = error.getString("stack");
      return new NodeError(errCode, error.getString("message"), newStackTraceElement(stack));
    } catch (JSONException e) {
      return new NodeError(errCode, "Node http err without err obj", newStackTraceElement("[RES]" + res));
    }
  }

  static private StackTraceElement newStackTraceElement(String data) {
    return new StackTraceElement("=========================", "\n[node.js] " + data, "flowcrypt-android.js", -1);
  }
}
