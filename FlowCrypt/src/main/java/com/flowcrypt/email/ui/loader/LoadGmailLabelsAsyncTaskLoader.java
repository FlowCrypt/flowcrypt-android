/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (tom@cryptup.org). Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.loader;

import android.accounts.Account;
import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;

import com.flowcrypt.email.api.email.FoldersManager;
import com.flowcrypt.email.api.email.JavaEmailConstants;
import com.flowcrypt.email.api.email.gmail.GmailConstants;
import com.flowcrypt.email.api.email.protocol.OpenStoreHelper;
import com.flowcrypt.email.model.results.LoaderResult;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.sun.mail.gimap.GmailSSLStore;
import com.sun.mail.imap.IMAPFolder;

import java.util.Arrays;
import java.util.List;

import javax.mail.Folder;
import javax.mail.MessagingException;

/**
 * This loader load and return available Gmail labels.
 *
 * @author DenBond7
 *         Date: 10.05.2017
 *         Time: 16:52
 *         E-mail: DenBond7@gmail.com
 */

public class LoadGmailLabelsAsyncTaskLoader extends AsyncTaskLoader<LoaderResult> {

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
    public LoaderResult loadInBackground() {
        try {
            FoldersManager foldersManager = new FoldersManager();
            String token = GoogleAuthUtil.getToken(getContext(), account,
                    JavaEmailConstants.OAUTH2 + GmailConstants.SCOPE_MAIL_GOOGLE_COM);
            GmailSSLStore gmailSSLStore = OpenStoreHelper.openAndConnectToGimapsStore(token,
                    account.name);

            Folder[] folders = gmailSSLStore.getDefaultFolder().list("*");

            for (Folder folder : folders) {
                IMAPFolder imapFolder = (IMAPFolder) folder;
                if (!isFolderHasNoSelectAttribute(imapFolder)) {
                    foldersManager.addFolder(imapFolder, folder.getName());
                }
            }
            gmailSSLStore.close();
            return new LoaderResult(foldersManager, null);
        } catch (Exception e) {
            e.printStackTrace();
            return new LoaderResult(null, e);
        }
    }

    @Override
    public void onStopLoading() {
        cancelLoad();
    }

    /**
     * Check if current folder has {@link JavaEmailConstants#FOLDER_ATTRIBUTE_NO_SELECT}. If the
     * folder contains it attribute we will not show this folder in the list.
     *
     * @param imapFolder The {@link IMAPFolder} object.
     * @return true if current folder contains attribute
     * {@link JavaEmailConstants#FOLDER_ATTRIBUTE_NO_SELECT}, false otherwise.
     * @throws MessagingException
     */
    private boolean isFolderHasNoSelectAttribute(IMAPFolder imapFolder) throws MessagingException {
        List<String> attributes = Arrays.asList(imapFolder.getAttributes());
        return attributes.contains(JavaEmailConstants.FOLDER_ATTRIBUTE_NO_SELECT);
    }
}
