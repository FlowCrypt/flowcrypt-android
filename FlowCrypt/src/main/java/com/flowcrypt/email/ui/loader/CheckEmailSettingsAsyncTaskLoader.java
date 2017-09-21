/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (tom@cryptup.org).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.loader;

import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;

import com.flowcrypt.email.BuildConfig;
import com.flowcrypt.email.api.email.JavaEmailConstants;
import com.flowcrypt.email.api.email.model.AuthCredentials;
import com.flowcrypt.email.api.email.protocol.PropertiesHelper;
import com.flowcrypt.email.model.results.LoaderResult;

import javax.mail.Folder;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.Transport;

/**
 * This loader does job of valid {@link AuthCredentials}. If incoming {@link AuthCredentials} is valid then this
 * loader returns {@code true}, otherwise returns false.
 *
 * @author DenBond7
 *         Date: 14.09.2017.
 *         Time: 15:08.
 *         E-mail: DenBond7@gmail.com
 */
public class CheckEmailSettingsAsyncTaskLoader extends AsyncTaskLoader<LoaderResult> {

    private final AuthCredentials authCredentials;

    public CheckEmailSettingsAsyncTaskLoader(Context context, AuthCredentials authCredentials) {
        super(context);
        this.authCredentials = authCredentials;
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
            Session session = Session.getInstance(
                    PropertiesHelper.generatePropertiesFromAuthCredentials(authCredentials));
            session.setDebug(BuildConfig.DEBUG);

            testImapConnection(session);
            testSmtpConnection(session);

            return new LoaderResult(true, null);
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
     * Trying to connect to an IMAP server. If an exception will occur than that exception will be throw up.
     *
     * @param session The {@link Session} which will be used for the connection.
     * @throws MessagingException This operation can throw some exception.
     */
    private void testImapConnection(Session session) throws MessagingException {
        Store store = session.getStore(JavaEmailConstants.PROTOCOL_IMAP);
        store.connect(authCredentials.getImapServer(), authCredentials.getUsername(),
                authCredentials.getPassword());
        Folder folder = store.getFolder(JavaEmailConstants.FOLDER_INBOX);
        folder.open(Folder.READ_ONLY);
        folder.close(false);
        store.close();
    }

    /**
     * Trying to connect to the SMTP server. If an exception will occur than that exception will be throw up.
     *
     * @param session The {@link Session} which will be used for the connection.
     * @throws MessagingException This operation can throw some exception.
     */
    private void testSmtpConnection(Session session) throws MessagingException {
        Transport transport = session.getTransport(JavaEmailConstants.PROTOCOL_SMTP);
        String username;
        String password;

        if (authCredentials.isUseCustomSignInForSmtp()) {
            username = authCredentials.getSmtpSigInUsername();
            password = authCredentials.getSmtpSignInPassword();
        } else {
            username = authCredentials.getUsername();
            password = authCredentials.getPassword();
        }

        transport.connect(authCredentials.getSmtpServer(), authCredentials.getSmtpPort(), username, password);
        transport.close();
    }
}
