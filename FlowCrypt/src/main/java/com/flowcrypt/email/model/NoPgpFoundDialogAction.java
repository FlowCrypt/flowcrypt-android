/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (tom@cryptup.org).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.model;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Simple POJO class which describes an action in the {@link NoPgpFoundDialogAction}
 *
 * @author Denis Bondarenko
 *         Date: 01.08.2017
 *         Time: 11:29
 *         E-mail: DenBond7@gmail.com
 */

public class NoPgpFoundDialogAction implements Parcelable {
    public static final Creator<NoPgpFoundDialogAction> CREATOR = new
            Creator<NoPgpFoundDialogAction>() {
                @Override
                public NoPgpFoundDialogAction createFromParcel(Parcel source) {
                    return new NoPgpFoundDialogAction(source);
                }

                @Override
                public NoPgpFoundDialogAction[] newArray(int size) {
                    return new NoPgpFoundDialogAction[size];
                }
            };

    private int iconResourceId;
    private String title;
    private int id;

    public NoPgpFoundDialogAction() {
    }

    public NoPgpFoundDialogAction(int iconResourceId, String title, int id) {
        this.iconResourceId = iconResourceId;
        this.title = title;
        this.id = id;
    }

    protected NoPgpFoundDialogAction(Parcel in) {
        this.iconResourceId = in.readInt();
        this.title = in.readString();
        this.id = in.readInt();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.iconResourceId);
        dest.writeString(this.title);
        dest.writeInt(this.id);
    }

    public int getIconResourceId() {
        return iconResourceId;
    }

    public String getTitle() {
        return title;
    }

    public int getId() {
        return id;
    }
}
