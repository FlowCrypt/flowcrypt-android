/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
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
import com.flowcrypt.email.js.Js;
import com.flowcrypt.email.js.PasswordStrength;
import com.flowcrypt.email.js.PgpKey;
import com.flowcrypt.email.js.PgpKeyInfo;
import com.flowcrypt.email.security.model.PrivateKeyInfo;
import com.flowcrypt.email.util.exception.PrivateKeyStrengthException;
import com.nulabinc.zxcvbn.Zxcvbn;

import java.util.ArrayList;
import java.util.List;

/**
 * This class help to receive security information.
 *
 * @author DenBond7
 *         Date: 05.05.2017
 *         Time: 13:08
 *         E-mail: DenBond7@gmail.com
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
        Cursor cursor = context.getContentResolver().query(
                new KeysDaoSource().getBaseContentUri(), null, null, null, null);

        KeyStoreCryptoManager keyStoreCryptoManager = new KeyStoreCryptoManager(context);

        if (cursor != null && cursor.moveToFirst()) {
            do {
                String longId =
                        cursor.getString(cursor.getColumnIndex(KeysDaoSource.COL_LONG_ID));

                String randomVector =
                        KeyStoreCryptoManager.normalizeAlgorithmParameterSpecString(longId);

                String privateKey = keyStoreCryptoManager.decrypt(cursor.getString(cursor
                        .getColumnIndex(KeysDaoSource.COL_PRIVATE_KEY)), randomVector);
                String passphrase = keyStoreCryptoManager.decrypt(cursor.getString(cursor
                        .getColumnIndex(KeysDaoSource.COL_PASSPHRASE)), randomVector);

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
     * Check is backup keys exist in the database.
     *
     * @return <tt>Boolean</tt> true if exists one or more private keys, false otherwise;
     */
    public static boolean isBackupKeysExist(Context context) {
        Cursor cursor = context.getContentResolver().query(
                new KeysDaoSource().getBaseContentUri(), null, null, null, null);

        boolean isBackupKeysExist = false;
        if (cursor != null && cursor.moveToFirst()) {
            isBackupKeysExist = cursor.getCount() > 0;
        }

        if (cursor != null) {
            cursor.close();
        }

        return isBackupKeysExist;
    }

    /**
     * Generate a new name for the private key which will be exported.
     *
     * @param email The user email.
     * @return A generated name for a new file.
     */
    public static String generateNameForPrivateKey(String email) {
        String sanitizedEmail = email.replaceAll("[^a-z0-9]", "");
        return "flowcrypt-backup-" + sanitizedEmail + ".key";
    }

    /**
     * Generate a private keys backup for the given account.
     *
     * @param context    Interface to global information about an application environment.
     * @param js         An instance of {@link Js}
     * @param accountDao The given account
     * @return A string which includes private keys
     */
    public static String generatePrivateKeysBackup(Context context, Js js, AccountDao accountDao) throws
            PrivateKeyStrengthException {
        StringBuilder armoredPrivateKeysBackupStringBuilder = new StringBuilder();
        Zxcvbn zxcvbn = new Zxcvbn();
        List<String> longIdListOfAccountPrivateKeys = new UserIdEmailsKeysDaoSource().getLongIdsByEmail
                (context, accountDao.getEmail());

        PgpKeyInfo[] pgpKeyInfoArray = js.getStorageConnector().getFilteredPgpPrivateKeys
                (longIdListOfAccountPrivateKeys.toArray(new String[0]));

        if (pgpKeyInfoArray == null || pgpKeyInfoArray.length == 0) {
            throw new IllegalArgumentException("There are no private keys for " + accountDao.getEmail());
        }

        for (int i = 0; i < pgpKeyInfoArray.length; i++) {
            PgpKeyInfo pgpKeyInfo = pgpKeyInfoArray[i];

            String passPhrase = js.getStorageConnector().getPassphrase(pgpKeyInfo.getLongid());

            if (TextUtils.isEmpty(passPhrase)) {
                throw new PrivateKeyStrengthException("The pass phrase of some of your key(s) is empty!");
            }

            PasswordStrength passwordStrength = js.crypto_password_estimate_strength(
                    zxcvbn.measure(passPhrase, js.crypto_password_weak_words()).getGuesses());

            if (passwordStrength != null) {
                switch (passwordStrength.getWord()) {
                    case Constants.PASSWORD_QUALITY_WEAK:
                    case Constants.PASSWORD_QUALITY_POOR:
                        throw new PrivateKeyStrengthException("The pass phrase of some of your key(s) is too weak!");
                }
            }

            PgpKey pgpKey = js.crypto_key_read(pgpKeyInfo.getPrivate());
            pgpKey.encrypt(passPhrase);
            armoredPrivateKeysBackupStringBuilder.append(i > 0 ? "\n" + pgpKey.armor() : pgpKey.armor());
        }

        return armoredPrivateKeysBackupStringBuilder.toString();
    }
}
