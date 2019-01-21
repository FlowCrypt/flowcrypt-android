/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.security;

import android.content.Context;
import android.database.Cursor;
import android.text.TextUtils;

import com.flowcrypt.email.Constants;
import com.flowcrypt.email.database.dao.source.AccountDao;
import com.flowcrypt.email.database.dao.source.KeysDaoSource;
import com.flowcrypt.email.database.dao.source.UserIdEmailsKeysDaoSource;
import com.flowcrypt.email.js.PasswordStrength;
import com.flowcrypt.email.js.PgpContact;
import com.flowcrypt.email.js.PgpKey;
import com.flowcrypt.email.js.PgpKeyInfo;
import com.flowcrypt.email.js.core.Js;
import com.flowcrypt.email.security.model.PrivateKeyInfo;
import com.flowcrypt.email.util.exception.DifferentPassPhrasesException;
import com.flowcrypt.email.util.exception.NoKeyAvailableException;
import com.flowcrypt.email.util.exception.NoPrivateKeysAvailableException;
import com.flowcrypt.email.util.exception.PrivateKeyStrengthException;
import com.nulabinc.zxcvbn.Zxcvbn;

import java.util.ArrayList;
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

    KeyStoreCryptoManager keyStoreCryptoManager = new KeyStoreCryptoManager(context);

    if (cursor != null && cursor.moveToFirst()) {
      do {
        String longId = cursor.getString(cursor.getColumnIndex(KeysDaoSource.COL_LONG_ID));

        String randomVector = KeyStoreCryptoManager.normalizeAlgorithmParameterSpecString(longId);

        String privateKey = keyStoreCryptoManager.decrypt(cursor.getString(
            cursor.getColumnIndex(KeysDaoSource.COL_PRIVATE_KEY)), randomVector);
        String passphrase = keyStoreCryptoManager.decrypt(cursor.getString(
            cursor.getColumnIndex(KeysDaoSource.COL_PASSPHRASE)), randomVector);

        PgpKeyInfo pgpKeyInfo = new PgpKeyInfo(privateKey, longId);
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
   * @param context       Interface to global information about an application environment.
   * @param js            An instance of {@link Js}
   * @param account       The given account
   * @param checkWeakPass true if need to check is a pass phrase is too weak.
   * @return A string which includes private keys
   */
  public static String genPrivateKeysBackup(Context context, Js js, AccountDao account, boolean checkWeakPass)
      throws PrivateKeyStrengthException, DifferentPassPhrasesException, NoPrivateKeysAvailableException {
    StringBuilder builder = new StringBuilder();
    Zxcvbn zxcvbn = new Zxcvbn();
    String email = account.getEmail();
    List<String> longIdsByEmail = new UserIdEmailsKeysDaoSource().getLongIdsByEmail(context, email);
    String[] longids = longIdsByEmail.toArray(new String[0]);
    PgpKeyInfo[] pgpKeyInfoArray = js.getStorageConnector().getFilteredPgpPrivateKeys(longids);

    if (pgpKeyInfoArray == null || pgpKeyInfoArray.length == 0) {
      throw new NoPrivateKeysAvailableException(context, account.getEmail());
    }

    String firstPassPhrase = null;

    for (int i = 0; i < pgpKeyInfoArray.length; i++) {
      PgpKeyInfo pgpKeyInfo = pgpKeyInfoArray[i];

      String passPhrase = js.getStorageConnector().getPassphrase(pgpKeyInfo.getLongid());

      if (i == 0) {
        firstPassPhrase = passPhrase;
      } else if (!passPhrase.equals(firstPassPhrase)) {
        throw new DifferentPassPhrasesException("The keys have different pass phrase");
      }

      if (TextUtils.isEmpty(passPhrase)) {
        throw new PrivateKeyStrengthException("Empty pass phrase");
      }

      PasswordStrength passwordStrength = js.crypto_password_estimate_strength(
          zxcvbn.measure(passPhrase, js.crypto_password_weak_words()).getGuesses());

      if (passwordStrength != null && checkWeakPass) {
        switch (passwordStrength.getWord()) {
          case Constants.PASSWORD_QUALITY_WEAK:
          case Constants.PASSWORD_QUALITY_POOR:
            throw new PrivateKeyStrengthException("Pass phrase too weak");
        }
      }

      PgpKey pgpKey = js.crypto_key_read(pgpKeyInfo.getPrivate());
      pgpKey.encrypt(passPhrase);
      builder.append(i > 0 ? "\n" + pgpKey.armor() : pgpKey.armor());
    }

    return builder.toString();
  }

  /**
   * Get public keys for recipients + keys of the sender;
   *
   * @param context     Interface to global information about an application environment.
   * @param js          An instance of {@link Js}
   * @param pgpContacts An array which contains recipients
   * @param account     The given account
   * @param senderEmail The sender email
   * @return <tt>String[]</tt> An array of public keys.
   * @throws NoKeyAvailableException
   */
  public static String[] getRecipientsPubKeys(Context context, Js js, PgpContact[] pgpContacts, AccountDao account,
                                              String senderEmail) throws NoKeyAvailableException {
    ArrayList<String> publicKeys = new ArrayList<>();
    for (PgpContact pgpContact : pgpContacts) {
      if (!TextUtils.isEmpty(pgpContact.getPubkey())) {
        publicKeys.add(pgpContact.getPubkey());
      }
    }

    publicKeys.add(getSenderPublicKey(context, js, account, senderEmail));

    return publicKeys.toArray(new String[0]);
  }

  /**
   * Get a public key of the sender;
   *
   * @param context     Interface to global information about an application environment.
   * @param js          An instance of {@link Js}
   * @param account     The given account
   * @param senderEmail The sender email
   * @return <tt>String</tt> The sender public key.
   * @throws NoKeyAvailableException
   */
  public static String getSenderPublicKey(Context context, Js js, AccountDao account, String senderEmail) throws
      NoKeyAvailableException {
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

    PgpKeyInfo pgpKeyInfo = js.getStorageConnector().getPgpPrivateKey(longIds.get(0));
    if (pgpKeyInfo != null) {
      PgpKey pgpKey = js.crypto_key_read(pgpKeyInfo.getPrivate());
      if (pgpKey != null) {
        return pgpKey.toPublic().armor();
      }
    }

    throw new IllegalArgumentException("Internal error: PgpKeyInfo is null!");
  }
}
