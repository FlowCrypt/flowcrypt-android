/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.model;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Simple POJO class which describes a dialog item.
 *
 * @author Denis Bondarenko
 *         Date: 01.08.2017
 *         Time: 11:29
 *         E-mail: DenBond7@gmail.com
 */

public class DialogItem implements Parcelable {
    public static final Creator<DialogItem> CREATOR = new
            Creator<DialogItem>() {
                @Override
                public DialogItem createFromParcel(Parcel source) {
                    return new DialogItem(source);
                }

                @Override
                public DialogItem[] newArray(int size) {
                    return new DialogItem[size];
                }
            };

    private int iconResourceId;
    private String title;
    private int id;

    public DialogItem() {
    }

    public DialogItem(int iconResourceId, String title, int id) {
        this.iconResourceId = iconResourceId;
        this.title = title;
        this.id = id;
    }

    protected DialogItem(Parcel in) {
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
