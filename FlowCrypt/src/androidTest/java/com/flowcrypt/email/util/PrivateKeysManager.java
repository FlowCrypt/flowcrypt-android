/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.util;

import android.content.Context;

import com.flowcrypt.email.api.retrofit.node.gson.NodeGson;
import com.flowcrypt.email.api.retrofit.response.model.node.NodeKeyDetails;
import com.flowcrypt.email.base.BaseTest;
import com.flowcrypt.email.database.dao.KeysDao;
import com.flowcrypt.email.database.dao.source.KeysDaoSource;
import com.flowcrypt.email.database.dao.source.UserIdEmailsKeysDaoSource;
import com.flowcrypt.email.js.UiJsManager;
import com.flowcrypt.email.model.KeyDetails;
import com.flowcrypt.email.security.KeyStoreCryptoManager;
import com.flowcrypt.email.ui.activity.base.BaseActivity;
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

  public static void saveKeyFromAssetsToDatabase(String keyPath, String passphrase, KeyDetails.Type type,
                                                 BaseActivity baseActivity) throws Throwable {
    NodeKeyDetails nodeKeyDetails = getNodeKeyDetailsFromAssets(keyPath);
    saveKeyToDatabase(nodeKeyDetails, passphrase, type, baseActivity);
  }

  public static void saveKeyToDatabase(NodeKeyDetails nodeKeyDetails, String passphrase, KeyDetails.Type type,
                                       final BaseActivity baseActivity) throws Throwable {
    final Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();

    KeysDaoSource keysDaoSource = new KeysDaoSource();
    KeyStoreCryptoManager keyStoreCryptoManager = new KeyStoreCryptoManager(InstrumentationRegistry.getInstrumentation()
        .getTargetContext());

    keysDaoSource.addRow(context, KeysDao.generateKeysDao(keyStoreCryptoManager, type, nodeKeyDetails, passphrase));

    new UserIdEmailsKeysDaoSource().addRow(context, nodeKeyDetails.getLongId(),
        nodeKeyDetails.getPrimaryPgpContact().getEmail());

    UiThreadStatement.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        UiJsManager.getInstance(context).getJs().getStorageConnector().refresh(context);
        if (baseActivity != null) {
          baseActivity.restartJsService();
        }
      }
    });
    // Added timeout for a better sync between threads.
    Thread.sleep(3000);
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

  public static void deleteKey(String keyPath, final BaseActivity baseActivity) throws Throwable {
    NodeKeyDetails nodeKeyDetails = getNodeKeyDetailsFromAssets(keyPath);
    KeysDaoSource keysDaoSource = new KeysDaoSource();

    final Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();

    keysDaoSource.removeKey(context, nodeKeyDetails.getLongId());
    new UserIdEmailsKeysDaoSource().removeKey(context, nodeKeyDetails.getLongId());

    UiThreadStatement.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        UiJsManager.getInstance(context).getJs().getStorageConnector().refresh(context);
        if (baseActivity != null) {
          baseActivity.restartJsService();
        }
      }
    });
    // Added timeout for a better sync between threads.
    Thread.sleep(3000);
  }
}
