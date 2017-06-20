/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (tom@cryptup.org).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Arrays;

/**
 * This is a simple POJO object, which describe an information about the email folder.
 *
 * @author DenBond7
 *         Date: 07.06.2017
 *         Time: 14:49
 *         E-mail: DenBond7@gmail.com
 */

public class Folder implements Parcelable {

    public static final Creator<Folder> CREATOR = new Creator<Folder>() {
        @Override
        public Folder createFromParcel(Parcel source) {
            return new Folder(source);
        }

        @Override
        public Folder[] newArray(int size) {
            return new Folder[size];
        }
    };
    private String serverFullFolderName;
    private String folderAlias;
    private String[] attributes;
    private boolean isCustomLabel;

    public Folder(String serverFullFolderName, String folderAlias, String[] attributes, boolean
            isCustomLabel) {
        this.serverFullFolderName = serverFullFolderName;
        this.folderAlias = folderAlias;
        this.attributes = attributes;
        this.isCustomLabel = isCustomLabel;
    }

    public Folder(String folderAlias, String serverFullFolderName, boolean isCustomLabel) {
        this.folderAlias = folderAlias;
        this.serverFullFolderName = serverFullFolderName;
        this.isCustomLabel = isCustomLabel;
    }

    protected Folder(Parcel in) {
        this.serverFullFolderName = in.readString();
        this.folderAlias = in.readString();
        this.attributes = in.createStringArray();
        this.isCustomLabel = in.readByte() != 0;
    }

    @Override
    public String toString() {
        return "Folder{" +
                "serverFullFolderName='" + serverFullFolderName + '\'' +
                ", folderAlias='" + folderAlias + '\'' +
                ", attributes=" + Arrays.toString(attributes) +
                ", isCustomLabel=" + isCustomLabel +
                '}';
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.serverFullFolderName);
        dest.writeString(this.folderAlias);
        dest.writeStringArray(this.attributes);
        dest.writeByte(this.isCustomLabel ? (byte) 1 : (byte) 0);
    }

    public String getFolderAlias() {
        return folderAlias;
    }

    public void setFolderAlias(String folderAlias) {
        this.folderAlias = folderAlias;
    }

    public String getServerFullFolderName() {
        return serverFullFolderName;
    }

    public boolean isCustomLabel() {
        return isCustomLabel;
    }

    public String[] getAttributes() {
        return attributes;
    }

    public void setAttributes(String[] attributes) {
        this.attributes = attributes;
    }
}
