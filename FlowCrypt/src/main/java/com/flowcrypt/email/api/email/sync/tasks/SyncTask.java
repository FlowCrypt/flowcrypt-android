/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (tom@cryptup.org).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.sync.tasks;

import com.flowcrypt.email.api.email.sync.GmailSynsManager;
import com.flowcrypt.email.api.email.sync.SyncListener;
import com.sun.mail.gimap.GmailSSLStore;

/**
 * The sync task which will be runned by {@link GmailSynsManager}
 *
 * @author DenBond7
 *         Date: 16.06.2017
 *         Time: 16:12
 *         E-mail: DenBond7@gmail.com
 */

public interface SyncTask {
    /**
     * Run current task in the separate thread.
     *
     * @param gmailSSLStore The connected and opened {@link GmailSSLStore} object.
     * @param syncListener  The listener of synchronization.
     * @throws Exception
     */
    void run(GmailSSLStore gmailSSLStore, SyncListener syncListener) throws Exception;
}
