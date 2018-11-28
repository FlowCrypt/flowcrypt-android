/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.util;

import android.content.Context;
import android.text.TextUtils;

import com.flowcrypt.email.database.dao.KeysDao;
import com.flowcrypt.email.database.dao.source.KeysDaoSource;
import com.flowcrypt.email.js.Js;
import com.flowcrypt.email.js.PgpKey;
import com.flowcrypt.email.security.KeyStoreCryptoManager;
import com.flowcrypt.email.security.model.PrivateKeySourceType;

import org.apache.commons.io.IOUtils;

import java.util.UUID;

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

  private static final String TEMP_PASSPHRASE = "android";
  private static final String TEMP_PRIVATE_KEY_LONGID = "6C0DD31D159DF3EF";

  public static void addTempPrivateKey() throws Exception {
    String armoredPrivateKey = IOUtils.toString(InstrumentationRegistry.getInstrumentation().getContext().getAssets()
        .open
        ("pgp/temp-sec.asc"), "UTF-8");
    Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
    Js js = new Js(appContext, null);
    String normalizedArmoredKey = js.crypto_key_normalize(armoredPrivateKey);

    PgpKey pgpKey = js.crypto_key_read(normalizedArmoredKey);
    KeysDao keysDao = new KeysDao();
    keysDao.setLongId(pgpKey.getLongid());
    keysDao.setPrivateKeySourceType(PrivateKeySourceType.NEW);

    String randomVector;

    if (TextUtils.isEmpty(pgpKey.getLongid())) {
      randomVector = KeyStoreCryptoManager.normalizeAlgorithmParameterSpecString(
          UUID.randomUUID().toString().substring(0,
              KeyStoreCryptoManager.SIZE_OF_ALGORITHM_PARAMETER_SPEC));
    } else {
      randomVector = KeyStoreCryptoManager.normalizeAlgorithmParameterSpecString
          (pgpKey.getLongid());
    }

    KeyStoreCryptoManager keyStoreCryptoManager = new KeyStoreCryptoManager(appContext);

    String encryptedPrivateKey = keyStoreCryptoManager.encrypt(pgpKey.armor(), randomVector);
    keysDao.setPrivateKey(encryptedPrivateKey);
    keysDao.setPublicKey(pgpKey.toPublic().armor());

    String encryptedPassphrase = keyStoreCryptoManager.encrypt(TEMP_PASSPHRASE, randomVector);
    keysDao.setPassphrase(encryptedPassphrase);
    new KeysDaoSource().addRow(appContext, keysDao);
  }
}
