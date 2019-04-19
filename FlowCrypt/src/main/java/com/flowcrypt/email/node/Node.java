/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.node;

import android.app.Application;
import android.content.Context;
import android.content.res.AssetManager;

import com.flowcrypt.email.api.retrofit.node.RequestsManager;
import com.flowcrypt.email.api.retrofit.node.gson.NodeGson;
import com.flowcrypt.email.node.exception.NodeNotReady;
import com.flowcrypt.email.security.KeyStoreCryptoManager;
import com.google.gson.Gson;

import org.apache.commons.io.IOUtils;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

/**
 * This is node.js manager.
 */
public final class Node {
  private static final String NODE_SECRETS_CACHE_FILENAME = "flowcrypt-node-secrets-cache";

  private static final Node INSTANCE = new Node();
  private volatile NativeNode nativeNode;
  private volatile NodeSecret nodeSecret;
  private volatile RequestsManager requestsManager;
  private MutableLiveData<Boolean> liveData;

  private Node() {
    liveData = new MutableLiveData<>();
  }

  public static Node getInstance() {
    return INSTANCE;
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
          requestsManager = RequestsManager.getInstance();
          requestsManager.init(nodeSecret);
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

  private void start(AssetManager am, NodeSecret nodeSecret) throws IOException {
    if (nativeNode == null) {
      nativeNode = NativeNode.getInstance(nodeSecret); // takes about 100ms due to static native loads
    }
    nativeNode.start(IOUtils.toString(am.open("js/flowcrypt-android.js"), StandardCharsets.UTF_8));
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

  private void saveNodeSecretCertsToCache(Context context, NodeSecretCerts nodeSecretCerts) {
    Gson gson = NodeGson.getInstance().getGson();
    String data = gson.toJson(nodeSecretCerts);
    try (FileOutputStream outputStream = context.openFileOutput(NODE_SECRETS_CACHE_FILENAME, Context.MODE_PRIVATE)) {
      KeyStoreCryptoManager keyStoreCryptoManager = KeyStoreCryptoManager.getInstance(context);
      String spec = KeyStoreCryptoManager.generateAlgorithmParameterSpecString();
      String encryptedData = keyStoreCryptoManager.encrypt(data, spec);
      outputStream.write(spec.getBytes());
      outputStream.write('\n');
      outputStream.write(encryptedData.getBytes());
    } catch (Exception e) {
      throw new RuntimeException("Could not save certs cache", e);
    }
  }

  private NodeSecretCerts getCachedNodeSecretCerts(Context context) {
    try (FileInputStream inputStream = context.openFileInput(NODE_SECRETS_CACHE_FILENAME)) {
      Gson gson = NodeGson.getInstance().getGson();
      String rawData = IOUtils.toString(inputStream, StandardCharsets.UTF_8);

      int splitPosition = rawData.indexOf('\n');

      if (splitPosition == -1) {
        throw new IllegalArgumentException("wrong rawData");
      }

      String spec = rawData.substring(0, splitPosition);

      KeyStoreCryptoManager keyStoreCryptoManager = KeyStoreCryptoManager.getInstance(context);
      String decryptedData = keyStoreCryptoManager.decrypt(rawData.substring(splitPosition + 1), spec);
      return gson.fromJson(decryptedData, NodeSecretCerts.class);
    } catch (FileNotFoundException e) {
      e.printStackTrace();
      return null;
    } catch (Exception e) {
      throw new RuntimeException("Could not load certs cache", e);
    }
  }
}
