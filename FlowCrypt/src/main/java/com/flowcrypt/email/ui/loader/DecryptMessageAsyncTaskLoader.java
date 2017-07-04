/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (tom@cryptup.org).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.loader;

import android.content.Context;
import android.os.Build;
import android.support.v4.content.AsyncTaskLoader;
import android.text.Html;
import android.text.TextUtils;

import com.flowcrypt.email.api.email.model.IncomingMessageInfo;
import com.flowcrypt.email.api.email.model.MessageInfo;
import com.flowcrypt.email.js.Js;
import com.flowcrypt.email.js.MimeAddress;
import com.flowcrypt.email.js.MimeMessage;
import com.flowcrypt.email.js.PgpDecrypted;
import com.flowcrypt.email.model.results.LoaderResult;
import com.flowcrypt.email.security.SecurityStorageConnector;

import java.util.ArrayList;
import java.util.Date;

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
        IncomingMessageInfo messageInfo = new IncomingMessageInfo();
        if (rawMessage != null) {
            Js js = new Js(getContext(), new SecurityStorageConnector(getContext()));
            MimeMessage mimeMessage = js.mime_decode(rawMessage);
            ArrayList<String> addresses = new ArrayList<>();

            for (MimeAddress mimeAddress : mimeMessage.getAddressHeader("from")) {
                addresses.add(mimeAddress.getAddress());
            }

            messageInfo.setFrom(addresses);
            messageInfo.setSubject(mimeMessage.getStringHeader("subject"));
            messageInfo.setReceiveDate(new Date(mimeMessage.getTimeHeader("date")));
            messageInfo.setMessage(decryptMessageIfNeed(js, mimeMessage));
            messageInfo.setOriginalRawMessageWithoutAttachments(rawMessage);
        } else {
            return null;
        }
        return messageInfo;
    }

    /**
     * Decrypt a message if it encrypted. At now will be decrypted only a simple text.
     *
     * @param js          The Js object which used to decrypt a message text.
     * @param mimeMessage The MimeMessage object.
     * @return <tt>String</tt> Return a decrypted or original text.
     */
    @SuppressWarnings("deprecation")
    private String decryptMessageIfNeed(Js js, MimeMessage mimeMessage) {
        if (TextUtils.isEmpty(mimeMessage.getText())) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                return Html.fromHtml(mimeMessage.getHtml(), Html.FROM_HTML_MODE_LEGACY).toString();
            } else {
                return Html.fromHtml(mimeMessage.getHtml()).toString();
            }
        } else {
            String decryptedText = js.crypto_armor_clip(mimeMessage.getText());
            if (decryptedText != null) {
                PgpDecrypted pgpDecrypted = js.crypto_message_decrypt(decryptedText);
                try {
                    return pgpDecrypted != null ? pgpDecrypted.getContent() : "";
                } catch (Exception e) {
                    e.printStackTrace();
                    return mimeMessage.getText();
                }
            } else {
                return mimeMessage.getText();
            }
        }
    }
}
