/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.util;

import android.content.Context;
import android.os.Environment;

import com.flowcrypt.email.database.dao.KeysDao;
import com.flowcrypt.email.database.dao.source.KeysDaoSource;
import com.flowcrypt.email.database.dao.source.UserIdEmailsKeysDaoSource;
import com.flowcrypt.email.js.Js;
import com.flowcrypt.email.js.JsForUiManager;
import com.flowcrypt.email.js.PgpKey;
import com.flowcrypt.email.model.KeyDetails;
import com.flowcrypt.email.security.KeyStoreCryptoManager;
import com.google.gson.Gson;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import androidx.test.internal.runner.junit4.statement.UiThreadStatement;
import androidx.test.platform.app.InstrumentationRegistry;

/**
 * @author Denis Bondarenko
 * Date: 18.01.2018
 * Time: 13:02
 * E-mail: DenBond7@gmail.com
 */

public class TestGeneralUtil {

  public static <T> T readObjectFromResources(String path, Class<T> aClass) {
    try {
      return new Gson().fromJson(
          IOUtils.toString(aClass.getClassLoader().getResourceAsStream(path), StandardCharsets.UTF_8),
          aClass);
    } catch (IOException e) {
      e.printStackTrace();
    }
    return null;
  }

  public static String readFileFromAssetsAsString(Context context, String filePath) throws IOException {
    return IOUtils.toString(context.getAssets().open(filePath), "UTF-8");
  }

  public static void saveKeyToDatabase(String privetKey, String passphrase, KeyDetails.Type type) throws Throwable {
    KeysDaoSource keysDaoSource = new KeysDaoSource();
    KeyDetails keyDetails = new KeyDetails(privetKey, type);
    KeyStoreCryptoManager keyStoreCryptoManager = new KeyStoreCryptoManager(InstrumentationRegistry.getInstrumentation()
        .getTargetContext());
    String armoredPrivateKey = keyDetails.getValue();

    Js js = new Js(InstrumentationRegistry.getInstrumentation().getTargetContext(), null);
    String normalizedArmoredKey = js.crypto_key_normalize(armoredPrivateKey);

    PgpKey pgpKey = js.crypto_key_read(normalizedArmoredKey);
    keysDaoSource.addRow(InstrumentationRegistry.getInstrumentation().getTargetContext(),
        KeysDao.generateKeysDao(keyStoreCryptoManager, keyDetails, pgpKey, passphrase));

    new UserIdEmailsKeysDaoSource().addRow(InstrumentationRegistry.getInstrumentation().getTargetContext(), pgpKey
            .getLongid(),
        pgpKey.getPrimaryUserId().getEmail());

    UiThreadStatement.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        JsForUiManager.getInstance(InstrumentationRegistry.getInstrumentation().getTargetContext())
            .getJs()
            .getStorageConnector()
            .refresh(InstrumentationRegistry.getInstrumentation().getTargetContext());
      }
    });
    Thread.sleep(1000);// Added timeout for a better sync between threads.
  }

  public static void deleteFiles(List<File> files) {
    for (File file : files) {
      if (!file.delete()) {
        System.out.println("Can't delete a file " + file);
      }
    }
  }

  public static File createFile(String fileName, String fileText) {
    File file = new File(InstrumentationRegistry.getInstrumentation().getTargetContext().getExternalFilesDir(Environment
        .DIRECTORY_DOCUMENTS), fileName);
    try (FileOutputStream outputStream = new FileOutputStream(file)) {
      outputStream.write(fileText.getBytes());
    } catch (IOException e) {
      e.printStackTrace();
    }
    return file;
  }
}
