/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (human@flowcrypt.com).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.database.dao.source;

import android.accounts.Account;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.flowcrypt.email.api.email.JavaEmailConstants;
import com.flowcrypt.email.api.email.gmail.GmailConstants;
import com.flowcrypt.email.api.email.model.AuthCredentials;
import com.flowcrypt.email.api.email.model.SecurityType;
import com.flowcrypt.email.security.KeyStoreCryptoManager;
import com.flowcrypt.email.util.exception.ExceptionUtil;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

/**
 * This class describe creating of table which has name
 * {@link AccountDaoSource#TABLE_NAME_ACCOUNTS}, add, delete and update rows.
 *
 * @author Denis Bondarenko
 *         Date: 14.07.2017
 *         Time: 17:43
 *         E-mail: DenBond7@gmail.com
 */

public class AccountDaoSource extends BaseDaoSource {
    public static final String TABLE_NAME_ACCOUNTS = "accounts";

    public static final String COL_EMAIL = "email";
    public static final String COL_ACCOUNT_TYPE = "account_type";
    public static final String COL_DISPLAY_NAME = "display_name";
    public static final String COL_GIVEN_NAME = "given_name";
    public static final String COL_FAMILY_NAME = "family_name";
    public static final String COL_PHOTO_URL = "photo_url";
    public static final String COL_IS_ENABLE = "is_enable";
    public static final String COL_IS_ACTIVE = "is_active";
    public static final String COL_USERNAME = "username";
    public static final String COL_PASSWORD = "password";
    public static final String COL_IMAP_SERVER = "imap_server";
    public static final String COL_IMAP_PORT = "imap_port";
    public static final String COL_IMAP_IS_USE_SSL_TLS = "imap_is_use_ssl_tls";
    public static final String COL_IMAP_IS_USE_STARTTLS = "imap_is_use_starttls";
    public static final String COL_IMAP_AUTH_MECHANISMS = "imap_auth_mechanisms";
    public static final String COL_SMTP_SERVER = "smtp_server";
    public static final String COL_SMTP_PORT = "smtp_port";
    public static final String COL_SMTP_IS_USE_SSL_TLS = "smtp_is_use_ssl_tls";
    public static final String COL_SMTP_IS_USE_STARTTLS = "smtp_is_use_starttls";
    public static final String COL_SMTP_AUTH_MECHANISMS = "smtp_auth_mechanisms";
    public static final String COL_SMTP_IS_USE_CUSTOM_SIGN = "smtp_is_use_custom_sign";
    public static final String COL_SMTP_USERNAME = "smtp_username";
    public static final String COL_SMTP_PASSWORD = "smtp_password";

    public static final String ACCOUNTS_TABLE_SQL_CREATE = "CREATE TABLE IF NOT EXISTS " +
            TABLE_NAME_ACCOUNTS + " (" +
            BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            COL_EMAIL + " VARCHAR(100) NOT NULL, " +
            COL_ACCOUNT_TYPE + " VARCHAR(100) DEFAULT NULL, " +
            COL_DISPLAY_NAME + " VARCHAR(100) DEFAULT NULL, " +
            COL_GIVEN_NAME + " VARCHAR(100) DEFAULT NULL, " +
            COL_FAMILY_NAME + " VARCHAR(100) DEFAULT NULL, " +
            COL_PHOTO_URL + " TEXT DEFAULT NULL, " +
            COL_IS_ENABLE + " INTEGER DEFAULT 1, " +
            COL_IS_ACTIVE + " INTEGER DEFAULT 0, " +
            COL_USERNAME + " TEXT NOT NULL, " +
            COL_PASSWORD + " TEXT NOT NULL, " +
            COL_IMAP_SERVER + " TEXT NOT NULL, " +
            COL_IMAP_PORT + " INTEGER DEFAULT 143, " +
            COL_IMAP_IS_USE_SSL_TLS + " INTEGER DEFAULT 0, " +
            COL_IMAP_IS_USE_STARTTLS + " INTEGER DEFAULT 0, " +
            COL_IMAP_AUTH_MECHANISMS + " TEXT, " +
            COL_SMTP_SERVER + " TEXT NOT NULL, " +
            COL_SMTP_PORT + " INTEGER DEFAULT 25, " +
            COL_SMTP_IS_USE_SSL_TLS + " INTEGER DEFAULT 0, " +
            COL_SMTP_IS_USE_STARTTLS + " INTEGER DEFAULT 0, " +
            COL_SMTP_AUTH_MECHANISMS + " TEXT, " +
            COL_SMTP_IS_USE_CUSTOM_SIGN + " INTEGER DEFAULT 0, " +
            COL_SMTP_USERNAME + " TEXT DEFAULT NULL, " +
            COL_SMTP_PASSWORD + " TEXT DEFAULT NULL " + ");";

    public static final String CREATE_INDEX_EMAIL_TYPE_IN_ACCOUNTS = "CREATE UNIQUE INDEX IF NOT EXISTS "
            + COL_EMAIL + "_" + COL_ACCOUNT_TYPE + "_in_" + TABLE_NAME_ACCOUNTS + " ON " + TABLE_NAME_ACCOUNTS +
            " (" + COL_EMAIL + ", " + COL_ACCOUNT_TYPE + ")";

    /**
     * Generate the {@link AccountDao} from the current cursor position;
     *
     * @param context Interface to global information about an application environment;
     * @param cursor  The cursor from which to get the data.
     * @return {@link AccountDao}.
     */
    public static AccountDao getCurrentAccountDao(Context context, Cursor cursor) {
        AuthCredentials authCredentials = null;
        try {
            KeyStoreCryptoManager keyStoreCryptoManager = new KeyStoreCryptoManager(context);
            authCredentials = getCurrentAuthCredentialsFromCursor(keyStoreCryptoManager, cursor);
        } catch (Exception e) {
            e.printStackTrace();
            ExceptionUtil.handleError(e);
        }

        return new AccountDao(
                cursor.getString(cursor.getColumnIndex(COL_EMAIL)),
                cursor.getString(cursor.getColumnIndex(COL_ACCOUNT_TYPE)),
                cursor.getString(cursor.getColumnIndex(COL_DISPLAY_NAME)),
                cursor.getString(cursor.getColumnIndex(COL_GIVEN_NAME)),
                cursor.getString(cursor.getColumnIndex(COL_FAMILY_NAME)),
                cursor.getString(cursor.getColumnIndex(COL_PHOTO_URL)), authCredentials);
    }

    /**
     * Get the current {@link AuthCredentials} object from the current {@link Cursor} position.
     *
     * @param keyStoreCryptoManager The manager which does encryption/decryption work.
     * @param cursor                The cursor from which to get the data.
     * @return Generated {@link AuthCredentials} object.
     * @throws NoSuchPaddingException
     * @throws NoSuchAlgorithmException
     * @throws IllegalBlockSizeException
     * @throws BadPaddingException
     * @throws InvalidKeyException
     * @throws IOException
     */
    public static AuthCredentials getCurrentAuthCredentialsFromCursor(KeyStoreCryptoManager keyStoreCryptoManager,
                                                                      Cursor cursor) throws NoSuchPaddingException,
            NoSuchAlgorithmException, IllegalBlockSizeException, BadPaddingException, InvalidKeyException, IOException {

        SecurityType.Option imapSecurityTypeOption = SecurityType.Option.NONE;

        if (cursor.getInt(cursor.getColumnIndex(COL_IMAP_IS_USE_SSL_TLS)) == 1) {
            imapSecurityTypeOption = SecurityType.Option.SSL_TLS;
        } else if (cursor.getInt(cursor.getColumnIndex(COL_IMAP_IS_USE_STARTTLS)) == 1) {
            imapSecurityTypeOption = SecurityType.Option.STARTLS;
        }

        SecurityType.Option smtpSecurityTypeOption = SecurityType.Option.NONE;

        if (cursor.getInt(cursor.getColumnIndex(COL_SMTP_IS_USE_SSL_TLS)) == 1) {
            smtpSecurityTypeOption = SecurityType.Option.SSL_TLS;
        } else if (cursor.getInt(cursor.getColumnIndex(COL_SMTP_IS_USE_STARTTLS)) == 1) {
            smtpSecurityTypeOption = SecurityType.Option.STARTLS;
        }

        String originalPassword = cursor.getString(cursor.getColumnIndex(COL_PASSWORD));

        //fixed a bug when try to decrypting the template password.
        // See https://github.com/FlowCrypt/flowcrypt-android/issues/168
        if (originalPassword.equalsIgnoreCase("password")) {
            originalPassword = "";
        }

        return new AuthCredentials.Builder().setEmail(cursor.getString(cursor.getColumnIndex(COL_EMAIL)))
                .setUsername(cursor.getString(cursor.getColumnIndex(COL_USERNAME)))
                .setPassword(keyStoreCryptoManager.decryptWithRSA(originalPassword))
                .setImapServer(cursor.getString(cursor.getColumnIndex(COL_IMAP_SERVER)))
                .setImapPort(cursor.getInt(cursor.getColumnIndex(COL_IMAP_PORT)))
                .setImapSecurityTypeOption(imapSecurityTypeOption)
                .setSmtpServer(cursor.getString(cursor.getColumnIndex(COL_SMTP_SERVER)))
                .setSmtpPort(cursor.getInt(cursor.getColumnIndex(COL_SMTP_PORT)))
                .setSmtpSecurityTypeOption(smtpSecurityTypeOption)
                .setIsUseCustomSignInForSmtp(cursor.getInt(cursor.getColumnIndex(COL_SMTP_IS_USE_CUSTOM_SIGN)) == 1)
                .setSmtpSigInUsername(cursor.getString(cursor.getColumnIndex(COL_SMTP_USERNAME)))
                .setSmtpSignInPassword(keyStoreCryptoManager.decryptWithRSA(
                        cursor.getString(cursor.getColumnIndex(COL_SMTP_PASSWORD))))
                .build();
    }

    @Override
    public String getTableName() {
        return TABLE_NAME_ACCOUNTS;
    }

    /**
     * Save information about an account using the {@link GoogleSignInAccount};
     *
     * @param context             Interface to global information about an application environment;
     * @param googleSignInAccount Reflecting the user's sign in information.
     * @return The created {@link Uri} or null;
     */
    public Uri addRow(Context context, GoogleSignInAccount googleSignInAccount) {
        ContentResolver contentResolver = context.getContentResolver();
        if (googleSignInAccount != null && contentResolver != null) {
            ContentValues contentValues = generateContentValues(googleSignInAccount);
            if (contentValues == null) return null;

            return contentResolver.insert(getBaseContentUri(), contentValues);
        } else return null;
    }

    /**
     * Save information about an account using the {@link AuthCredentials};
     *
     * @param context         Interface to global information about an application environment;
     * @param authCredentials The sign-in settings of IMAP and SMTP servers.
     * @return The created {@link Uri} or null;
     * @throws Exception An exception maybe occurred when encrypt the user password.
     */
    public Uri addRow(Context context, AuthCredentials authCredentials) throws Exception {
        ContentResolver contentResolver = context.getContentResolver();
        if (authCredentials != null && contentResolver != null) {
            ContentValues contentValues = generateContentValuesWithEncryptedPassword(context, authCredentials);
            if (contentValues == null) return null;

            return contentResolver.insert(getBaseContentUri(), contentValues);
        } else return null;
    }

    /**
     * Get an active {@link AccountDao} object from the local database.
     *
     * @param context Interface to global information about an application environment.
     * @return The {@link AccountDao};
     */
    public AccountDao getActiveAccountInformation(Context context) {
        Cursor cursor = context.getContentResolver().query(
                getBaseContentUri(), null, AccountDaoSource.COL_IS_ACTIVE + " = ?", new String[]{"1"}, null);

        if (cursor != null && cursor.moveToFirst()) {
            return getCurrentAccountDao(context, cursor);
        }

        if (cursor != null) {
            cursor.close();
        }

        return null;
    }

    /**
     * Get a {@link AccountDao} object from the local database.
     *
     * @param context Interface to global information about an application environment.
     * @param email   An email of the some account information.
     * @return The {@link AccountDao};
     */
    public AccountDao getAccountInformation(Context context, String email) {
        if (email != null) {
            email = email.toLowerCase();
        }

        Cursor cursor = context.getContentResolver().query(getBaseContentUri(), null, AccountDaoSource.COL_EMAIL + " " +
                "= ?", new String[]{email}, null);

        if (cursor != null && cursor.moveToFirst()) {
            return getCurrentAccountDao(context, cursor);
        }

        if (cursor != null) {
            cursor.close();
        }

        return null;
    }

    /**
     * Update information about some {@link AccountDao}.
     *
     * @param context             Interface to global information about an application environment.
     * @param googleSignInAccount Reflecting the user's sign in information.
     * @return The count of updated rows. Will be 1 if information about {@link AccountDao} was
     * updated or -1 otherwise.
     */
    public int updateAccountInformation(Context context, GoogleSignInAccount googleSignInAccount) {
        if (googleSignInAccount != null) {
            Account account = googleSignInAccount.getAccount();

            if (account == null) {
                return -1;
            }

            String email = account.name;
            if (email == null) {
                return -1;
            } else {
                email = email.toLowerCase();
            }

            String type = account.type;
            if (type == null) {
                return -1;
            } else {
                type = type.toLowerCase();
            }

            ContentResolver contentResolver = context.getContentResolver();
            if (contentResolver != null) {
                ContentValues contentValues = generateContentValues(googleSignInAccount);
                return contentResolver.update(getBaseContentUri(),
                        contentValues,
                        COL_EMAIL + " = ? AND " + COL_ACCOUNT_TYPE + " = ?",
                        new String[]{email, type});
            } else return -1;
        } else return -1;
    }

    /**
     * Delete information about some {@link AccountDao}.
     *
     * @param context    Interface to global information about an application environment.
     * @param accountDao The object which contains information about an email account.
     * @return The count of deleted rows. Will be 1 if information about {@link AccountDao} was
     * deleted or -1 otherwise.
     */
    public int deleteAccountInformation(Context context, AccountDao accountDao) {
        if (accountDao != null) {

            String email = accountDao.getEmail();
            if (email == null) {
                return -1;
            } else {
                email = email.toLowerCase();
            }

            String type = accountDao.getAccountType();
            if (type == null) {
                return -1;
            } else {
                type = type.toLowerCase();
            }

            ContentResolver contentResolver = context.getContentResolver();
            if (contentResolver != null) {
                return contentResolver.delete(getBaseContentUri(), COL_EMAIL + " = ? AND " + COL_ACCOUNT_TYPE + " = ?",
                        new String[]{email, type});
            } else return -1;
        } else return -1;
    }

    /**
     * Get the list of all added account without the active account.
     *
     * @param context Interface to global information about an application environment.
     * @param email   An email of the active account.
     * @return The list of all added account without the active account
     */
    public List<AccountDao> getAccountsWithoutActive(Context context, String email) {
        if (email != null) {
            email = email.toLowerCase();
        }

        Cursor cursor = context.getContentResolver().query(getBaseContentUri(), null,
                AccountDaoSource.COL_EMAIL + " != ?", new String[]{email}, null);

        List<AccountDao> accountDaoList = new ArrayList<>();
        if (cursor != null) {
            while (cursor.moveToNext()) {
                accountDaoList.add(getCurrentAccountDao(context, cursor));
            }
        }

        if (cursor != null) {
            cursor.close();
        }

        return accountDaoList;
    }

    /**
     * Mark some account as active.
     *
     * @param context Interface to global information about an application environment.
     * @param email   The account which will be set as active.
     * @return The count of updated rows.
     */
    public int setActiveAccount(Context context, String email) {
        if (email == null) {
            return -1;
        } else {
            email = email.toLowerCase();
        }

        ContentResolver contentResolver = context.getContentResolver();
        if (contentResolver != null) {
            ContentValues contentValuesDeactivateAllAccount = new ContentValues();
            contentValuesDeactivateAllAccount.put(COL_IS_ACTIVE, 0);
            int updateRowCount = contentResolver.update(getBaseContentUri(), contentValuesDeactivateAllAccount,
                    null, null);

            ContentValues contentValuesActivateAccount = new ContentValues();
            contentValuesActivateAccount.put(COL_IS_ACTIVE, 1);
            updateRowCount += contentResolver.update(getBaseContentUri(), contentValuesActivateAccount,
                    COL_EMAIL + " = ? ", new String[]{email});

            return updateRowCount;

        } else return -1;
    }

    /**
     * Generate a {@link ContentValues} using {@link GoogleSignInAccount}.
     *
     * @param googleSignInAccount The {@link GoogleSignInAccount} object;
     * @return The generated {@link ContentValues}.
     */
    @Nullable
    private ContentValues generateContentValues(GoogleSignInAccount googleSignInAccount) {
        ContentValues contentValues = new ContentValues();
        if (googleSignInAccount.getEmail() != null) {
            contentValues.put(COL_EMAIL, googleSignInAccount.getEmail().toLowerCase());
        } else return null;

        Account account = googleSignInAccount.getAccount();

        if (account != null && account.type != null) {
            contentValues.put(COL_ACCOUNT_TYPE, account.type.toLowerCase());
        }

        contentValues.put(COL_DISPLAY_NAME, googleSignInAccount.getDisplayName());
        contentValues.put(COL_USERNAME, googleSignInAccount.getEmail());
        contentValues.put(COL_PASSWORD, "");
        contentValues.put(COL_IMAP_SERVER, GmailConstants.GMAIL_IMAP_SERVER);
        contentValues.put(COL_IMAP_PORT, GmailConstants.GMAIL_IMAP_PORT);
        contentValues.put(COL_SMTP_SERVER, GmailConstants.GMAIL_SMTP_SERVER);
        contentValues.put(COL_SMTP_PORT, GmailConstants.GMAIL_SMTP_PORT);
        contentValues.put(COL_IMAP_AUTH_MECHANISMS, JavaEmailConstants.AUTH_MECHANISMS_XOAUTH2);
        contentValues.put(COL_SMTP_AUTH_MECHANISMS, JavaEmailConstants.AUTH_MECHANISMS_XOAUTH2);
        contentValues.put(COL_IMAP_IS_USE_SSL_TLS, 1);
        contentValues.put(COL_SMTP_IS_USE_SSL_TLS, 1);
        contentValues.put(COL_GIVEN_NAME, googleSignInAccount.getGivenName());
        contentValues.put(COL_FAMILY_NAME, googleSignInAccount.getFamilyName());
        contentValues.put(COL_IS_ACTIVE, true);
        if (googleSignInAccount.getPhotoUrl() != null) {
            contentValues.put(COL_PHOTO_URL, googleSignInAccount.getPhotoUrl().toString());
        }
        return contentValues;
    }

    /**
     * Generate a {@link ContentValues} using {@link AuthCredentials}.
     *
     * @param context         Interface to global information about an application environment;
     * @param authCredentials The {@link AuthCredentials} object;
     * @return The generated {@link ContentValues}.
     */
    private ContentValues generateContentValuesWithEncryptedPassword(Context context,
                                                                     AuthCredentials authCredentials) throws Exception {
        ContentValues contentValues = new ContentValues();
        String email = authCredentials.getEmail();
        if (!TextUtils.isEmpty(email)) {
            contentValues.put(COL_EMAIL, email.toLowerCase());
        } else return null;

        KeyStoreCryptoManager keyStoreCryptoManager = new KeyStoreCryptoManager(context);

        contentValues.put(COL_ACCOUNT_TYPE, email.substring(email.indexOf('@') + 1, email.length()));
        contentValues.put(COL_USERNAME, authCredentials.getUsername());
        contentValues.put(COL_PASSWORD, keyStoreCryptoManager.encryptWithRSA(authCredentials.getPassword()));
        contentValues.put(COL_IMAP_SERVER, authCredentials.getImapServer());
        contentValues.put(COL_IMAP_PORT, authCredentials.getImapPort());
        contentValues.put(COL_IMAP_IS_USE_SSL_TLS,
                authCredentials.getImapSecurityTypeOption() == SecurityType.Option.SSL_TLS);
        contentValues.put(COL_IMAP_IS_USE_STARTTLS,
                authCredentials.getImapSecurityTypeOption() == SecurityType.Option.STARTLS);
        contentValues.put(COL_SMTP_SERVER, authCredentials.getSmtpServer());
        contentValues.put(COL_SMTP_PORT, authCredentials.getSmtpPort());
        contentValues.put(COL_SMTP_IS_USE_SSL_TLS,
                authCredentials.getSmtpSecurityTypeOption() == SecurityType.Option.SSL_TLS);
        contentValues.put(COL_SMTP_IS_USE_STARTTLS,
                authCredentials.getSmtpSecurityTypeOption() == SecurityType.Option.STARTLS);
        contentValues.put(COL_SMTP_IS_USE_CUSTOM_SIGN, authCredentials.isUseCustomSignInForSmtp());
        contentValues.put(COL_SMTP_USERNAME, authCredentials.getSmtpSigInUsername());
        contentValues.put(COL_SMTP_PASSWORD, keyStoreCryptoManager.encryptWithRSA(authCredentials
                .getSmtpSignInPassword()));

        contentValues.put(COL_IS_ACTIVE, true);

        return contentValues;
    }

}
