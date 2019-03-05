/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.util;

import android.content.Context;

import com.flowcrypt.email.api.retrofit.node.NodeCallsExecutor;
import com.flowcrypt.email.api.retrofit.response.model.node.NodeKeyDetails;
import com.flowcrypt.email.database.dao.KeysDao;
import com.flowcrypt.email.database.dao.source.KeysDaoSource;
import com.flowcrypt.email.security.KeyStoreCryptoManager;
import com.flowcrypt.email.security.model.PrivateKeySourceType;

import org.apache.commons.io.IOUtils;

import java.util.List;

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
        .open("pgp/temp-sec.asc"), "UTF-8");
    Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
    List<NodeKeyDetails> details = NodeCallsExecutor.parseKeys(armoredPrivateKey);

    NodeKeyDetails nodeKeyDetails = details.get(0);

    KeysDao keysDao = new KeysDao();
    keysDao.setLongId(nodeKeyDetails.getLongId());
    keysDao.setPrivateKeySourceType(PrivateKeySourceType.NEW);

    String randomVector = KeyStoreCryptoManager.normalizeAlgorithmParameterSpecString(nodeKeyDetails.getLongId());

    KeyStoreCryptoManager keyStoreCryptoManager = new KeyStoreCryptoManager(appContext);

    String encryptedPrivateKey = keyStoreCryptoManager.encrypt(nodeKeyDetails.getPrivateKey(), randomVector);
    keysDao.setPrivateKey(encryptedPrivateKey);
    keysDao.setPublicKey(nodeKeyDetails.getPublicKey());

    String encryptedPassphrase = keyStoreCryptoManager.encrypt(TEMP_PASSPHRASE, randomVector);
    keysDao.setPassphrase(encryptedPassphrase);
    new KeysDaoSource().addRow(appContext, keysDao);
  }
}
