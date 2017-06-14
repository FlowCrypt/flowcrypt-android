/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (tom@cryptup.org). Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/tree/master/src/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.database.dao;

import android.os.Parcel;

import com.flowcrypt.email.database.dao.source.BaseDaoSource;
import com.flowcrypt.email.security.model.PrivateKeySourceType;

/**
 * This class describe a key information object.
 *
 * @author DenBond7
 *         Date: 13.05.2017
 *         Time: 12:54
 *         E-mail: DenBond7@gmail.com
 */

public class KeysDao extends BaseDao {

    public static final Creator<KeysDao> CREATOR = new Creator<KeysDao>() {
        @Override
        public KeysDao createFromParcel(Parcel source) {
            return new KeysDao(source);
        }

        @Override
        public KeysDao[] newArray(int size) {
            return new KeysDao[size];
        }
    };

    private String longId;
    private PrivateKeySourceType privateKeySourceType;
    private String publicKey;
    private String privateKey;
    private String passphrase;

    public KeysDao() {
    }

    public KeysDao(String longId, PrivateKeySourceType privateKeySourceType, String publicKey,
                   String
                           privateKey, String
                           passphrase) {
        this.longId = longId;
        this.privateKeySourceType = privateKeySourceType;
        this.publicKey = publicKey;
        this.privateKey = privateKey;
        this.passphrase = passphrase;
    }

    protected KeysDao(Parcel in) {
        this.longId = in.readString();
        int tmpPrivateKeySourceType = in.readInt();
        this.privateKeySourceType = tmpPrivateKeySourceType == -1 ? null : PrivateKeySourceType
                .values()[tmpPrivateKeySourceType];
        this.publicKey = in.readString();
        this.privateKey = in.readString();
        this.passphrase = in.readString();
    }

    @Override
    public BaseDaoSource getDaoSource() {
        return null;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.longId);
        dest.writeInt(this.privateKeySourceType == null ? -1 : this.privateKeySourceType.ordinal());
        dest.writeString(this.publicKey);
        dest.writeString(this.privateKey);
        dest.writeString(this.passphrase);
    }

    public String getLongId() {
        return longId;
    }

    public void setLongId(String longId) {
        this.longId = longId;
    }

    public PrivateKeySourceType getPrivateKeySourceType() {
        return privateKeySourceType;
    }

    public void setPrivateKeySourceType(PrivateKeySourceType privateKeySourceType) {
        this.privateKeySourceType = privateKeySourceType;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
    }

    public String getPassphrase() {
        return passphrase;
    }

    public void setPassphrase(String passphrase) {
        this.passphrase = passphrase;
    }
}
