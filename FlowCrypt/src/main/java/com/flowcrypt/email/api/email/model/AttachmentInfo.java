/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (human@flowcrypt.com).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.model;

import android.accounts.Account;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;

import com.flowcrypt.email.database.dao.source.AccountDao;

/**
 * Simple POJO which defines information about email attachments.
 *
 * @author Denis Bondarenko
 *         Date: 07.08.2017
 *         Time: 18:38
 *         E-mail: DenBond7@gmail.com
 */

public class AttachmentInfo implements Parcelable {

    public static final Creator<AttachmentInfo> CREATOR = new Creator<AttachmentInfo>() {
        @Override
        public AttachmentInfo createFromParcel(Parcel source) {
            return new AttachmentInfo(source);
        }

        @Override
        public AttachmentInfo[] newArray(int size) {
            return new AttachmentInfo[size];
        }
    };
    private String email;
    private String folder;
    private int uid;
    private String name;
    private long encodedSize;
    private String type;
    private String id;
    private Uri uri;
    private boolean isCanBeDeleted = true;

    public AttachmentInfo() {
    }

    protected AttachmentInfo(Parcel in) {
        this.email = in.readString();
        this.folder = in.readString();
        this.uid = in.readInt();
        this.name = in.readString();
        this.encodedSize = in.readLong();
        this.type = in.readString();
        this.id = in.readString();
        this.uri = in.readParcelable(Uri.class.getClassLoader());
        this.isCanBeDeleted = in.readByte() != 0;
    }

    @Override
    public String toString() {
        return "AttachmentInfo{" +
                "email='" + email + '\'' +
                ", folder='" + folder + '\'' +
                ", uid=" + uid +
                ", name='" + name + '\'' +
                ", encodedSize=" + encodedSize +
                ", type='" + type + '\'' +
                ", id='" + id + '\'' +
                ", uri=" + uri +
                ", isCanBeDeleted=" + isCanBeDeleted +
                '}';
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.email);
        dest.writeString(this.folder);
        dest.writeInt(this.uid);
        dest.writeString(this.name);
        dest.writeLong(this.encodedSize);
        dest.writeString(this.type);
        dest.writeString(this.id);
        dest.writeParcelable(this.uri, flags);
        dest.writeByte(this.isCanBeDeleted ? (byte) 1 : (byte) 0);
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFolder() {
        return folder;
    }

    public void setFolder(String folder) {
        this.folder = folder;
    }

    public int getUid() {
        return uid;
    }

    public void setUid(int uid) {
        this.uid = uid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getEncodedSize() {
        return encodedSize;
    }

    public void setEncodedSize(long encodedSize) {
        this.encodedSize = encodedSize;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Uri getUri() {
        return uri;
    }

    public void setUri(Uri uri) {
        this.uri = uri;
    }

    public boolean isCanBeDeleted() {
        return isCanBeDeleted;
    }

    public void setCanBeDeleted(boolean isCanBeDeleted) {
        this.isCanBeDeleted = isCanBeDeleted;
    }

    @Nullable
    public Account getGoogleAccount() {
        return this.email == null ? null : new Account(this.email, AccountDao.ACCOUNT_TYPE_GOOGLE);
    }
}
