/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (human@flowcrypt.com).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.service.actionqueue.actions;

import android.content.Context;
import android.os.Parcelable;

/**
 * Must be run in non-UI thread. This class describes an action which will be run on a queue.
 *
 * @author Denis Bondarenko
 *         Date: 29.01.2018
 *         Time: 16:56
 *         E-mail: DenBond7@gmail.com
 */

public abstract class Action implements Parcelable {

    private long id;

    public abstract boolean run(Context context);

    public abstract ActionType getType();

    public abstract String getEmail();


    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    /**
     * This class contains information about all action types.
     */
    public enum ActionType {
        BACKUP_PRIVATE_KEY_TO_INBOX("backup_private_key_to_inbox");


        private String value;

        ActionType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }
}
