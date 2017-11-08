/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (human@flowcrypt.com).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.loader;

import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;
import android.text.TextUtils;

import com.flowcrypt.email.api.email.EmailUtil;
import com.flowcrypt.email.api.email.JavaEmailConstants;
import com.flowcrypt.email.api.email.SearchBackupsUtil;
import com.flowcrypt.email.api.email.protocol.OpenStoreHelper;
import com.flowcrypt.email.database.dao.source.AccountDao;
import com.flowcrypt.email.model.KeyDetails;
import com.flowcrypt.email.model.results.LoaderResult;
import com.sun.mail.imap.IMAPFolder;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import javax.mail.BodyPart;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.internet.MimeBodyPart;

/**
 * This loader finds and returns a user backup of private keys from the mail.
 *
 * @author DenBond7
 *         Date: 30.04.2017.
 *         Time: 22:28.
 *         E-mail: DenBond7@gmail.com
 */
public class LoadPrivateKeysFromMailAsyncTaskLoader extends AsyncTaskLoader<LoaderResult> {

    /**
     * An user account.
     */
    private AccountDao accountDao;

    public LoadPrivateKeysFromMailAsyncTaskLoader(Context context, AccountDao accountDao) {
        super(context);
        this.accountDao = accountDao;
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
        ArrayList<KeyDetails> privateKeyDetailsList = new ArrayList<>();
        try {
            Session session = OpenStoreHelper.getSessionForAccountDao(accountDao);
            Store store = OpenStoreHelper.openAndConnectToStore(getContext(), accountDao, session);

            Folder[] folders = store.getDefaultFolder().list("*");

            for (Folder folder : folders) {
                if (!EmailUtil.isFolderHasNoSelectAttribute((IMAPFolder) folder)) {
                    folder.open(Folder.READ_ONLY);

                    Message[] foundMessages = folder.search(
                            SearchBackupsUtil.generateSearchTerms(accountDao.getEmail()));

                    for (Message message : foundMessages) {
                        String key = getKeyFromMessageIfItExists(message);
                        if (!TextUtils.isEmpty(key) && privateKeyNotExistsInList(privateKeyDetailsList, key)) {
                            privateKeyDetailsList.add(new KeyDetails(key,
                                    KeyDetails.Type.EMAIL));
                        }
                    }

                    folder.close(false);
                }
            }

            store.close();
            return new LoaderResult(privateKeyDetailsList, null);
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
     * Check is the private key exists in the keys list.
     *
     * @param keyDetailsList The list of {@link KeyDetails} objects.
     * @param key            The private key armored string.
     * @return true if the key not exists in the list, otherwise false.
     */
    private boolean privateKeyNotExistsInList(ArrayList<KeyDetails> keyDetailsList,
                                              String key) {
        for (KeyDetails keyDetails : keyDetailsList) {
            if (key.equals(keyDetails.getValue())) {
                return false;
            }
        }
        return true;
    }

    /**
     * Get a private key from {@link Message}, if it exists in.
     *
     * @param message The original {@link Message} object.
     * @return <tt>String</tt> A private key.
     * @throws MessagingException
     * @throws IOException
     */
    private String getKeyFromMessageIfItExists(Message message) throws MessagingException,
            IOException {
        if (message.isMimeType(JavaEmailConstants.MIME_TYPE_MULTIPART)) {
            Multipart multiPart = (Multipart) message.getContent();
            int numberOfParts = multiPart.getCount();
            for (int partCount = 0; partCount < numberOfParts; partCount++) {
                BodyPart bodyPart = multiPart.getBodyPart(partCount);
                if (bodyPart instanceof MimeBodyPart) {
                    MimeBodyPart mimeBodyPart = (MimeBodyPart) bodyPart;
                    if (Part.ATTACHMENT.equalsIgnoreCase(mimeBodyPart.getDisposition())) {
                        return IOUtils.toString(mimeBodyPart.getInputStream(),
                                StandardCharsets.UTF_8);
                    }
                }
            }
        }

        return null;
    }
}
