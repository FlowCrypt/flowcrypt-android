/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.request.model;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * This is a POJO object which used to make a request to the API "https://attester.cryptup
 * .io/lookup/email" to retrieve info about an array of keys.
 *
 * @author Denis Bondarenko
 * Date: 13.11.2017
 * Time: 15:16
 * E-mail: DenBond7@gmail.com
 */

public class PostLookUpEmailsModel implements Parcelable {
  public static final Parcelable.Creator<PostLookUpEmailsModel> CREATOR = new Parcelable
      .Creator<PostLookUpEmailsModel>() {
    @Override
    public PostLookUpEmailsModel createFromParcel(Parcel source) {
      return new PostLookUpEmailsModel(source);
    }

    @Override
    public PostLookUpEmailsModel[] newArray(int size) {
      return new PostLookUpEmailsModel[size];
    }
  };

  @SerializedName("email")
  @Expose
  private List<String> emails;

  public PostLookUpEmailsModel() {
  }

  public PostLookUpEmailsModel(List<String> emails) {
    this.emails = emails;
  }

  public PostLookUpEmailsModel(Parcel in) {
    this.emails = in.createStringArrayList();
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeStringList(this.emails);
  }

  public List<String> getEmails() {
    return emails;
  }
}
