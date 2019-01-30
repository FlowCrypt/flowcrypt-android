/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.request.model;

import android.os.Parcel;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * The request model for the https://attester.flowcrypt.com/test/welcome API.
 *
 * @author Denis Bondarenko
 * Date: 12.07.2017
 * Time: 16:40
 * E-mail: DenBond7@gmail.com
 */

public class TestWelcomeModel extends BaseRequestModel {
  public static final Creator<TestWelcomeModel> CREATOR = new Creator<TestWelcomeModel>() {
    @Override
    public TestWelcomeModel createFromParcel(Parcel source) {
      return new TestWelcomeModel(source);
    }

    @Override
    public TestWelcomeModel[] newArray(int size) {
      return new TestWelcomeModel[size];
    }
  };

  @SerializedName("email")
  @Expose
  private String email;

  @SerializedName("pubkey")
  @Expose
  private String pubKey;

  public TestWelcomeModel(String email, String pubKey) {
    this.email = email;
    this.pubKey = pubKey;
  }


  protected TestWelcomeModel(Parcel in) {
    this.email = in.readString();
    this.pubKey = in.readString();
  }

  @Override
  public String toString() {
    return "TestWelcomeModel{" +
        "email='" + email + '\'' +
        ", pubKey='" + pubKey + '\'' +
        "} " + super.toString();
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeString(this.email);
    dest.writeString(this.pubKey);
  }
}
