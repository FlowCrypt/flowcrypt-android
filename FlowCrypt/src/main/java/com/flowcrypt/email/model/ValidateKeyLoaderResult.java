/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.model;

import android.os.Parcel;
import android.os.Parcelable;

import com.flowcrypt.email.js.PgpContact;
import com.flowcrypt.email.ui.loader.ValidateKeyAsyncTaskLoader;

/**
 * The results object for {@link ValidateKeyAsyncTaskLoader}
 *
 * @author Denis Bondarenko
 *         Date: 03.08.2017
 *         Time: 14:56
 *         E-mail: DenBond7@gmail.com
 */

public class ValidateKeyLoaderResult implements Parcelable {
    public static final Creator<ValidateKeyLoaderResult> CREATOR = new
            Creator<ValidateKeyLoaderResult>() {
                @Override
                public ValidateKeyLoaderResult createFromParcel(Parcel source) {
                    return new ValidateKeyLoaderResult(source);
                }

                @Override
                public ValidateKeyLoaderResult[] newArray(int size) {
                    return new ValidateKeyLoaderResult[size];
                }
            };

    private PgpContact pgpContact;
    private String key;
    private boolean isValidated;

    public ValidateKeyLoaderResult(String key, PgpContact pgpContact, boolean isValidated) {
        this.key = key;
        this.pgpContact = pgpContact;
        this.isValidated = isValidated;
    }

    public ValidateKeyLoaderResult(Parcel in) {
        this.pgpContact = in.readParcelable(PgpContact.class.getClassLoader());
        this.key = in.readString();
        this.isValidated = in.readByte() != 0;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(this.pgpContact, flags);
        dest.writeString(this.key);
        dest.writeByte(this.isValidated ? (byte) 1 : (byte) 0);
    }

    public PgpContact getPgpContact() {
        return pgpContact;
    }

    public boolean isValidated() {
        return isValidated;
    }

    public String getKey() {
        return key;
    }
}
