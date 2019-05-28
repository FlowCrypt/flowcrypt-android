/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.security;

import android.content.Context;
import android.database.Cursor;
import android.text.TextUtils;

import com.flowcrypt.email.Constants;
import com.flowcrypt.email.api.retrofit.node.NodeCallsExecutor;
import com.flowcrypt.email.api.retrofit.response.model.node.NodeKeyDetails;
import com.flowcrypt.email.api.retrofit.response.node.EncryptKeyResult;
import com.flowcrypt.email.api.retrofit.response.node.ZxcvbnStrengthBarResult;
import com.flowcrypt.email.database.dao.source.AccountDao;
import com.flowcrypt.email.database.dao.source.ContactsDaoSource;
import com.flowcrypt.email.database.dao.source.KeysDaoSource;
import com.flowcrypt.email.database.dao.source.UserIdEmailsKeysDaoSource;
import com.flowcrypt.email.model.PgpContact;
import com.flowcrypt.email.model.PgpKeyInfo;
import com.flowcrypt.email.security.model.PrivateKeyInfo;
import com.flowcrypt.email.util.exception.DifferentPassPhrasesException;
import com.flowcrypt.email.util.exception.NoKeyAvailableException;
import com.flowcrypt.email.util.exception.NoPrivateKeysAvailableException;
import com.flowcrypt.email.util.exception.NodeException;
import com.flowcrypt.email.util.exception.PrivateKeyStrengthException;
import com.google.android.gms.common.util.CollectionUtils;
import com.nulabinc.zxcvbn.Zxcvbn;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This class help to receive security information.
 *
 * @author DenBond7
 * Date: 05.05.2017
 * Time: 13:08
 * E-mail: DenBond7@gmail.com
 */

public class SecurityUtils {

  /**
   * Get a PrivateKeyInfo list.
   *
   * @param context Interface to global information about an application environment.
   * @return <tt>List<PrivateKeyInfo></tt> Return a list of PrivateKeyInfo objects.
   */
  public static List<PrivateKeyInfo> getPrivateKeysInfo(Context context) throws Exception {
    ArrayList<PrivateKeyInfo> privateKeysInfo = new ArrayList<>();
    Cursor cursor = context.getContentResolver().query(new KeysDaoSource().getBaseContentUri(), null, null, null, null);

    KeyStoreCryptoManager keyStoreCryptoManager = KeyStoreCryptoManager.getInstance(context);

    if (cursor != null && cursor.moveToFirst()) {
      do {
        String longId = cursor.getString(cursor.getColumnIndex(KeysDaoSource.COL_LONG_ID));
        String pubKey = cursor.getString(cursor.getColumnIndex(KeysDaoSource.COL_PUBLIC_KEY));

        String randomVector = KeyStoreCryptoManager.normalizeAlgorithmParameterSpecString(longId);

        String prvKey = keyStoreCryptoManager.decrypt(cursor.getString(
            cursor.getColumnIndex(KeysDaoSource.COL_PRIVATE_KEY)), randomVector);
        String passphrase = keyStoreCryptoManager.decrypt(cursor.getString(
            cursor.getColumnIndex(KeysDaoSource.COL_PASSPHRASE)), randomVector);

        PgpKeyInfo pgpKeyInfo = new PgpKeyInfo(longId, prvKey, pubKey);
        PrivateKeyInfo privateKeyInfo = new PrivateKeyInfo(pgpKeyInfo, passphrase);

        privateKeysInfo.add(privateKeyInfo);
      } while (cursor.moveToNext());
    }

    if (cursor != null) {
      cursor.close();
    }
    return privateKeysInfo;
  }

  /**
   * Check is backup of keys exist in the database.
   *
   * @return <tt>Boolean</tt> true if exists one or more private keys, false otherwise;
   */
  public static boolean hasBackup(Context context) {
    Cursor cursor = context.getContentResolver().query(new KeysDaoSource().getBaseContentUri(), null, null, null, null);

    boolean hasBackup = false;
    if (cursor != null && cursor.moveToFirst()) {
      hasBackup = cursor.getCount() > 0;
    }

    if (cursor != null) {
      cursor.close();
    }

    return hasBackup;
  }

  /**
   * Generate a new name for the private key which will be exported.
   *
   * @param email The user email.
   * @return A generated name for a new file.
   */
  public static String genPrivateKeyName(String email) {
    String sanitizedEmail = email.replaceAll("[^a-z0-9]", "");
    return "flowcrypt-backup-" + sanitizedEmail + ".key";
  }

  /**
   * Generate a private keys backup for the given account.
   *
   * @param context Interface to global information about an application environment.
   * @param account The given account
   * @return A string which includes private keys
   */
  public static String genPrivateKeysBackup(Context context, AccountDao account)
      throws PrivateKeyStrengthException, DifferentPassPhrasesException,
      NoPrivateKeysAvailableException, IOException, NodeException {
    StringBuilder builder = new StringBuilder();
    String email = account.getEmail();
    List<String> longIdsByEmail = new UserIdEmailsKeysDaoSource().getLongIdsByEmail(context, email);
    String[] longids = longIdsByEmail.toArray(new String[0]);
    KeysStorageImpl keysStorage = KeysStorageImpl.getInstance(context);
    List<PgpKeyInfo> pgpKeyInfoList = keysStorage.getFilteredPgpPrivateKeys(longids);

    if (CollectionUtils.isEmpty(pgpKeyInfoList)) {
      throw new NoPrivateKeysAvailableException(context, account.getEmail());
    }

    String firstPassPhrase = null;

    for (int i = 0; i < pgpKeyInfoList.size(); i++) {
      PgpKeyInfo pgpKeyInfo = pgpKeyInfoList.get(i);

      String passPhrase = keysStorage.getPassphrase(pgpKeyInfo.getLongid());

      if (i == 0) {
        firstPassPhrase = passPhrase;
      } else if (!passPhrase.equals(firstPassPhrase)) {
        throw new DifferentPassPhrasesException("The keys have different pass phrase");
      }

      if (TextUtils.isEmpty(passPhrase)) {
        throw new PrivateKeyStrengthException("Empty pass phrase");
      }

      Zxcvbn zxcvbn = new Zxcvbn();
      double measure = zxcvbn.measure(passPhrase, Arrays.asList(Constants.PASSWORD_WEAK_WORDS)).getGuesses();
      ZxcvbnStrengthBarResult passwordStrength = NodeCallsExecutor.zxcvbnStrengthBar(measure);

      if (passwordStrength != null) {
        switch (passwordStrength.getWord().getWord()) {
          case Constants.PASSWORD_QUALITY_WEAK:
          case Constants.PASSWORD_QUALITY_POOR:
            throw new PrivateKeyStrengthException("Pass phrase too weak");
        }
      }

      List<NodeKeyDetails> nodeKeyDetailsList = NodeCallsExecutor.parseKeys(pgpKeyInfo.getPrivate());
      NodeKeyDetails keyDetails = nodeKeyDetailsList.get(0);

      String encryptedKey;

      if (keyDetails.isDecrypted()) {
        EncryptKeyResult encryptKeyResult = NodeCallsExecutor.encryptKey(pgpKeyInfo.getPrivate(), passPhrase);
        encryptedKey = encryptKeyResult.getEncryptedKey();
      } else {
        encryptedKey = keyDetails.getPrivateKey();
      }

      builder.append(i > 0 ? "\n" + encryptedKey : encryptedKey);
    }

    return builder.toString();
  }

  /**
   * Get public keys for recipients + keys of the sender;
   *
   * @param context     Interface to global information about an application environment.
   * @param contacts    A list which contains recipients
   * @param account     The given account
   * @param senderEmail The sender email
   * @return A list of public keys.
   * @throws NoKeyAvailableException
   */
  public static List<String> getRecipientsPubKeys(Context context, List<String> contacts,
                                                  AccountDao account, String senderEmail)
      throws NoKeyAvailableException, IOException, NodeException {
    ArrayList<String> publicKeys = new ArrayList<>();
    List<PgpContact> pgpContacts = new ContactsDaoSource().getPgpContacts(context, contacts);

    for (PgpContact pgpContact : pgpContacts) {
      if (pgpContact != null && !TextUtils.isEmpty(pgpContact.getPubkey())) {
        publicKeys.add(pgpContact.getPubkey());
      }
    }

    publicKeys.add(getSenderPublicKey(context, account, senderEmail));

    return publicKeys;
  }

  /**
   * Get a public key of the sender;
   *
   * @param context     Interface to global information about an application environment.
   * @param account     The given account
   * @param senderEmail The sender email
   * @return <tt>String</tt> The sender public key.
   * @throws NoKeyAvailableException
   */
  public static String getSenderPublicKey(Context context, AccountDao account, String senderEmail) throws
      NoKeyAvailableException, IOException, NodeException {
    UserIdEmailsKeysDaoSource userIdEmailsKeysDaoSource = new UserIdEmailsKeysDaoSource();
    List<String> longIds = userIdEmailsKeysDaoSource.getLongIdsByEmail(context, senderEmail);

    if (longIds.isEmpty()) {
      if (account.getEmail().equalsIgnoreCase(senderEmail)) {
        throw new NoKeyAvailableException(context, account.getEmail(), null);
      } else {
        longIds = userIdEmailsKeysDaoSource.getLongIdsByEmail(context, account.getEmail());
        if (longIds.isEmpty()) {
          throw new NoKeyAvailableException(context, account.getEmail(), senderEmail);
        }
      }
    }

    PgpKeyInfo pgpKeyInfo = KeysStorageImpl.getInstance(context).getPgpPrivateKey(longIds.get(0));
    if (pgpKeyInfo != null) {
      List<NodeKeyDetails> details = NodeCallsExecutor.parseKeys(pgpKeyInfo.getPrivate());
      if (CollectionUtils.isEmpty(details)) {
        throw new IllegalStateException("There are no details about the given private key");
      }

      if (details.size() > 1) {
        throw new IllegalStateException("A wrong private key");
      }

      return details.get(0).getPublicKey();
    }

    throw new IllegalArgumentException("Internal error: PgpKeyInfo is null!");
  }
}
