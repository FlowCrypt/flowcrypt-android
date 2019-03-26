/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.response.model.node;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import androidx.annotation.NonNull;

/**
 * @author Denis Bondarenko
 * Date: 3/26/19
 * Time: 3:30 PM
 * E-mail: DenBond7@gmail.com
 */
public class DecryptErrorDetails implements Parcelable {
  public static final Creator<DecryptErrorDetails> CREATOR = new Creator<DecryptErrorDetails>() {
    @Override
    public DecryptErrorDetails createFromParcel(Parcel source) {
      return new DecryptErrorDetails(source);
    }

    @Override
    public DecryptErrorDetails[] newArray(int size) {
      return new DecryptErrorDetails[size];
    }
  };

  @Expose
  private Type type;

  @Expose
  private String message;

  public DecryptErrorDetails() {
  }

  protected DecryptErrorDetails(Parcel in) {
    int tmpType = in.readInt();
    this.type = tmpType == -1 ? null : Type.values()[tmpType];
    this.message = in.readString();
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeInt(this.type == null ? -1 : this.type.ordinal());
    dest.writeString(this.message);
  }

  @NonNull
  @Override
  public String toString() {
    return "DecryptErrorDetails{" +
        "type=" + type +
        ", message='" + message + '\'' +
        '}';
  }

  public Type getType() {
    return type;
  }

  public String getMessage() {
    return message;
  }

  public enum Type {
    UNKNOWN,

    @SerializedName("key_mismatch")
    KEY_MISMATCH,

    @SerializedName("use_password")
    USE_PASSWORD,

    @SerializedName("wrong_password")
    WRONG_PASSWORD,

    @SerializedName("no_mdc")
    NO_MDC,

    @SerializedName("need_passphrase")
    NEED_PASSPHRASE,

    @SerializedName("format")
    FORMAT,

    @SerializedName("other")
    OTHER
  }
}
