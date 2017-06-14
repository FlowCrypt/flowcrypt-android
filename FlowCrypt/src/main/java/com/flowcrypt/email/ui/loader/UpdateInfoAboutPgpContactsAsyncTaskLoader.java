/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (tom@cryptup.org). Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.loader;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v4.content.AsyncTaskLoader;
import android.text.TextUtils;

import com.flowcrypt.email.api.retrofit.ApiHelper;
import com.flowcrypt.email.api.retrofit.ApiService;
import com.flowcrypt.email.api.retrofit.request.model.PostLookUpEmailModel;
import com.flowcrypt.email.api.retrofit.response.LookUpEmailResponse;
import com.flowcrypt.email.database.dao.source.ContactsDaoSource;
import com.flowcrypt.email.model.results.LoaderResult;
import com.flowcrypt.email.test.Js;
import com.flowcrypt.email.test.PgpContact;

import java.io.IOException;
import java.util.List;

import retrofit2.Response;

/**
 * This loader do next job for all input emails:
 * <p>
 * <ul>
 * <li>a) look up the email in the database;
 * <li>b) if there is a record for that email and has_pgp==true, we can use the `pubkey` instead of
 * querying Attester;
 * <li>c) if there is a record but `has_pgp==false`, do `attester.cryptup.io/lookup/email` API call
 * to see if you can now get the pubkey. If a pubkey is available, save it back to the database.
 * <li>e) no record in the db found:<ol>
 * <li>save an empty record eg `new PgpContact(email, null);` - this means we don't know if
 * they
 * have PGP yet
 * <li>look up the email on `attester.cryptup.io/lookup/email`
 * <li>if pubkey comes back, create something like `new PgpContact(js, email, null, pubkey,
 * client, attested);`. The PgpContact constructor will define has_pgp, longid, fingerprint, etc
 * for you. Then save that object into database.
 * <li>if no pubkey found, create `new PgpContact(js, email, null, null, null, null);` - this
 * means we know they don't currently have PGP
 * <p>
 *
 * @author DenBond7
 *         Date: 19.05.2017
 *         Time: 10:50
 *         E-mail: DenBond7@gmail.com
 */

public class UpdateInfoAboutPgpContactsAsyncTaskLoader extends
        AsyncTaskLoader<LoaderResult> {
    private List<String> emails;

    public UpdateInfoAboutPgpContactsAsyncTaskLoader(Context context, List<String> emails) {
        super(context);
        this.emails = emails;
        onContentChanged();
    }

    @Override
    public LoaderResult loadInBackground() {
        ContactsDaoSource contactsDaoSource = new ContactsDaoSource();
        try {
            Js js = new Js(getContext(), null);
            for (String email : emails) {
                PgpContact localPgpContact = contactsDaoSource.getPgpContact(getContext(), email);
                if (localPgpContact != null) {
                    if (!localPgpContact.getHasPgp()) {
                        PgpContact remotePgpContact = getPgpContactInfoFromServer(email, js);
                        if (remotePgpContact != null) {
                            contactsDaoSource.updatePgpContact(getContext(), remotePgpContact);
                        }
                    }
                } else {
                    contactsDaoSource.addRow(getContext(), new PgpContact(email, null));
                    PgpContact remotePgpContact = getPgpContactInfoFromServer(email, js);
                    if (remotePgpContact != null) {
                        contactsDaoSource.updatePgpContact(getContext(), remotePgpContact);
                    }
                }
            }

            return new LoaderResult(true, null);
        } catch (Exception e) {
            e.printStackTrace();
            return new LoaderResult(null, e);
        }
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

    /**
     * Get an information about {@link PgpContact} from the remote server.
     *
     * @param email Used to generate a request to the server.
     * @param js    Used to create a {@link PgpContact} object from the information which
     *              received from the server.
     * @return {@link PgpContact}
     * @throws IOException
     */
    @Nullable
    private PgpContact getPgpContactInfoFromServer(String email, Js js) throws
            IOException {
        LookUpEmailResponse lookUpEmailResponse = getLookUpEmailResponse(email);

        if (lookUpEmailResponse != null) {
            if (!TextUtils.isEmpty(lookUpEmailResponse.getPubkey())) {
                String client = lookUpEmailResponse.getPubkey() == null ? null : lookUpEmailResponse
                        .isHasCryptup() ? ContactsDaoSource.CLIENT_FLOWCRYPT : ContactsDaoSource
                        .CLIENT_PGP;

                return new PgpContact(js, email, null,
                        lookUpEmailResponse.getPubkey(), client, lookUpEmailResponse.isAttested());
            } else {
                return new PgpContact(js, email, null, null, null, false);
            }
        } else return null;
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
        ApiService apiService = ApiHelper.getInstance().getRetrofit().create(ApiService.class);
        Response<LookUpEmailResponse> response = apiService.postLookUpEmail(
                new PostLookUpEmailModel(email)).execute();
        return response.body();
    }
}
