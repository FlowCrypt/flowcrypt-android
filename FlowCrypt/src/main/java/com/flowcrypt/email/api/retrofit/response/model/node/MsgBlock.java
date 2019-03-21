/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.response.model.node;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.Expose;

public class MsgBlock implements Parcelable {

  public static final String TYPE_TEXT = "text";
  public static final String TYPE_PGP_MESSAGE = "message";
  public static final String TYPE_PGP_PUBLIC_KEY = "public_key";
  public static final String TYPE_PGP_SIGNED_MESSAGE = "signed_message";
  public static final String TYPE_PGP_PASSWORD_MESSAGE = "password_message";
  public static final String TYPE_ATTEST_PACKET = "attest_packet";
  public static final String TYPE_VERIFICATION = "cryptup_verification";
  public static final String TYPE_PGP_PRIVATE_KEY = "private_key";
  public static final String TYPE_ATTACHMENT = "attachment";
  public static final String TYPE_HTML = "html";

  public static final Creator<MsgBlock> CREATOR = new Creator<MsgBlock>() {
    @Override
    public MsgBlock createFromParcel(Parcel source) {
      return new MsgBlock(source);
    }

    @Override
    public MsgBlock[] newArray(int size) {
      return new MsgBlock[size];
    }
  };

  @Expose
  private String type;

  @Expose
  private String content;

  @Expose
  private boolean complete;

  public MsgBlock() {
  }

  protected MsgBlock(Parcel in) {
    this.type = in.readString();
    this.content = in.readString();
    this.complete = in.readByte() != 0;
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeString(this.type);
    dest.writeString(this.content);
    dest.writeByte(this.complete ? (byte) 1 : (byte) 0);
  }

  @Override
  public String toString() {
    return "MsgBlock{" +
        "type='" + type + '\'' +
        ", content='" + content + '\'' +
        ", complete=" + complete +
        '}';
  }

  public String getType() {
    return type;
  }

  public String getContent() {
    return content;
  }

  public boolean isComplete() {
    return complete;
  }
}
