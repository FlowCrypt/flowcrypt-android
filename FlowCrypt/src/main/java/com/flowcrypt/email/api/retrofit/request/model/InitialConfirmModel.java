/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.request.model;

import android.os.Parcel;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * The request model for the https://attester.flowcrypt.com/initial/confirm API.
 *
 * @author Denis Bondarenko
 * Date: 12.07.2017
 * Time: 17:09
 * E-mail: DenBond7@gmail.com
 */

public class InitialConfirmModel extends BaseRequestModel {
  public static final Creator<InitialConfirmModel> CREATOR = new Creator<InitialConfirmModel>() {
    @Override
    public InitialConfirmModel createFromParcel(Parcel source) {
      return new InitialConfirmModel(source);
    }

    @Override
    public InitialConfirmModel[] newArray(int size) {
      return new InitialConfirmModel[size];
    }
  };

  @SerializedName("signed_message")
  @Expose
  private String signedMsg;

  public InitialConfirmModel() {
  }


  protected InitialConfirmModel(Parcel in) {
    this.signedMsg = in.readString();
  }

  @Override
  public String toString() {
    return "InitialConfirmModel{" +
        "signedMsg='" + signedMsg + '\'' +
        "} " + super.toString();
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeString(this.signedMsg);
  }

  public void setSignedMsg(String signedMsg) {
    this.signedMsg = signedMsg;
  }
}
