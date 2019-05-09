/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.loader;

import android.content.Context;
import android.text.TextUtils;

import com.flowcrypt.email.api.retrofit.ApiHelper;
import com.flowcrypt.email.api.retrofit.ApiService;
import com.flowcrypt.email.api.retrofit.node.NodeCallsExecutor;
import com.flowcrypt.email.api.retrofit.request.model.PostLookUpEmailModel;
import com.flowcrypt.email.api.retrofit.response.attester.LookUpEmailResponse;
import com.flowcrypt.email.api.retrofit.response.model.node.NodeKeyDetails;
import com.flowcrypt.email.database.dao.source.ContactsDaoSource;
import com.flowcrypt.email.model.PgpContact;
import com.flowcrypt.email.model.UpdateInfoAboutPgpContactsResult;
import com.flowcrypt.email.model.results.LoaderResult;
import com.flowcrypt.email.util.GeneralUtil;
import com.flowcrypt.email.util.exception.ExceptionUtil;
import com.flowcrypt.email.util.exception.NodeException;
import com.google.android.gms.common.util.CollectionUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.Nullable;
import androidx.loader.content.AsyncTaskLoader;
import retrofit2.Response;

/**
 * This loader do next job for all input emails:
 * <p>
 * <ul>
 * <li>a) look up the email in the database;
 * <li>b) if there is a record for that email and has_pgp==true, we can use the `pubkey` instead of
 * querying Attester;
 * <li>c) if there is a record but `has_pgp==false`, do `flowcrypt.com/attester/lookup/email` API call
 * to see if you can now get the pubkey. If a pubkey is available, save it back to the database.
 * <li>e) no record in the db found:<ol>
 * <li>save an empty record eg `new PgpContact(email, null);` - this means we don't know if they have PGP yet
 * <li>look up the email on `flowcrypt.com/attester/lookup/email`
 * <li>if pubkey comes back, create something like `new PgpContact(js, email, null, pubkey,
 * client);`. The PgpContact constructor will define has_pgp, longid, fingerprint, etc
 * for you. Then save that object into database.
 * <li>if no pubkey found, create `new PgpContact(js, email, null, null, null, null);` - this
 * means we know they don't currently have PGP
 * <p>
 *
 * @author DenBond7
 * Date: 19.05.2017
 * Time: 10:50
 * E-mail: DenBond7@gmail.com
 */

public class UpdateInfoAboutPgpContactsAsyncTaskLoader extends AsyncTaskLoader<LoaderResult> {
  private List<String> emails;

  public UpdateInfoAboutPgpContactsAsyncTaskLoader(Context context, List<String> emails) {
    super(context);
    this.emails = emails;
    onContentChanged();
  }

  @Override
  public LoaderResult loadInBackground() {
    ContactsDaoSource contactsDaoSource = new ContactsDaoSource();
    return getLoaderResult(contactsDaoSource);
  }

  @Override
  public void onStopLoading() {
    cancelLoad();
  }

  @Override
  public void onStartLoading() {
    if (takeContentChanged()) {
      forceLoad();
    }
  }

  private LoaderResult getLoaderResult(ContactsDaoSource contactsDaoSource) {
    boolean isAllInfoReceived = true;
    List<PgpContact> pgpContacts = new ArrayList<>();
    try {
      for (String email : emails) {
        if (GeneralUtil.isEmailValid(email)) {
          if (email != null) {
            email = email.toLowerCase();
          }

          PgpContact localPgpContact = contactsDaoSource.getPgpContact(getContext(), email);

          if (localPgpContact == null) {
            localPgpContact = new PgpContact(email, null);
            contactsDaoSource.addRow(getContext(), localPgpContact);
          }

          try {
            if (!localPgpContact.getHasPgp()) {
              PgpContact remotePgpContact = getPgpContactInfoFromServer(email);
              if (remotePgpContact != null) {
                contactsDaoSource.updatePgpContact(getContext(), localPgpContact, remotePgpContact);
                localPgpContact = contactsDaoSource.getPgpContact(getContext(), email);
              }
            }
          } catch (Exception e) {
            isAllInfoReceived = false;
            e.printStackTrace();
            ExceptionUtil.handleError(e);
          }

          pgpContacts.add(localPgpContact);
        }
      }
      return new LoaderResult(new UpdateInfoAboutPgpContactsResult(emails, isAllInfoReceived, pgpContacts), null);
    } catch (Exception e) {
      e.printStackTrace();
      ExceptionUtil.handleError(e);
      return new LoaderResult(null, e);
    }
  }

  /**
   * Get information about {@link PgpContact} from the remote server.
   *
   * @param email Used to generate a request to the server.
   * @return {@link PgpContact}
   * @throws IOException
   */
  @Nullable
  private PgpContact getPgpContactInfoFromServer(String email) throws IOException, NodeException {
    LookUpEmailResponse response = getLookUpEmailResponse(email);

    if (response != null) {
      if (!TextUtils.isEmpty(response.getPubKey())) {
        String client = response.hasCryptup() ? ContactsDaoSource.CLIENT_FLOWCRYPT : ContactsDaoSource.CLIENT_PGP;
        List<NodeKeyDetails> details = NodeCallsExecutor.parseKeys(response.getPubKey());
        if (!CollectionUtils.isEmpty(details)) {
          PgpContact pgpContact = details.get(0).getPrimaryPgpContact();
          pgpContact.setClient(client);
          return pgpContact;
        }
      }
    }

    return null;
  }

  /**
   * Get {@link LookUpEmailResponse} object which contain a remote information about
   * {@link PgpContact}.
   *
   * @param email Used to generate a request to the server.
   * @return {@link LookUpEmailResponse}
   * @throws IOException
   */
  private LookUpEmailResponse getLookUpEmailResponse(String email) throws IOException {
    ApiService apiService = ApiHelper.getInstance(getContext()).getRetrofit().create(ApiService.class);
    Response<LookUpEmailResponse> response = apiService.postLookUpEmail(new PostLookUpEmailModel(email)).execute();
    return response.body();
  }
}
