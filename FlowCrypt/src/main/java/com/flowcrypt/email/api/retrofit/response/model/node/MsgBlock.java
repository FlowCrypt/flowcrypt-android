/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.response.model.node;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.Expose;

public class MsgBlock implements Parcelable {

  public static final String TYPE_PLAIN_TEXT = "plainText";
  public static final String TYPE_DECRYPTED_TEXT = "decryptedText";
  public static final String TYPE_PGP_ENCRYPTED_MSG = "encryptedMsg";
  public static final String TYPE_PGP_PUBLIC_KEY = "publicKey";
  public static final String TYPE_PGP_SIGNED_MSG = "signedMsg";
  public static final String TYPE_PGP_ENCRYPTED_MSG_LINK = "encryptedMsgLink";
  public static final String TYPE_ATTEST_PACKET = "attestPacket";
  public static final String TYPE_VERIFICATION = "cryptupVerification";
  public static final String TYPE_PGP_PRIVATE_KEY = "privateKey";
  public static final String TYPE_PLAIN_ATT = "plainAtt";
  public static final String TYPE_ENCRYPTED_ATT = "encryptedAtt";
  public static final String TYPE_DECRYPTED_ATT = "decryptedAtt";
  public static final String TYPE_ENCRYPTED_ATT_LINK = "encryptedAttLink";
  public static final String TYPE_PLAIN_HTML = "plainHtml";
  public static final String TYPE_DECRYPTED_HTML = "decryptedHtml";

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
