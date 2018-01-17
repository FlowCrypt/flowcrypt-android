/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (human@flowcrypt.com).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.loader;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.support.v4.content.AsyncTaskLoader;

import com.flowcrypt.email.api.email.EmailUtil;
import com.flowcrypt.email.api.email.gmail.GmailApiHelper;
import com.flowcrypt.email.api.email.protocol.OpenStoreHelper;
import com.flowcrypt.email.api.email.protocol.SmtpProtocolUtil;
import com.flowcrypt.email.api.retrofit.ApiHelper;
import com.flowcrypt.email.api.retrofit.ApiService;
import com.flowcrypt.email.api.retrofit.request.model.InitialLegacySubmitModel;
import com.flowcrypt.email.api.retrofit.request.model.TestWelcomeModel;
import com.flowcrypt.email.api.retrofit.response.attester.InitialLegacySubmitResponse;
import com.flowcrypt.email.api.retrofit.response.attester.TestWelcomeResponse;
import com.flowcrypt.email.database.dao.KeysDao;
import com.flowcrypt.email.database.dao.source.AccountDao;
import com.flowcrypt.email.database.dao.source.KeysDaoSource;
import com.flowcrypt.email.js.Js;
import com.flowcrypt.email.js.PgpContact;
import com.flowcrypt.email.js.PgpKey;
import com.flowcrypt.email.model.KeyDetails;
import com.flowcrypt.email.model.results.LoaderResult;
import com.flowcrypt.email.security.KeyStoreCryptoManager;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.ListSendAsResponse;
import com.google.api.services.gmail.model.SendAs;

import org.acra.ACRA;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;

import retrofit2.Response;

/**
 * This loader does job of creating a private key and returns the private key long id as result.
 *
 * @author DenBond7
 *         Date: 12.01.2018.
 *         Time: 12:36.
 *         E-mail: DenBond7@gmail.com
 */
public class CreatePrivateKeyAsyncTaskLoader extends AsyncTaskLoader<LoaderResult> {

    private static final int DEFAULT_KEY_SIZE = 2048;

    private final String passphrase;
    private final AccountDao accountDao;
    private boolean isActionStarted;
    private LoaderResult data;

    public CreatePrivateKeyAsyncTaskLoader(Context context, AccountDao accountDao, String passphrase) {
        super(context);
        this.accountDao = accountDao;
        this.passphrase = passphrase;
    }

    @Override
    public void onStartLoading() {
        if (data != null) {
            deliverResult(data);
        } else {
            if (!isActionStarted) {
                forceLoad();
            }
        }
    }

    @Override
    public LoaderResult loadInBackground() {
        isActionStarted = true;
        PgpKey pgpKey = null;
        try {
            pgpKey = createPgpKey();

            if (pgpKey == null) {
                return new LoaderResult(false, new NullPointerException("The generated private key is null!"));
            }

            Uri uri = new KeysDaoSource().addRow(getContext(),
                    KeysDao.generateKeysDao(new KeyStoreCryptoManager(getContext()),
                            new KeyDetails(null, pgpKey.armor(), null,
                                    KeyDetails.Type.NEW, true, pgpKey.getPrimaryUserId()), pgpKey, passphrase));

            if (uri == null) {
                return new LoaderResult(false, new NullPointerException("Cannot save the generated private key"));
            }

            if (!saveCreatedPrivateKeyAsBackupToInbox(pgpKey)) {
                new KeysDaoSource().removeKey(getContext(), pgpKey);
                return new LoaderResult(false, new NullPointerException("Cannot save a copy of the private key in " +
                        "INBOX"));
            }

            registerUserPublicKey(pgpKey);
            requestingTestMessageWithNewPublicKey(pgpKey);

            return new LoaderResult(pgpKey.getLongid(), null);
        } catch (Exception e) {
            e.printStackTrace();
            new KeysDaoSource().removeKey(getContext(), pgpKey);
            if (ACRA.isInitialised()) {
                ACRA.getErrorReporter().handleException(e);
            }
            return new LoaderResult(false, e);
        }
    }

    @Override
    public void deliverResult(@Nullable LoaderResult data) {
        this.data = data;
        super.deliverResult(data);
    }

    /**
     * Perform a backup of the armored key in INBOX.
     *
     * @return true if message was send.
     * @throws Exception Some exceptions can be occurred.
     */
    private boolean saveCreatedPrivateKeyAsBackupToInbox(PgpKey pgpKey) throws Exception {
        Session session = OpenStoreHelper.getSessionForAccountDao(accountDao);
        Transport transport = SmtpProtocolUtil.prepareTransportForSmtp(getContext(), session, accountDao);
        Message message = EmailUtil.generateMessageWithPrivateKeysBackup(getContext(), accountDao.getEmail(),
                session, EmailUtil.generateAttachmentBodyPartWithPrivateKey(accountDao.getEmail(), pgpKey, -1));
        transport.sendMessage(message, message.getAllRecipients());
        return true;
    }

    /**
     * Create a private PGP key.
     *
     * @return Generated {@link PgpKey}
     * @throws IOException Some exceptions can be throw.
     */
    private PgpKey createPgpKey() throws Exception {
        PgpContact pgpContactMain = new PgpContact(accountDao.getEmail(), accountDao.getDisplayName());
        PgpContact[] pgpContacts;
        switch (accountDao.getAccountType()) {
            case AccountDao.ACCOUNT_TYPE_GOOGLE:
                List<PgpContact> pgpContactList = new ArrayList<>();
                pgpContactList.add(pgpContactMain);
                Gmail gmail = GmailApiHelper.generateGmailApiService(getContext(), accountDao);
                ListSendAsResponse aliases = gmail.users().settings().sendAs().list("me").execute();
                for (SendAs alias : aliases.getSendAs()) {
                    if (alias.getVerificationStatus() != null) {
                        pgpContactList.add(new PgpContact(alias.getSendAsEmail(), alias.getDisplayName()));
                    }
                }
                pgpContacts = pgpContactList.toArray(new PgpContact[0]);
                break;

            default:
                pgpContacts = new PgpContact[]{pgpContactMain};
                break;
        }

        return new Js(getContext(), null).crypto_key_create(pgpContacts, DEFAULT_KEY_SIZE, passphrase);
    }

    /**
     * Registering a key with attester API.
     * Note: this will only be successful if it's the first time submitting a key for this email address, or if the
     * key being submitted has the same fingerprint as the one already recorded. If it's an error due to key
     * conflict, ignore the error.
     *
     * @param pgpKey A created PGP key.
     * @return true if no errors.
     * @throws IOException
     */
    private boolean registerUserPublicKey(PgpKey pgpKey) throws IOException {
        ApiService apiService = ApiHelper.getInstance(getContext()).getRetrofit().create(ApiService.class);
        Response<InitialLegacySubmitResponse> response = apiService.postInitialLegacySubmit(
                new InitialLegacySubmitModel(accountDao.getEmail(), pgpKey.toPublic().armor())).execute();

        InitialLegacySubmitResponse initialLegacySubmitResponse = response.body();
        return initialLegacySubmitResponse != null;
    }

    /**
     * Request a test email from FlowCrypt.
     *
     * @param pgpKey A created PGP key.
     * @return true if no errors.
     * @throws IOException
     */
    private boolean requestingTestMessageWithNewPublicKey(PgpKey pgpKey) throws IOException {
        ApiService apiService = ApiHelper.getInstance(getContext()).getRetrofit().create(ApiService.class);
        Response<TestWelcomeResponse> response = apiService.postTestWelcome(
                new TestWelcomeModel(accountDao.getEmail(), pgpKey.toPublic().armor())).execute();

        TestWelcomeResponse testWelcomeResponse = response.body();
        return testWelcomeResponse != null && testWelcomeResponse.isSent();
    }
}
