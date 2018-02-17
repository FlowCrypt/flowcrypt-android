/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.gmail;

import android.accounts.Account;
import android.content.Context;

import com.flowcrypt.email.R;
import com.flowcrypt.email.api.email.JavaEmailConstants;
import com.flowcrypt.email.database.dao.source.AccountDao;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.gmail.Gmail;

/**
 * This class helps to work with Gmail API.
 *
 * @author Denis Bondarenko
 *         Date: 30.10.2017
 *         Time: 14:35
 *         E-mail: DenBond7@gmail.com
 */

public class GmailApiHelper {
    public static final String DEFAULT_USER_ID = "me";
    public static final String MESSAGE_RESPONSE_FORMAT_RAW = "raw";

    /**
     * Generate {@link Gmail} using incoming {@link AccountDao}. The {@link} Gmail is the main point in using Gmail API.
     *
     * @param context    Interface to global information about an application environment.
     * @param accountDao The {@link AccountDao} object which contains information about an email account.
     * @return Generated {@link Gmail}.
     */
    public static Gmail generateGmailApiService(Context context, AccountDao accountDao) {
        if (accountDao == null || accountDao.getAccount() == null) {
            throw new IllegalArgumentException("AccountDao is not valid.");
        }

        GoogleAccountCredential googleAccountCredential = generateGoogleAccountCredential(context,
                accountDao.getAccount());

        HttpTransport httpTransport = AndroidHttp.newCompatibleTransport();
        JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
        return new Gmail.Builder(httpTransport, jsonFactory, googleAccountCredential)
                .setApplicationName(context.getString(R.string.app_name)).build();
    }

    /**
     * Generate {@link GoogleAccountCredential} which will be used with Gmail API.
     *
     * @param context Interface to global information about an application environment.
     * @param account The Gmail account.
     * @return Generated {@link GoogleAccountCredential}.
     */
    private static GoogleAccountCredential generateGoogleAccountCredential(Context context, Account account) {
        GoogleAccountCredential googleAccountCredential = new GoogleAccountCredential(context,
                JavaEmailConstants.OAUTH2 + GmailConstants.SCOPE_MAIL_GOOGLE_COM);
        googleAccountCredential.setSelectedAccount(account);

        return googleAccountCredential;
    }
}
