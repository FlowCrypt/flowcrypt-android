/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.request.model;

import android.os.Parcel;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * The request model for the https://flowcrypt.com/attester/replace/request API.
 *
 * @author Denis Bondarenko
 * Date: 13.07.2017
 * Time: 10:03
 * E-mail: DenBond7@gmail.com
 */

public class ReplaceRequestModel extends BaseRequestModel {
  public static final Creator<ReplaceRequestModel> CREATOR = new Creator<ReplaceRequestModel>() {
    @Override
    public ReplaceRequestModel createFromParcel(Parcel source) {
      return new ReplaceRequestModel(source);
    }

    @Override
    public ReplaceRequestModel[] newArray(int size) {
      return new ReplaceRequestModel[size];
    }
  };

  @SerializedName("signed_message")
  @Expose
  private String signedMsg;

  @SerializedName("new_pubkey")
  @Expose
  private String newPubKey;

  @SerializedName("email")
  @Expose
  private String email;

  public ReplaceRequestModel(String signedMsg, String newPubKey, String email) {
    this.signedMsg = signedMsg;
    this.newPubKey = newPubKey;
    this.email = email;
  }

  protected ReplaceRequestModel(Parcel in) {
    this.signedMsg = in.readString();
    this.newPubKey = in.readString();
    this.email = in.readString();
  }

  @Override
  public String toString() {
    return "ReplaceRequestModel{" +
        "signedMsg='" + signedMsg + '\'' +
        ", newPubKey='" + newPubKey + '\'' +
        ", email='" + email + '\'' +
        "} " + super.toString();
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeString(this.signedMsg);
    dest.writeString(this.newPubKey);
    dest.writeString(this.email);
  }
}
