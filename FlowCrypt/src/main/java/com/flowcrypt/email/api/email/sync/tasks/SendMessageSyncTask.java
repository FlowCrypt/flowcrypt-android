/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (tom@cryptup.org).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.sync.tasks;

import android.os.Messenger;

import com.flowcrypt.email.api.email.JavaEmailConstants;
import com.flowcrypt.email.api.email.gmail.GmailConstants;
import com.flowcrypt.email.api.email.sync.SyncListener;

import org.apache.commons.io.IOUtils;

import java.nio.charset.StandardCharsets;

import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.MimeMessage;

/**
 * This task does job of sending a message.
 *
 * @author DenBond7
 *         Date: 29.06.2017
 *         Time: 11:38
 *         E-mail: DenBond7@gmail.com
 */

public class SendMessageSyncTask extends BaseSyncTask {
    private String rawEncryptedMessage;

    /**
     * The base constructor.
     *
     * @param ownerKey            The name of the reply to {@link Messenger}.
     * @param requestCode         The unique request code for the reply to {@link Messenger}.
     * @param rawEncryptedMessage The raw encrypted message..
     */
    public SendMessageSyncTask(String ownerKey, int requestCode, String rawEncryptedMessage) {
        super(ownerKey, requestCode);
        this.rawEncryptedMessage = rawEncryptedMessage;
    }

    @Override
    public boolean isUseSMTP() {
        return true;
    }

    @Override
    public void run(Session session, String userName, String password, SyncListener syncListener)
            throws Exception {
        super.run(session, userName, password, syncListener);
        if (syncListener != null) {
            MimeMessage mimeMessage = new MimeMessage(session,
                    IOUtils.toInputStream(rawEncryptedMessage, StandardCharsets.UTF_8));

            Transport transport = session.getTransport(JavaEmailConstants.PROTOCOL_SMTP);
            transport.connect(GmailConstants.HOST_SMTP_GMAIL_COM,
                    GmailConstants.PORT_SMTP_GMAIL_COM, userName, password);

            transport.sendMessage(mimeMessage, mimeMessage.getAllRecipients());
            syncListener.onEncryptedMessageSent(ownerKey, requestCode, true);
        }
    }
}
