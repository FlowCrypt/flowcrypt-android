/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (human@flowcrypt.com).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.loader;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.content.AsyncTaskLoader;
import android.text.TextUtils;

import com.flowcrypt.email.R;
import com.flowcrypt.email.api.email.model.IncomingMessageInfo;
import com.flowcrypt.email.api.email.model.MessageInfo;
import com.flowcrypt.email.database.dao.source.ContactsDaoSource;
import com.flowcrypt.email.js.Js;
import com.flowcrypt.email.js.MessageBlock;
import com.flowcrypt.email.js.MimeAddress;
import com.flowcrypt.email.js.MimeMessage;
import com.flowcrypt.email.js.PgpContact;
import com.flowcrypt.email.js.PgpDecrypted;
import com.flowcrypt.email.js.PgpKey;
import com.flowcrypt.email.js.ProcessedMime;
import com.flowcrypt.email.model.messages.MessagePart;
import com.flowcrypt.email.model.messages.MessagePartPgpMessage;
import com.flowcrypt.email.model.messages.MessagePartPgpPublicKey;
import com.flowcrypt.email.model.messages.MessagePartText;
import com.flowcrypt.email.model.messages.MessagePartType;
import com.flowcrypt.email.model.results.LoaderResult;
import com.flowcrypt.email.security.SecurityStorageConnector;

import org.acra.ACRA;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

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
            ACRA.getErrorReporter().handleException(e);
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
            incomingMessageInfo.setMessageParts(getMessagePartsFromProcessedMime(js, processedMime));

            MimeMessage mimeMessage = js.mime_decode(rawMessage);

            if (mimeMessage != null && !isMessageContainsPGPBlocks(incomingMessageInfo)) {
                incomingMessageInfo.setHtmlMessage(mimeMessage.getHtml());
            }

        } else {
            return null;
        }
        return incomingMessageInfo;
    }

    /**
     * Check that {@link IncomingMessageInfo} contains PGP blocks.
     *
     * @param incomingMessageInfo The incoming message.
     * @return true if {@link IncomingMessageInfo} contains PGP blocks
     * ({@link MessagePartType#PGP_MESSAGE}, {@link MessagePartType#PGP_PUBLIC_KEY},
     * {@link MessagePartType#PGP_PASSWORD_MESSAGE},  {@link MessagePartType#PGP_SIGNED_MESSAGE}), otherwise - false
     */
    private boolean isMessageContainsPGPBlocks(IncomingMessageInfo incomingMessageInfo) {
        if (incomingMessageInfo != null) {
            List<MessagePart> messageParts = incomingMessageInfo.getMessageParts();

            if (messageParts != null) {
                for (MessagePart messagePart : messageParts) {
                    if (messagePart.getMessagePartType() != null) {
                        switch (messagePart.getMessagePartType()) {
                            case PGP_MESSAGE:
                            case PGP_PUBLIC_KEY:
                            case PGP_PASSWORD_MESSAGE:
                            case PGP_SIGNED_MESSAGE:
                                return true;
                        }
                    }
                }
            }
        }
        return false;
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
                        messageParts.add(generateMessagePartPgpMessage(js, messageBlock));
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
     * Generate {@link MessagePartPgpMessage} from encrypted {@link MessageBlock}.
     *
     * @param js           The {@link Js} util;
     * @param messageBlock The encrypted {@link MessageBlock}.
     * @return Generated {@link MessagePartPgpMessage}.
     */
    @NonNull
    private MessagePartPgpMessage generateMessagePartPgpMessage(Js js, MessageBlock messageBlock) {
        String encryptedContent = messageBlock.getContent();
        String value = encryptedContent;
        String errorMessage = null;
        MessagePartPgpMessage.PgpMessageDecryptError pgpMessageDecryptError = null;

        if (TextUtils.isEmpty(encryptedContent)) {
            return new MessagePartPgpMessage("", null, null);
        }

        PgpDecrypted pgpDecrypted = js.crypto_message_decrypt(encryptedContent);

        if (pgpDecrypted != null) {
            if (pgpDecrypted.isSuccess()) {
                value = pgpDecrypted.getString();
            } else if (!TextUtils.isEmpty(pgpDecrypted.getFormatError())) {
                errorMessage = getContext().getString(R.string.decrypt_error_message_badly_formatted,
                        getContext().getString(R.string.app_name)) + "\n\n" + pgpDecrypted.getFormatError();
                pgpMessageDecryptError = MessagePartPgpMessage.PgpMessageDecryptError.FORMAT_ERROR;
            } else if (pgpDecrypted.getMissingPassphraseLongids() != null
                    && pgpDecrypted.getMissingPassphraseLongids().length > 0) {
                pgpMessageDecryptError = MessagePartPgpMessage.PgpMessageDecryptError.MISSING_PASS_PHRASES;
            } else if (Objects.equals(pgpDecrypted.countPotentiallyMatchingKeys(), pgpDecrypted.countAttempts())
                    && Objects.equals(pgpDecrypted.countKeyMismatchErrors(), pgpDecrypted.countAttempts())) {
                pgpMessageDecryptError = MessagePartPgpMessage.PgpMessageDecryptError.MISSING_PRIVATE_KEY;
                if (pgpDecrypted.getEncryptedForLongids().length > 1) {
                    errorMessage = getContext().getString(R.string.decrypt_error_current_key_cannot_message);
                } else {
                    errorMessage = getContext().getString(R.string.decrypt_error_could_not_open_message,
                            getContext().getString(R.string.app_name)) + "\n\n" +
                            getContext().getString(R.string.decrypt_error_single_sender);
                }
            } else if (pgpDecrypted.countUnsecureMdcErrors() > 0) {
                pgpMessageDecryptError = MessagePartPgpMessage.PgpMessageDecryptError.UNSECURED_MDC_ERROR;
            } else if (pgpDecrypted.getOtherErrors() != null && pgpDecrypted.getOtherErrors().length > 0) {
                pgpMessageDecryptError = MessagePartPgpMessage.PgpMessageDecryptError.OTHER_ERRORS;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(getContext().getString(R.string.decrypt_error_could_not_open_message,
                        getContext().getString(R.string.app_name)));
                stringBuilder.append("\n\n");
                stringBuilder.append(getContext().getString(R.string.decrypt_error_please_write_me, getContext()
                        .getString(R.string.support_email)));
                stringBuilder.append("\n\n");

                for (String s : pgpDecrypted.getOtherErrors()) {
                    stringBuilder.append(s);
                    stringBuilder.append("\n");
                }

                errorMessage = stringBuilder.toString();
            } else {
                pgpMessageDecryptError = MessagePartPgpMessage.PgpMessageDecryptError.UNKNOWN_ERROR;
                errorMessage = getContext().getString(R.string.decrypt_error_could_not_open_message,
                        getContext().getString(R.string.app_name)) +
                        "\n\n" + getContext().getString(R.string.decrypt_error_please_write_me);
            }
        } else {
            pgpMessageDecryptError = MessagePartPgpMessage.PgpMessageDecryptError.JS_TOOL_ERROR;
            errorMessage = getContext().getString(R.string.decrypt_error_js_tool_error) + "\n\n" +
                    getContext().getString(R.string.decrypt_error_please_write_me);
        }

        return new MessagePartPgpMessage(value, errorMessage, pgpMessageDecryptError);
    }
}
