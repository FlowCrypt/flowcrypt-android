/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.gmail;

import android.accounts.Account;
import android.content.Context;

import com.flowcrypt.email.R;
import com.flowcrypt.email.database.dao.source.AccountDao;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;

import java.util.Arrays;

/**
 * This class helps to work with Gmail API.
 *
 * @author Denis Bondarenko
 * Date: 30.10.2017
 * Time: 14:35
 * E-mail: DenBond7@gmail.com
 */

public class GmailApiHelper {
  public static final String DEFAULT_USER_ID = "me";
  public static final String MESSAGE_RESPONSE_FORMAT_RAW = "raw";
  private static final String[] SCOPES = {GmailScopes.MAIL_GOOGLE_COM};

  /**
   * Generate {@link Gmail} using incoming {@link AccountDao}. The {@link} Gmail is the main point in using Gmail API.
   *
   * @param context    Interface to global information about an application environment.
   * @param account The {@link AccountDao} object which contains information about an email account.
   * @return Generated {@link Gmail}.
   */
  public static Gmail generateGmailApiService(Context context, AccountDao account) {
    if (account == null || account.getAccount() == null) {
      throw new IllegalArgumentException("AccountDao is not valid.");
    }

    GoogleAccountCredential credential = generateGoogleAccountCredential(context, account.getAccount());

    HttpTransport transport = new NetHttpTransport();
    JsonFactory factory = JacksonFactory.getDefaultInstance();
    String appName = context.getString(R.string.app_name);
    return new Gmail.Builder(transport, factory, credential).setApplicationName(appName).build();
  }

  /**
   * Generate {@link GoogleAccountCredential} which will be used with Gmail API.
   *
   * @param context Interface to global information about an application environment.
   * @param account The Gmail account.
   * @return Generated {@link GoogleAccountCredential}.
   */
  private static GoogleAccountCredential generateGoogleAccountCredential(Context context, Account account) {
    return GoogleAccountCredential.usingOAuth2(context, Arrays.asList(SCOPES)).setSelectedAccount(account);
  }
}
