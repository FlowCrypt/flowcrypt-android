/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.model;

import android.os.Parcel;
import android.os.Parcelable;

import com.flowcrypt.email.js.PgpContact;
import com.flowcrypt.email.ui.loader.UpdateInfoAboutPgpContactsAsyncTaskLoader;

import java.util.List;

/**
 * The simple POJO class which describes information about result from the
 * {@link UpdateInfoAboutPgpContactsAsyncTaskLoader}
 *
 * @author Denis Bondarenko
 *         Date: 31.07.2017
 *         Time: 17:31
 *         E-mail: DenBond7@gmail.com
 */

public class UpdateInfoAboutPgpContactsResult implements Parcelable {
    public static final Creator<UpdateInfoAboutPgpContactsResult> CREATOR = new
            Creator<UpdateInfoAboutPgpContactsResult>() {
                @Override
                public UpdateInfoAboutPgpContactsResult createFromParcel(Parcel source) {
                    return new UpdateInfoAboutPgpContactsResult(source);
                }

                @Override
                public UpdateInfoAboutPgpContactsResult[] newArray(int size) {
                    return new UpdateInfoAboutPgpContactsResult[size];
                }
            };

    private List<String> emails;
    private boolean isAllInfoReceived;
    private List<PgpContact> updatedPgpContacts;

    public UpdateInfoAboutPgpContactsResult(List<String> emails, boolean isAllInfoReceived,
                                            List<PgpContact> updatedPgpContacts) {
        this.emails = emails;
        this.isAllInfoReceived = isAllInfoReceived;
        this.updatedPgpContacts = updatedPgpContacts;
    }

    protected UpdateInfoAboutPgpContactsResult(Parcel in) {
        this.emails = in.createStringArrayList();
        this.isAllInfoReceived = in.readByte() != 0;
        this.updatedPgpContacts = in.createTypedArrayList(PgpContact.CREATOR);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeStringList(this.emails);
        dest.writeByte(this.isAllInfoReceived ? (byte) 1 : (byte) 0);
        dest.writeTypedList(this.updatedPgpContacts);
    }

    public List<String> getEmails() {
        return emails;
    }

    public boolean isAllInfoReceived() {
        return isAllInfoReceived;
    }

    public List<PgpContact> getUpdatedPgpContacts() {
        return updatedPgpContacts;
    }
}
