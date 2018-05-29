/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.request.model;

import android.os.Parcel;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * The request model for the https://flowcrypt.com/api/link/message API.
 *
 * @author Denis Bondarenko
 *         Date: 13.07.2017
 *         Time: 15:12
 *         E-mail: DenBond7@gmail.com
 */

public class LinkMessageModel extends BaseRequestModel {
    public static final Creator<LinkMessageModel> CREATOR = new Creator<LinkMessageModel>() {
        @Override
        public LinkMessageModel createFromParcel(Parcel source) {
            return new LinkMessageModel(source);
        }

        @Override
        public LinkMessageModel[] newArray(int size) {
            return new LinkMessageModel[size];
        }
    };

    @SerializedName("short")
    @Expose
    private String shortValue;

    public LinkMessageModel(String shortValue) {
        this.shortValue = shortValue;
    }


    protected LinkMessageModel(Parcel in) {
        this.shortValue = in.readString();
    }

    @Override
    public String toString() {
        return "LinkMessageModel{" +
                "shortValue='" + shortValue + '\'' +
                "} " + super.toString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.shortValue);
    }
}
