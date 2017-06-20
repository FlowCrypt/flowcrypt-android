/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (tom@cryptup.org).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.sync;

import com.google.android.gms.auth.GoogleAuthException;

import java.io.IOException;

import javax.mail.Folder;
import javax.mail.Message;

/**
 * This class can be used for communication with {@link GmailSynsManager}
 *
 * @author DenBond7
 *         Date: 19.06.2017
 *         Time: 13:35
 *         E-mail: DenBond7@gmail.com
 */

public interface SyncListener {
    /**
     * This method called when a new messages received from the some folder.
     *
     * @param folder   The folder where the new messages exist.
     * @param messages The new messages.
     */
    void onMessageReceived(Folder folder, Message[] messages);

    /**
     * This method called when new folders list received.
     *
     * @param folders The new folders list.
     */
    void onFolderInfoReceived(Folder[] folders);

    /**
     * Handle an error of synchronization.
     *
     * @param errorType The error type code.
     * @param e         The exception that occurred during synchronization.
     */
    void onError(int errorType, Exception e);

    /**
     * Get a valid OAuth2 token for current user. Must be called on the non-UI thread.
     *
     * @return A new valid OAuth2 token;
     * @throws IOException
     * @throws GoogleAuthException
     */
    String getValidToken() throws IOException, GoogleAuthException;

    /**
     * Get the email of the current account.
     *
     * @return The account email.
     */
    String getEmail();
}
