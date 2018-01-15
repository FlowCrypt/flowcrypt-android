/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (human@flowcrypt.com).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.database.dao;

import android.os.Parcel;
import android.text.TextUtils;

import com.flowcrypt.email.database.dao.source.BaseDaoSource;
import com.flowcrypt.email.js.PgpKey;
import com.flowcrypt.email.model.KeyDetails;
import com.flowcrypt.email.security.KeyStoreCryptoManager;
import com.flowcrypt.email.security.model.PrivateKeySourceType;

import java.util.UUID;

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

    /**
     * Generate {@link KeysDao} using input parameters.
     * This method use {@link PgpKey#getLongid()} for generate an algorithm parameter spec String and
     * {@link KeyStoreCryptoManager} for generate encrypted version of the private key and password.
     *
     * @param keyStoreCryptoManager A {@link KeyStoreCryptoManager} which will bu used to encrypt
     *                              information about a key;
     * @param keyDetails            The private key details
     * @param pgpKey                A normalized key;
     * @param passphrase            A passphrase which user provided;
     */
    public static KeysDao generateKeysDao(KeyStoreCryptoManager keyStoreCryptoManager, KeyDetails keyDetails,
                                          PgpKey pgpKey, String passphrase) throws Exception {
        KeysDao keysDao = new KeysDao();
        keysDao.setLongId(pgpKey.getLongid());

        String randomVector;

        if (TextUtils.isEmpty(pgpKey.getLongid())) {
            randomVector = KeyStoreCryptoManager.normalizeAlgorithmParameterSpecString(
                    UUID.randomUUID().toString().substring(0,
                            KeyStoreCryptoManager.SIZE_OF_ALGORITHM_PARAMETER_SPEC));
        } else {
            randomVector = KeyStoreCryptoManager.normalizeAlgorithmParameterSpecString
                    (pgpKey.getLongid());
        }

        switch (keyDetails.getBornType()) {
            case EMAIL:
                keysDao.setPrivateKeySourceType(PrivateKeySourceType.BACKUP);
                break;

            case FILE:
            case CLIPBOARD:
                keysDao.setPrivateKeySourceType(PrivateKeySourceType.IMPORT);
                break;

            case NEW:
                keysDao.setPrivateKeySourceType(PrivateKeySourceType.NEW);
                break;
        }

        String encryptedPrivateKey = keyStoreCryptoManager.encrypt(pgpKey.armor(), randomVector);
        keysDao.setPrivateKey(encryptedPrivateKey);
        keysDao.setPublicKey(pgpKey.toPublic().armor());

        String encryptedPassphrase = keyStoreCryptoManager.encrypt(passphrase, randomVector);
        keysDao.setPassphrase(encryptedPassphrase);
        return keysDao;
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
