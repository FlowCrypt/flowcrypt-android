package com.flowcrypt.email.ui.loader;

import android.accounts.Account;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.content.AsyncTaskLoader;
import android.text.TextUtils;

import com.flowcrypt.email.api.email.JavaEmailConstants;
import com.flowcrypt.email.api.email.gmail.GmailConstants;
import com.flowcrypt.email.api.email.model.OutgoingMessageInfo;
import com.flowcrypt.email.api.email.protocol.PropertiesHelper;
import com.flowcrypt.email.api.retrofit.ApiHelper;
import com.flowcrypt.email.api.retrofit.ApiService;
import com.flowcrypt.email.api.retrofit.request.model.PostLookUpEmailModel;
import com.flowcrypt.email.api.retrofit.response.LookUpEmailResponse;
import com.flowcrypt.email.model.results.ActionResult;
import com.flowcrypt.email.security.SecurityStorageConnector;
import com.flowcrypt.email.test.Js;
import com.flowcrypt.email.test.PgpContact;
import com.flowcrypt.email.test.PgpKey;
import com.flowcrypt.email.test.PgpKeyInfo;
import com.google.android.gms.auth.GoogleAuthUtil;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.MimeMessage;

import retrofit2.Response;

/**
 * This loader do a job of sending encrypted message. When we sending a message, we do next steps:
 * <ul>
 * <li>1) generate an access token for OAuth2 authentication;</li>
 * <li>2) generate a smtp session;</li>
 * <li>3) get public keys for recipients from the server + keys of the sender(generated locally);
 * </li>
 * <li>4) doing encrypt the text with public keys;</li>
 * <li>5) create an {@link MimeMessage} using {@link Js} object</li>
 * <li>6) Send generated {@link MimeMessage} object</li>
 * </ul>
 *
 * @author DenBond7
 *         Date: 08.05.2017
 *         Time: 15:36
 *         E-mail: DenBond7@gmail.com
 */

public class SendEncryptedMessageAsyncTaskLoader extends AsyncTaskLoader<ActionResult<Boolean>> {
    private Account account;
    private OutgoingMessageInfo outgoingMessageInfo;

    public SendEncryptedMessageAsyncTaskLoader(Context context, @NonNull Account account,
                                               @NonNull OutgoingMessageInfo outgoingMessageInfo) {
        super(context);
        this.account = account;
        this.outgoingMessageInfo = outgoingMessageInfo;

        this.outgoingMessageInfo.setFromPgpContact(new PgpContact(account.name, null));
        onContentChanged();
    }

    @Override
    public ActionResult<Boolean> loadInBackground() {
        try {
            String token = GoogleAuthUtil.getToken(getContext(), account,
                    JavaEmailConstants.OAUTH2 + GmailConstants.SCOPE_MAIL_GOOGLE_COM);

            String username = account.name;
            Session session = Session.getInstance(
                    PropertiesHelper.generatePropertiesForGmailSmtp());
            Js js = new Js(getContext(), new SecurityStorageConnector(getContext()));

            String[] pubKeys = getPubKeys(js);

            if (pubKeys.length > 0) {
                String encryptedText = js.crypto_message_encrypt(pubKeys,
                        outgoingMessageInfo.getMessage(), true);

                String encryptedMessage = js.mime_encode(encryptedText,
                        outgoingMessageInfo.getToPgpContacts(),
                        outgoingMessageInfo.getFromPgpContact(),
                        outgoingMessageInfo.getSubject(),
                        null);

                MimeMessage mimeMessage = new MimeMessage(session,
                        IOUtils.toInputStream(encryptedMessage, StandardCharsets.UTF_8));

                Transport transport = session.getTransport(JavaEmailConstants.PROTOCOL_SMTP);
                transport.connect(GmailConstants.HOST_SMTP_GMAIL_COM,
                        GmailConstants.PORT_SMTP_GMAIL_COM, username, token);

                transport.sendMessage(mimeMessage, mimeMessage.getAllRecipients());

                return new ActionResult<>(true, null);
            } else return new ActionResult<>(false, null);
        } catch (Exception e) {
            e.printStackTrace();
            return new ActionResult<>(false, e);
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
        ApiService apiService = ApiHelper.getInstance().getRetrofit().create(ApiService.class);
        for (PgpContact pgpContact : outgoingMessageInfo.getToPgpContacts()) {
            try {
                Response<LookUpEmailResponse> response = apiService.postLookUpEmail(
                        new PostLookUpEmailModel(pgpContact.getEmail())).execute();
                String pubKey = response.body().getPubkey();
                if (!TextUtils.isEmpty(pubKey)) {
                    publicKeys.add(pubKey);
                }
            } catch (IOException e) {
                e.printStackTrace();
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
