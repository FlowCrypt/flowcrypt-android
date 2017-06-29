/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (tom@cryptup.org).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.loader;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.content.AsyncTaskLoader;
import android.text.TextUtils;

import com.flowcrypt.email.api.email.model.OutgoingMessageInfo;
import com.flowcrypt.email.database.dao.source.ContactsDaoSource;
import com.flowcrypt.email.model.results.LoaderResult;
import com.flowcrypt.email.security.SecurityStorageConnector;
import com.flowcrypt.email.test.Js;
import com.flowcrypt.email.test.PgpContact;
import com.flowcrypt.email.test.PgpKey;
import com.flowcrypt.email.test.PgpKeyInfo;

import java.util.ArrayList;

/**
 * This loader do a job of prepare encrypted raw message. When we preparing a message, we do next
 * steps:
 * <ul>
 * <li>1) get public keys for recipients from the server + keys of the sender(generated locally);
 * </li>
 * <li>2) doing encrypt the text with public keys;</li>
 * </ul>
 *
 * @author DenBond7
 *         Date: 08.05.2017
 *         Time: 15:36
 *         E-mail: DenBond7@gmail.com
 */

public class PrepareEncryptedRawMessageAsyncTaskLoader extends AsyncTaskLoader<LoaderResult> {
    private OutgoingMessageInfo outgoingMessageInfo;

    public PrepareEncryptedRawMessageAsyncTaskLoader(Context context,
                                                     @NonNull OutgoingMessageInfo
                                                             outgoingMessageInfo) {
        super(context);
        this.outgoingMessageInfo = outgoingMessageInfo;
        onContentChanged();
    }

    @Override
    public LoaderResult loadInBackground() {
        ContactsDaoSource contactsDaoSource = new ContactsDaoSource();

        for (PgpContact pgpContact : outgoingMessageInfo.getToPgpContacts()) {
            contactsDaoSource.updateLastUseOfPgpContact(getContext(), pgpContact);
        }

        try {
            Js js = new Js(getContext(), new SecurityStorageConnector(getContext()));
            String[] pubKeys = getPubKeys(js);

            if (pubKeys.length > 0) {
                String encryptedText = js.crypto_message_encrypt(pubKeys,
                        outgoingMessageInfo.getMessage(), true);

                String encryptedMessage = js.mime_encode(encryptedText,
                        outgoingMessageInfo.getToPgpContacts(),
                        outgoingMessageInfo.getFromPgpContact(),
                        outgoingMessageInfo.getSubject(),
                        null,
                        js.mime_decode(outgoingMessageInfo.getRawReplyMessage()));

                return new LoaderResult(encryptedMessage, null);
            } else return new LoaderResult(null, null);
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
     * Get public keys for recipients + keys of the sender;
     *
     * @param js - {@link Js} util class.
     * @return <tt>String[]</tt> An array of public keys.
     */
    private String[] getPubKeys(Js js) {
        ArrayList<String> publicKeys = new ArrayList<>();
        for (PgpContact pgpContact : outgoingMessageInfo.getToPgpContacts()) {
            if (!TextUtils.isEmpty(pgpContact.getPubkey())) {
                publicKeys.add(pgpContact.getPubkey());
            }
        }

        publicKeys.addAll(generateOwnPublicKeys(js));

        return publicKeys.toArray(new String[0]);
    }

    /**
     * Get public keys of the sender;
     *
     * @param js - {@link Js} util class.
     * @return <tt>String[]</tt> An array of the sender public keys.
     */
    private ArrayList<String> generateOwnPublicKeys(Js js) {
        ArrayList<String> publicKeys = new ArrayList<>();

        SecurityStorageConnector securityStorageConnector = new SecurityStorageConnector
                (getContext());
        PgpKeyInfo[] pgpKeyInfoArray = securityStorageConnector.getAllPgpPrivateKeys();

        for (PgpKeyInfo pgpKeyInfo : pgpKeyInfoArray) {
            PgpKey pgpKey = js.crypto_key_read(pgpKeyInfo.getArmored());
            publicKeys.add(pgpKey.toPublic().armor());
        }

        return publicKeys;
    }
}
