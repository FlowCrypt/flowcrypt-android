/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (tom@cryptup.org).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.loader;

import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;

import com.flowcrypt.email.api.email.model.IncomingMessageInfo;
import com.flowcrypt.email.api.email.model.MessageInfo;
import com.flowcrypt.email.database.dao.source.ContactsDaoSource;
import com.flowcrypt.email.js.Js;
import com.flowcrypt.email.js.MessageBlock;
import com.flowcrypt.email.js.MimeAddress;
import com.flowcrypt.email.js.PgpContact;
import com.flowcrypt.email.js.PgpDecrypted;
import com.flowcrypt.email.js.PgpKey;
import com.flowcrypt.email.js.ProcessedMime;
import com.flowcrypt.email.model.messages.MessagePart;
import com.flowcrypt.email.model.messages.MessagePartPgpMessage;
import com.flowcrypt.email.model.messages.MessagePartPgpPublicKey;
import com.flowcrypt.email.model.messages.MessagePartText;
import com.flowcrypt.email.model.results.LoaderResult;
import com.flowcrypt.email.security.SecurityStorageConnector;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

/**
 * This loader decrypt if need a message and return
 * {@link IncomingMessageInfo} object.
 *
 * @author DenBond7
 *         Date: 04.05.2017
 *         Time: 9:59
 *         E-mail: DenBond7@gmail.com
 */

public class DecryptMessageAsyncTaskLoader extends AsyncTaskLoader<LoaderResult> {
    private String rawMessageWithoutAttachments;

    public DecryptMessageAsyncTaskLoader(Context context, String rawMessageWithoutAttachments) {
        super(context);
        this.rawMessageWithoutAttachments = rawMessageWithoutAttachments;
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
            IncomingMessageInfo messageInfo = parseRawMessage(rawMessageWithoutAttachments);
            return new LoaderResult(messageInfo, null);
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
     * Parse an original message and return {@link MessageInfo} object.
     *
     * @param rawMessage Original message which will be parsed.
     * @return <tt>MessageInfo</tt> Return a MessageInfo object.
     * @throws Exception The parsing process can be throws different exceptions.
     */
    private IncomingMessageInfo parseRawMessage(String rawMessage) throws Exception {
        IncomingMessageInfo incomingMessageInfo = new IncomingMessageInfo();
        if (rawMessage != null) {
            Js js = new Js(getContext(), new SecurityStorageConnector(getContext()));
            ProcessedMime processedMime = js.mime_process(rawMessage);
            ArrayList<String> addressesFrom = new ArrayList<>();
            ArrayList<String> addressesTo = new ArrayList<>();

            for (MimeAddress mimeAddress : processedMime.getAddressHeader("from")) {
                addressesFrom.add(mimeAddress.getAddress());
            }

            for (MimeAddress mimeAddress : processedMime.getAddressHeader("to")) {
                addressesTo.add(mimeAddress.getAddress());
            }

            incomingMessageInfo.setFrom(addressesFrom);
            incomingMessageInfo.setTo(addressesTo);
            incomingMessageInfo.setSubject(processedMime.getStringHeader("subject"));
            incomingMessageInfo.setReceiveDate(new Date(processedMime.getTimeHeader("date")));
            incomingMessageInfo.setOriginalRawMessageWithoutAttachments(rawMessage);
            incomingMessageInfo.setMessageParts(getMessagePartsFromProcessedMime(js,
                    processedMime));
        } else {
            return null;
        }
        return incomingMessageInfo;
    }

    /**
     * Generate a list of {@link MessagePart} object which contains information about
     * {@link MessageBlock}
     *
     * @param js            The {@link Js} util.
     * @param processedMime The {@link ProcessedMime} object which contains information about an
     *                      encrypted message.
     * @return The list of {@link MessagePart}.
     */
    private List<MessagePart> getMessagePartsFromProcessedMime(Js js, ProcessedMime processedMime) {
        MessageBlock[] blocks = processedMime.getBlocks();

        LinkedList<MessagePart> messageParts = new LinkedList<>();

        for (MessageBlock messageBlock : blocks) {
            if (messageBlock != null && messageBlock.getType() != null) {
                switch (messageBlock.getType()) {
                    case MessageBlock.TYPE_TEXT:
                        messageParts.add(new MessagePartText(messageBlock.getContent()));
                        break;

                    case MessageBlock.TYPE_PGP_MESSAGE:
                        messageParts.add(new MessagePartPgpMessage(decryptText(js,
                                messageBlock.getContent())));
                        break;

                    case MessageBlock.TYPE_PGP_PUBLIC_KEY:
                        String publicKey = messageBlock.getContent();
                        String fingerprint =
                                js.crypto_key_fingerprint(js.crypto_key_read(publicKey));
                        String longId = js.crypto_key_longid(fingerprint);
                        String keywords = js.mnemonic(longId);
                        PgpKey pgpKey = js.crypto_key_read(publicKey);
                        String keyOwner = pgpKey.getPrimaryUserId().getEmail();

                        PgpContact pgpContact =
                                new ContactsDaoSource().getPgpContact(getContext(), keyOwner);

                        MessagePartPgpPublicKey messagePartPgpPublicKey
                                = new MessagePartPgpPublicKey(publicKey, longId, keywords,
                                fingerprint, keyOwner, pgpContact);

                        messageParts.add(messagePartPgpPublicKey);
                        break;

                    //Todo-DenBond7 need to describe other types of MessageBlock
                }
            }
        }
        return messageParts;
    }

    /**
     * Decrypt an encrypted text.
     *
     * @param js            The {@link Js} util.
     * @param encryptedText The encrypted text which will be decrypted.
     * @return The decrypted text.
     */
    private String decryptText(Js js, String encryptedText) {
        if (encryptedText != null) {
            PgpDecrypted pgpDecrypted = js.crypto_message_decrypt(encryptedText);
            try {
                return pgpDecrypted != null ? pgpDecrypted.getContent() : "";
            } catch (Exception e) {
                e.printStackTrace();
                return encryptedText;
            }
        } else {
            return null;
        }
    }
}
