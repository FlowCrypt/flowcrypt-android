/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (tom@cryptup.org).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.security;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.preference.PreferenceManager;

import com.flowcrypt.email.database.dao.source.KeysDaoSource;
import com.flowcrypt.email.database.provider.FlowcryptContract;
import com.flowcrypt.email.security.model.PrivateKeyInfo;
import com.flowcrypt.email.test.PgpKeyInfo;

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
     * Reset a security information of a current user.
     */
    public static void cleanSecurityInfo(Context context) {
        context.getContentResolver().delete(Uri.parse(FlowcryptContract.AUTHORITY_URI
                + "/" + FlowcryptContract.CLEAN_DATABASE), null, null);

        SharedPreferences sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor edit = sharedPreferences.edit();
        edit.remove(KeyStoreCryptoManager.PREFERENCE_KEY_SECRET);
        edit.apply();
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
}
