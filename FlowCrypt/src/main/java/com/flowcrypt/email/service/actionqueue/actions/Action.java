/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.service.actionqueue.actions;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.SerializedName;

/**
 * Must be run in non-UI thread. This class describes an action which will be run on a queue.
 *
 * @author Denis Bondarenko
 *         Date: 29.01.2018
 *         Time: 16:56
 *         E-mail: DenBond7@gmail.com
 */

public class Action implements Parcelable {

    public static final String TAG_NAME_ACTION_TYPE = "actionType";

    public static final Creator<Action> CREATOR = new Creator<Action>() {
        @Override
        public Action createFromParcel(Parcel source) {
            return new Action(source);
        }

        @Override
        public Action[] newArray(int size) {
            return new Action[size];
        }
    };

    @SerializedName(TAG_NAME_ACTION_TYPE)
    private final ActionType actionType;
    protected long id;
    protected String email;
    protected int version = 0;

    public Action(String email, ActionType actionType) {
        this.email = email;
        this.actionType = actionType;
    }

    protected Action(Parcel in) {
        int tmpActionType = in.readInt();
        this.actionType = tmpActionType == -1 ? null : ActionType.values()[tmpActionType];
        this.id = in.readLong();
        this.email = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.actionType == null ? -1 : this.actionType.ordinal());
        dest.writeLong(this.id);
        dest.writeString(this.email);
    }

    public ActionType getActionType() {
        return actionType;
    }

    public void run(Context context) throws Exception {
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    /**
     * This class contains information about all action types.
     */
    public enum ActionType {
        BACKUP_PRIVATE_KEY_TO_INBOX("backup_private_key_to_inbox"),
        REGISTER_USER_PUBLIC_KEY("register_user_public_key"),
        SEND_WELCOME_TEST_EMAIL("send_welcome_test_email");

        private String value;

        ActionType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }
}
