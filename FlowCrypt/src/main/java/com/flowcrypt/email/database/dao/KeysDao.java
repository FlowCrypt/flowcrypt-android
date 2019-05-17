/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.database.dao;

import android.text.TextUtils;

import com.flowcrypt.email.api.retrofit.response.model.node.NodeKeyDetails;
import com.flowcrypt.email.model.KeyDetails;
import com.flowcrypt.email.security.KeyStoreCryptoManager;
import com.flowcrypt.email.security.model.PrivateKeySourceType;

/**
 * This class describe a key information object.
 *
 * @author DenBond7
 * Date: 13.05.2017
 * Time: 12:54
 * E-mail: DenBond7@gmail.com
 */

public class KeysDao {

  private String longId;
  private PrivateKeySourceType privateKeySourceType;
  private String publicKey;
  private String privateKey;
  private String passphrase;

  public KeysDao() {
  }

  public KeysDao(String longId, PrivateKeySourceType privateKeySourceType, String publicKey,
                 String privateKey, String passphrase) {
    this.longId = longId;
    this.privateKeySourceType = privateKeySourceType;
    this.publicKey = publicKey;
    this.privateKey = privateKey;
    this.passphrase = passphrase;
  }

  /**
   * Generate {@link KeysDao} using input parameters.
   * This method use {@link NodeKeyDetails#getLongId()} for generate an algorithm parameter spec String and
   * {@link KeyStoreCryptoManager} for generate encrypted version of the private key and password.
   *
   * @param keyStoreCryptoManager A {@link KeyStoreCryptoManager} which will bu used to encrypt
   *                              information about a key;
   * @param type                  The private key born type
   * @param nodeKeyDetails        Key details;
   * @param passphrase            A passphrase which user provided;
   */
  public static KeysDao generateKeysDao(KeyStoreCryptoManager keyStoreCryptoManager, KeyDetails.Type type,
                                        NodeKeyDetails nodeKeyDetails, String passphrase) throws Exception {
    KeysDao keysDao = generateKeysDao(keyStoreCryptoManager, nodeKeyDetails, passphrase);

    switch (type) {
      case EMAIL:
        keysDao.setPrivateKeySourceType(PrivateKeySourceType.BACKUP);
        break;

      case FILE:
      case CLIPBOARD:
        keysDao.setPrivateKeySourceType(PrivateKeySourceType.IMPORT);
        break;

      case NEW:
        keysDao.setPrivateKeySourceType(PrivateKeySourceType.NEW);
        break;
    }

    return keysDao;
  }

  /**
   * Generate {@link KeysDao} using input parameters.
   * This method use {@link NodeKeyDetails#getLongId()} for generate an algorithm parameter spec String and
   * {@link KeyStoreCryptoManager} for generate encrypted version of the private key and password.
   *
   * @param keyStoreCryptoManager A {@link KeyStoreCryptoManager} which will bu used to encrypt
   *                              information about a key;
   * @param nodeKeyDetails        Key details;
   * @param passphrase            A passphrase which user provided;
   */
  public static KeysDao generateKeysDao(KeyStoreCryptoManager keyStoreCryptoManager, NodeKeyDetails nodeKeyDetails,
                                        String passphrase) throws Exception {
    if (nodeKeyDetails.isDecrypted()) {
      throw new IllegalArgumentException("Error. The key is decrypted!");
    }

    KeysDao keysDao = new KeysDao();
    keysDao.setLongId(nodeKeyDetails.getLongId());

    String randomVector;

    if (TextUtils.isEmpty(nodeKeyDetails.getLongId())) {
      throw new IllegalArgumentException("longid == null");
    } else {
      randomVector = KeyStoreCryptoManager.normalizeAlgorithmParameterSpecString(nodeKeyDetails.getLongId());
    }

    String encryptedPrivateKey = keyStoreCryptoManager.encrypt(nodeKeyDetails.getPrivateKey(), randomVector);
    keysDao.setPrivateKey(encryptedPrivateKey);
    keysDao.setPublicKey(nodeKeyDetails.getPublicKey());

    String encryptedPassphrase = keyStoreCryptoManager.encrypt(passphrase, randomVector);
    keysDao.setPassphrase(encryptedPassphrase);
    return keysDao;
  }

  public String getLongId() {
    return longId;
  }

  public void setLongId(String longId) {
    this.longId = longId;
  }

  public PrivateKeySourceType getPrivateKeySourceType() {
    return privateKeySourceType;
  }

  public void setPrivateKeySourceType(PrivateKeySourceType privateKeySourceType) {
    this.privateKeySourceType = privateKeySourceType;
  }

  public String getPublicKey() {
    return publicKey;
  }

  public void setPublicKey(String publicKey) {
    this.publicKey = publicKey;
  }

  public String getPrivateKey() {
    return privateKey;
  }

  public void setPrivateKey(String privateKey) {
    this.privateKey = privateKey;
  }

  public String getPassphrase() {
    return passphrase;
  }

  public void setPassphrase(String passphrase) {
    this.passphrase = passphrase;
  }
}
