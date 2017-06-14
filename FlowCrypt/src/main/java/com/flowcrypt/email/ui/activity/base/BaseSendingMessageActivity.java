/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (tom@cryptup.org). Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/tree/master/src/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.base;

import android.widget.Toast;

import com.flowcrypt.email.R;

/**
 * The base activity which describe a sending message logic.
 *
 * @author DenBond7
 *         Date: 10.05.2017
 *         Time: 11:43
 *         E-mail: DenBond7@gmail.com
 */

public abstract class BaseSendingMessageActivity extends BaseBackStackAuthenticationActivity {
    /**
     * Check the message sending status.
     *
     * @return true if message was sent, false otherwise.
     */
    public abstract boolean isMessageSendingNow();

    @Override
    public void onBackPressed() {
        if (isMessageSendingNow()) {
            Toast.makeText(this, R.string.please_wait_while_message_will_be_sent, Toast
                    .LENGTH_SHORT).show();
        } else {
            super.onBackPressed();
        }
    }
}
