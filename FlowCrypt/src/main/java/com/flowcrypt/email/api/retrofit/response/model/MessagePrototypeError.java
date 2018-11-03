/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.response.model;

import android.os.Parcel;

import com.flowcrypt.email.api.retrofit.response.base.BaseResponseModel;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * The message prototype error which can be received from the API
 * "https://flowcrypt.com/api/message/prototype"
 *
 * @author DenBond7
 * Date: 24.04.2017
 * Time: 13:58
 * E-mail: DenBond7@gmail.com
 */

public class MessagePrototypeError extends BaseResponseModel {
  public static final Creator<MessagePrototypeError> CREATOR = new
      Creator<MessagePrototypeError>() {
        @Override
        public MessagePrototypeError createFromParcel(Parcel source) {
          return new MessagePrototypeError(source);
        }

        @Override
        public MessagePrototypeError[] newArray(int size) {
          return new MessagePrototypeError[size];
        }
      };

  @SerializedName("internal_msg")
  @Expose
  private String internalMsg;

  @SerializedName("code")
  @Expose
  private int code;

  @SerializedName("public_msg")
  @Expose
  private String publicMsg;

  public MessagePrototypeError() {
  }

  protected MessagePrototypeError(Parcel in) {
    this.internalMsg = in.readString();
    this.code = in.readInt();
    this.publicMsg = in.readString();
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeString(this.internalMsg);
    dest.writeInt(this.code);
    dest.writeString(this.publicMsg);
  }

  public String getInternalMsg() {
    return internalMsg;
  }

  public int getCode() {
    return code;
  }

  public String getPublicMsg() {
    return publicMsg;
  }
}
