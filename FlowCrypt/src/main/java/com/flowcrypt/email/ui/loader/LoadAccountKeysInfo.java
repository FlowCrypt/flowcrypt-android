/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.loader;

import android.content.Context;

import com.flowcrypt.email.api.email.gmail.GmailApiHelper;
import com.flowcrypt.email.api.retrofit.ApiHelper;
import com.flowcrypt.email.api.retrofit.ApiService;
import com.flowcrypt.email.api.retrofit.request.model.PostLookUpEmailsModel;
import com.flowcrypt.email.api.retrofit.response.attester.LookUpEmailResponse;
import com.flowcrypt.email.api.retrofit.response.attester.LookUpEmailsResponse;
import com.flowcrypt.email.database.dao.source.AccountDao;
import com.flowcrypt.email.js.PgpContact;
import com.flowcrypt.email.model.results.LoaderResult;
import com.flowcrypt.email.util.exception.ExceptionUtil;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.ListSendAsResponse;
import com.google.api.services.gmail.model.SendAs;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import androidx.loader.content.AsyncTaskLoader;
import retrofit2.Response;

/**
 * This loader does job of receiving information about an array of public
 * keys from "https://attester.flowcrypt.com/lookup/email".
 *
 * @author Denis Bondarenko
 * Date: 13.11.2017
 * Time: 15:13
 * E-mail: DenBond7@gmail.com
 */

public class LoadAccountKeysInfo extends AsyncTaskLoader<LoaderResult> {
  /**
   * An user account.
   */
  private AccountDao account;

  public LoadAccountKeysInfo(Context context, AccountDao account) {
    super(context);
    this.account = account;
    onContentChanged();
  }

  @Override
  public void onStartLoading() {
    if (takeContentChanged()) {
      forceLoad();
    }
  }

  @Override
  public LoaderResult loadInBackground() {
    if (account != null) {
      List<String> emails = new ArrayList<>();
      try {
        switch (account.getAccountType()) {
          case AccountDao.ACCOUNT_TYPE_GOOGLE:
            emails.addAll(getAvailableGmailAliases(account));
            break;

          default:
            emails.add(account.getEmail());
            break;
        }

        return new LoaderResult(getLookUpEmailsResponse(emails), null);
      } catch (IOException e) {
        e.printStackTrace();
        ExceptionUtil.handleError(e);
        return new LoaderResult(null, e);
      }
    } else {
      return new LoaderResult(null, new NullPointerException("AccountDao is null!"));
    }
  }

  @Override
  public void onStopLoading() {
    cancelLoad();
  }

  /**
   * Get available Gmail aliases for an input {@link AccountDao}.
   *
   * @param account The {@link AccountDao} object which contains information about an email account.
   * @return The list of available Gmail aliases.
   */
  private Collection<? extends String> getAvailableGmailAliases(AccountDao account) {
    List<String> emails = new ArrayList<>();
    emails.add(account.getEmail());

    try {
      Gmail gmail = GmailApiHelper.generateGmailApiService(getContext(), account);
      ListSendAsResponse aliases = gmail.users().settings().sendAs().list(GmailApiHelper.DEFAULT_USER_ID).execute();
      for (SendAs alias : aliases.getSendAs()) {
        if (alias.getVerificationStatus() != null) {
          emails.add(alias.getSendAsEmail());
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
      ExceptionUtil.handleError(e);
    }

    return emails;
  }

  /**
   * Get {@link LookUpEmailsResponse} object which contain a remote information about
   * {@link PgpContact}.
   *
   * @param emails Used to generate a request to the server.
   * @return {@link LookUpEmailsResponse}
   * @throws IOException
   */
  private List<LookUpEmailResponse> getLookUpEmailsResponse(List<String> emails) throws IOException {
    ApiService apiService = ApiHelper.getInstance(getContext()).getRetrofit().create(ApiService.class);
    Response<LookUpEmailsResponse> response = apiService.postLookUpEmails(new PostLookUpEmailsModel(emails)).execute();
    LookUpEmailsResponse lookUpEmailsResponse = response.body();

    if (lookUpEmailsResponse != null) {
      return lookUpEmailsResponse.getResults();
    }
    return new ArrayList<>();
  }
}
