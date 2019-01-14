package com.flowcrypt.email.node;

import android.app.Application;
import android.content.Context;
import android.content.res.AssetManager;

import com.flowcrypt.email.api.retrofit.node.RequestsManager;
import com.flowcrypt.email.node.exception.NodeNotReady;

import org.apache.commons.io.IOUtils;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

/**
 * This is node.js manager.
 */
public class Node {
  private static final String NODE_SECRETS_CACHE_FILENAME = "flowcrypt-node-secrets-cache";

  private static final Node ourInstance = new Node();
  private volatile NativeNode nativeNode;
  private NodeSecret nodeSecret;
  private MutableLiveData<Boolean> liveData;
  private RequestsManager requestsManager;

  private Node() {
    liveData = new MutableLiveData<>();
  }

  public static Node getInstance() {
    return ourInstance;
  }

  public static void init(@NonNull Application app) {
    Node node = Node.getInstance();
    node.start(app);
  }

  public LiveData<Boolean> getLiveData() {
    return liveData;
  }

  public RequestsManager getRequestsManager() {
    return requestsManager;
  }

  public NativeNode getNativeNode() {
    return nativeNode;
  }

  public NodeSecret getNodeSecret() {
    return nodeSecret;
  }

  private void start(final Context context) {
    new Thread(new Runnable() {
      @Override
      public void run() {
        Thread.currentThread().setName("Node");
        try {
          NodeSecretCerts certs = getCachedNodeSecretCerts(context);
          if (certs == null) {
            nodeSecret = new NodeSecret(context.getFilesDir().getAbsolutePath());
            saveNodeSecretCertsToCache(context, nodeSecret.getCache());
          } else {
            nodeSecret = new NodeSecret(context.getFilesDir().getAbsolutePath(), certs);
          }
          requestsManager = new RequestsManager(nodeSecret);
          start(context.getAssets(), nodeSecret);
          waitUntilReady();
          liveData.postValue(true);
        } catch (Exception e) {
          e.printStackTrace();
          liveData.postValue(false);
        }
      }
    }).start();
  }

  private void waitUntilReady() throws NodeNotReady {
    if (nativeNode == null) {
      throw new NodeNotReady("NativeNode not started. Call Node.start first");
    }
    while (!nativeNode.isReady()) {
      try {
        Thread.sleep(50);
      } catch (InterruptedException e) {
        throw new NodeNotReady("Was interrupted while waiting for node to become ready", e);
      }
    }
  }

  /**
   * this is just an example. Production app should use encrypted store
   */
  private void saveNodeSecretCertsToCache(Context context, NodeSecretCerts nodeSecretCerts) {
    try {
      FileOutputStream fos = context.openFileOutput(NODE_SECRETS_CACHE_FILENAME, Context.MODE_PRIVATE);
      ObjectOutputStream oos = new ObjectOutputStream(fos);
      oos.writeObject(nodeSecretCerts);
      oos.close();
      fos.close();
    } catch (Exception e) {
      throw new RuntimeException("Could not save certs cache", e);
    }
  }

  /**
   * this is just an example. Production app should use encrypted store
   */
  private NodeSecretCerts getCachedNodeSecretCerts(Context context) {
    try {
      FileInputStream fis = context.openFileInput(NODE_SECRETS_CACHE_FILENAME);
      ObjectInputStream ois = new ObjectInputStream(fis);
      return (NodeSecretCerts) ois.readObject();
    } catch (FileNotFoundException e) {
      return null;
    } catch (Exception e) {
      throw new RuntimeException("Could not load certs cache", e);
    }
  }

  private void start(AssetManager am, NodeSecret nodeSecret) throws IOException {
    if (nativeNode == null) {
      nativeNode = new NativeNode(nodeSecret); // takes about 100ms due to static native loads
    }
    nativeNode.start(IOUtils.toString(am.open("js/flowcrypt-android.js"), StandardCharsets.UTF_8));
  }
}
