/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.request.model;

import android.os.Parcel;

import com.flowcrypt.email.api.retrofit.request.api.PostHelpFeedbackRequest;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * The model of {@link PostHelpFeedbackRequest}.
 *
 * @author DenBond7
 * Date: 30.05.2017
 * Time: 12:42
 * E-mail: DenBond7@gmail.com
 */

public class PostHelpFeedbackModel extends BaseRequestModel {
  public static final Creator<PostHelpFeedbackModel> CREATOR = new
      Creator<PostHelpFeedbackModel>() {
        @Override
        public PostHelpFeedbackModel createFromParcel(Parcel source) {
          return new PostHelpFeedbackModel(source);
        }

        @Override
        public PostHelpFeedbackModel[] newArray(int size) {
          return new PostHelpFeedbackModel[size];
        }
      };

  @Expose
  private String email;

  @SerializedName("message")
  @Expose
  private String msg;

  public PostHelpFeedbackModel() {
  }

  public PostHelpFeedbackModel(String email, String msg) {
    this.email = email;
    this.msg = msg;
  }

  public PostHelpFeedbackModel(Parcel in) {
    this.email = in.readString();
    this.msg = in.readString();
  }

  @Override
  public String toString() {
    return "PostHelpFeedbackModel{" +
        "email='" + email + '\'' +
        ", msg='" + msg + '\'' +
        "} " + super.toString();
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeString(this.email);
    dest.writeString(this.msg);
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getMsg() {
    return msg;
  }

  public void setMsg(String message) {
    this.msg = message;
  }
}
