/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.response.model;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;

/**
 * This POJO class describes information about a public key from the
 * API <code> https://attester.flowcrypt.com/lookup/</code>
 *
 * @author Denis Bondarenko
 * Date: 05.05.2018
 * Time: 14:04
 * E-mail: DenBond7@gmail.com
 */
public class LookUpPublicKeyInfo implements Parcelable {
    public static final Creator<LookUpPublicKeyInfo> CREATOR = new Creator<LookUpPublicKeyInfo>() {
        @Override
        public LookUpPublicKeyInfo createFromParcel(Parcel source) {
            return new LookUpPublicKeyInfo(source);
        }

        @Override
        public LookUpPublicKeyInfo[] newArray(int size) {
            return new LookUpPublicKeyInfo[size];
        }
    };

    @SerializedName("longid")
    @Expose
    private String longId;

    @SerializedName("pubkey")
    @Expose
    private String publicKey;

    @Expose
    private String query;

    @Expose
    private ArrayList<String> attests;

    public LookUpPublicKeyInfo() {
    }

    protected LookUpPublicKeyInfo(Parcel in) {
        this.longId = in.readString();
        this.publicKey = in.readString();
        this.query = in.readString();
        this.attests = in.createStringArrayList();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.longId);
        dest.writeString(this.publicKey);
        dest.writeString(this.query);
        dest.writeStringList(this.attests);
    }

    public String getLongId() {
        return longId;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public String getQuery() {
        return query;
    }

    public ArrayList<String> getAttests() {
        return attests;
    }
}
