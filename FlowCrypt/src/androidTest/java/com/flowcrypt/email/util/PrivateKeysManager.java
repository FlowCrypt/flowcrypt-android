/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.util;

import com.flowcrypt.email.api.retrofit.node.NodeGson;
import com.flowcrypt.email.api.retrofit.response.model.node.NodeKeyDetails;
import com.flowcrypt.email.base.BaseTest;
import com.flowcrypt.email.database.dao.KeysDao;
import com.flowcrypt.email.database.dao.source.KeysDaoSource;
import com.flowcrypt.email.database.dao.source.UserIdEmailsKeysDaoSource;
import com.flowcrypt.email.js.UiJsManager;
import com.flowcrypt.email.model.KeyDetails;
import com.flowcrypt.email.security.KeyStoreCryptoManager;
import com.google.gson.Gson;

import java.io.IOException;
import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.test.internal.runner.junit4.statement.UiThreadStatement;
import androidx.test.platform.app.InstrumentationRegistry;

/**
 * This tool can help manage private keys in the database. For testing purposes only.
 *
 * @author Denis Bondarenko
 * Date: 27.12.2017
 * Time: 17:44
 * E-mail: DenBond7@gmail.com
 */

public class PrivateKeysManager {

  public static void saveKeyFromAssetsToDatabase(String keyPath, String passphrase, KeyDetails.Type type) throws Throwable {
    NodeKeyDetails nodeKeyDetails = getNodeKeyDetailsFromAssets(keyPath);
    saveKeyToDatabase(nodeKeyDetails, passphrase, type);
  }

  public static void saveKeyToDatabase(NodeKeyDetails nodeKeyDetails, String passphrase, KeyDetails.Type type) throws Throwable {
    KeysDaoSource keysDaoSource = new KeysDaoSource();
    KeyStoreCryptoManager keyStoreCryptoManager = new KeyStoreCryptoManager(InstrumentationRegistry.getInstrumentation()
        .getTargetContext());

    keysDaoSource.addRow(InstrumentationRegistry.getInstrumentation().getTargetContext(),
        KeysDao.generateKeysDao(keyStoreCryptoManager, type, nodeKeyDetails, passphrase));

    new UserIdEmailsKeysDaoSource().addRow(InstrumentationRegistry.getInstrumentation().getTargetContext(),
        nodeKeyDetails.getLongId(), nodeKeyDetails.getPrimaryPgpContact().getEmail());

    UiThreadStatement.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        UiJsManager.getInstance(InstrumentationRegistry.getInstrumentation().getTargetContext())
            .getJs()
            .getStorageConnector()
            .refresh(InstrumentationRegistry.getInstrumentation().getTargetContext());
      }
    });

    // Added timeout for a better sync between threads.
    Thread.sleep(1000);
  }

  public static NodeKeyDetails getNodeKeyDetailsFromAssets(String assetsPath) throws IOException {
    Gson gson = NodeGson.getInstance().getGson();
    String json = TestGeneralUtil.readFileFromAssetsAsString(BaseTest.getContext(), assetsPath);
    return gson.fromJson(json, NodeKeyDetails.class);
  }

  @NonNull
  public static ArrayList<NodeKeyDetails> getKeysFromAssets(String[] keysPaths) throws IOException {
    ArrayList<NodeKeyDetails> privateKeys = new ArrayList<>();
    for (String path : keysPaths) {
      privateKeys.add(getNodeKeyDetailsFromAssets(path));
    }
    return privateKeys;
  }
}
