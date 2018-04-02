/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.js.tasks;

import android.content.Context;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.flowcrypt.email.R;
import com.flowcrypt.email.api.email.model.IncomingMessageInfo;
import com.flowcrypt.email.database.dao.source.ContactsDaoSource;
import com.flowcrypt.email.js.Js;
import com.flowcrypt.email.js.JsListener;
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
import com.flowcrypt.email.model.messages.MessagePartSignedMessage;
import com.flowcrypt.email.model.messages.MessagePartText;
import com.flowcrypt.email.model.messages.MessagePartType;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

/**
 * This task can be used for decryption a raw MIME message.
 *
 * @author Denis Bondarenko
 *         Date: 16.02.2018
 *         Time: 10:39
 *         E-mail: DenBond7@gmail.com
 */

public class DecryptRawMimeMessageJsTask extends BaseJsTask {
    private String rawMimeMessage;

    public DecryptRawMimeMessageJsTask(String ownerKey, int requestCode, String rawMimeMessage) {
        super(ownerKey, requestCode);
        this.rawMimeMessage = rawMimeMessage;
    }

    @Override
    public void runAction(Js js, JsListener jsListener) throws Exception {
        IncomingMessageInfo incomingMessageInfo = new IncomingMessageInfo();
        if (rawMimeMessage != null) {
            ProcessedMime processedMime = js.mime_process(rawMimeMessage);
            ArrayList<String> addressesFrom = new ArrayList<>();
            ArrayList<String> addressesTo = new ArrayList<>();
            ArrayList<String> addressesCc = new ArrayList<>();

            for (MimeAddress mimeAddress : processedMime.getAddressHeader("from")) {
                addressesFrom.add(mimeAddress.getAddress());
            }

            for (MimeAddress mimeAddress : processedMime.getAddressHeader("to")) {
                addressesTo.add(mimeAddress.getAddress());
            }

            for (MimeAddress mimeAddress : processedMime.getAddressHeader("cc")) {
                addressesCc.add(mimeAddress.getAddress());
            }

            incomingMessageInfo.setFrom(addressesFrom);
            incomingMessageInfo.setTo(addressesTo);
            incomingMessageInfo.setCc(addressesCc);
            incomingMessageInfo.setSubject(processedMime.getStringHeader("subject"));
            incomingMessageInfo.setReceiveDate(new Date(processedMime.getTimeHeader("date")));
            incomingMessageInfo.setOriginalRawMessageWithoutAttachments(rawMimeMessage);
            incomingMessageInfo.setMessageParts(getMessagePartsFromProcessedMime(jsListener.getContext(), js,
                    processedMime));

            MimeMessage mimeMessage = js.mime_decode(rawMimeMessage);

            if (mimeMessage != null && !isMessageContainsPGPBlocks(incomingMessageInfo)) {
                incomingMessageInfo.setHtmlMessage(mimeMessage.getHtml());
            }

            jsListener.onMessageDecrypted(ownerKey, requestCode, incomingMessageInfo);
        } else {
            jsListener.onError(JsErrorTypes.TASK_RUNNING_ERROR,
                    new NullPointerException("The raw MIME message is null!"), ownerKey, requestCode);
        }
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
     * @param context       Interface to global information about an application environment;
     * @param js            The {@link Js} util.
     * @param processedMime The {@link ProcessedMime} object which contains information about an
     *                      encrypted message.
     * @return The list of {@link MessagePart}.
     */
    private List<MessagePart> getMessagePartsFromProcessedMime(Context context, Js js, ProcessedMime processedMime) {
        MessageBlock[] blocks = processedMime.getBlocks();

        LinkedList<MessagePart> messageParts = new LinkedList<>();

        for (MessageBlock messageBlock : blocks) {
            if (messageBlock != null && messageBlock.getType() != null) {
                switch (messageBlock.getType()) {
                    case MessageBlock.TYPE_TEXT:
                        messageParts.add(new MessagePartText(messageBlock.getContent()));
                        break;

                    case MessageBlock.TYPE_PGP_MESSAGE:
                        messageParts.add(generateMessagePartPgpMessage(context, js, messageBlock));
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
                                new ContactsDaoSource().getPgpContact(context, keyOwner);

                        MessagePartPgpPublicKey messagePartPgpPublicKey
                                = new MessagePartPgpPublicKey(publicKey, longId, keywords,
                                fingerprint, keyOwner, pgpContact);

                        messageParts.add(messagePartPgpPublicKey);
                        break;

                    case MessageBlock.TYPE_PGP_SIGNED_MESSAGE:
                        messageParts.add(new MessagePartSignedMessage(messageBlock.getContent()));
                        break;

                    case MessageBlock.TYPE_VERIFICATION:
                        messageParts.add(new MessagePart(MessagePartType.VERIFICATION,
                                messageBlock.getContent()));
                        break;

                    case MessageBlock.TYPE_ATTEST_PACKET:
                        messageParts.add(new MessagePart(MessagePartType.ATTEST_PACKET,
                                messageBlock.getContent()));
                        break;

                    case MessageBlock.TYPE_PGP_PASSWORD_MESSAGE:
                        messageParts.add(new MessagePart(MessagePartType.PGP_PASSWORD_MESSAGE,
                                messageBlock.getContent()));
                        break;
                }
            }
        }
        return messageParts;
    }

    /**
     * Generate {@link MessagePartPgpMessage} from encrypted {@link MessageBlock}.
     *
     * @param context      Interface to global information about an application environment;
     * @param js           The {@link Js} util;
     * @param messageBlock The encrypted {@link MessageBlock}.
     * @return Generated {@link MessagePartPgpMessage}.
     */
    @NonNull
    private MessagePartPgpMessage generateMessagePartPgpMessage(Context context, Js js, MessageBlock messageBlock) {
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
                errorMessage = context.getString(R.string.decrypt_error_message_badly_formatted,
                        context.getString(R.string.app_name)) + "\n\n" + pgpDecrypted.getFormatError();
                pgpMessageDecryptError = MessagePartPgpMessage.PgpMessageDecryptError.FORMAT_ERROR;
            } else if (pgpDecrypted.getMissingPassphraseLongids() != null
                    && pgpDecrypted.getMissingPassphraseLongids().length > 0) {
                pgpMessageDecryptError = MessagePartPgpMessage.PgpMessageDecryptError.MISSING_PASS_PHRASES;
            } else if (Objects.equals(pgpDecrypted.countPotentiallyMatchingKeys(), pgpDecrypted.countAttempts())
                    && Objects.equals(pgpDecrypted.countKeyMismatchErrors(), pgpDecrypted.countAttempts())) {
                pgpMessageDecryptError = MessagePartPgpMessage.PgpMessageDecryptError.MISSING_PRIVATE_KEY;
                if (pgpDecrypted.getEncryptedForLongids().length > 1) {
                    errorMessage = context.getString(R.string.decrypt_error_current_key_cannot_message);
                } else {
                    errorMessage = context.getString(R.string.decrypt_error_could_not_open_message,
                            context.getString(R.string.app_name)) + "\n\n" +
                            context.getString(R.string.decrypt_error_single_sender);
                }
            } else if (pgpDecrypted.countUnsecureMdcErrors() > 0) {
                pgpMessageDecryptError = MessagePartPgpMessage.PgpMessageDecryptError.UNSECURED_MDC_ERROR;
            } else if (pgpDecrypted.getOtherErrors() != null && pgpDecrypted.getOtherErrors().length > 0) {
                pgpMessageDecryptError = MessagePartPgpMessage.PgpMessageDecryptError.OTHER_ERRORS;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(context.getString(R.string.decrypt_error_could_not_open_message,
                        context.getString(R.string.app_name)));
                stringBuilder.append("\n\n");
                stringBuilder.append(context.getString(R.string.decrypt_error_please_write_me, context
                        .getString(R.string.support_email)));
                stringBuilder.append("\n\n");

                for (String s : pgpDecrypted.getOtherErrors()) {
                    stringBuilder.append(s);
                    stringBuilder.append("\n");
                }

                errorMessage = stringBuilder.toString();
            } else {
                pgpMessageDecryptError = MessagePartPgpMessage.PgpMessageDecryptError.UNKNOWN_ERROR;
                errorMessage = context.getString(R.string.decrypt_error_could_not_open_message,
                        context.getString(R.string.app_name)) +
                        "\n\n" + context.getString(R.string.decrypt_error_please_write_me);
            }
        } else {
            pgpMessageDecryptError = MessagePartPgpMessage.PgpMessageDecryptError.JS_TOOL_ERROR;
            errorMessage = context.getString(R.string.decrypt_error_js_tool_error) + "\n\n" +
                    context.getString(R.string.decrypt_error_please_write_me);
        }

        return new MessagePartPgpMessage(value, errorMessage, pgpMessageDecryptError);
    }
}
