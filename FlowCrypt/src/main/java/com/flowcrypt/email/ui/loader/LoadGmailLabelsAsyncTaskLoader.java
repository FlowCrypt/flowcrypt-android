package com.flowcrypt.email.ui.loader;

import android.accounts.Account;
import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;

import com.flowcrypt.email.api.email.JavaEmailConstants;
import com.flowcrypt.email.api.email.gmail.GmailConstants;
import com.flowcrypt.email.api.email.protocol.OpenStoreHelper;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.sun.mail.gimap.GmailSSLStore;

import java.util.ArrayList;
import java.util.List;

import javax.mail.Folder;

/**
 * This loader load and return available Gmail labels.
 *
 * @author DenBond7
 *         Date: 10.05.2017
 *         Time: 16:52
 *         E-mail: DenBond7@gmail.com
 */

public class LoadGmailLabelsAsyncTaskLoader extends AsyncTaskLoader<List<String>> {

    private Account account;

    public LoadGmailLabelsAsyncTaskLoader(Context context, Account account) {
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
    public List<String> loadInBackground() {
        try {
            List<String> labels = new ArrayList<>();

            String token = GoogleAuthUtil.getToken(getContext(), account,
                    JavaEmailConstants.OAUTH2 + GmailConstants.SCOPE_MAIL_GOOGLE_COM);
            GmailSSLStore gmailSSLStore = OpenStoreHelper.openAndConnectToGimapsStore(token,
                    account.name);

            Folder[] folders = gmailSSLStore.getDefaultFolder().list("*");

            for (Folder folder : folders) {
                labels.add(folder.getFullName());
            }
            gmailSSLStore.close();
            return labels;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void onStopLoading() {
        cancelLoad();
    }
}
