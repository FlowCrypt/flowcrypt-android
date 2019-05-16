/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.response.model.node;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.Objects;

import androidx.annotation.NonNull;

public class MsgBlock implements Parcelable {
  public static final String TAG_TYPE = "type";

  public static final Creator<MsgBlock> CREATOR = new Creator<MsgBlock>() {
    @Override
    public MsgBlock createFromParcel(Parcel source) {
      int tmp = source.readInt();
      Type partType = tmp == -1 ? null : Type.values()[tmp];

      if (partType != null) {
        return genMsgBlockFromType(source, partType);
      } else {
        return new MsgBlock(source, Type.UNKNOWN);
      }
    }

    @Override
    public MsgBlock[] newArray(int size) {
      return new MsgBlock[size];
    }
  };

  @Expose
  @SerializedName(TAG_TYPE)
  protected Type type;

  @Expose
  private String content;

  @Expose
  private boolean complete;

  public MsgBlock() {
  }

  public MsgBlock(Type type, String content, boolean complete) {
    this.type = type;
    this.content = content;
    this.complete = complete;
  }

  protected MsgBlock(Parcel in, Type type) {
    this.type = type;
    this.content = in.readString();
    this.complete = in.readByte() != 0;
  }

  @NonNull
  @Override
  public String toString() {
    return "MsgBlock{" +
        "type='" + type + '\'' +
        ", content='" + content + '\'' +
        ", complete=" + complete +
        '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    MsgBlock msgBlock = (MsgBlock) o;
    return complete == msgBlock.complete &&
        type == msgBlock.type &&
        Objects.equals(content, msgBlock.content);
  }

  @Override
  public int hashCode() {
    return Objects.hash(type, content, complete);
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeInt(this.type == null ? -1 : this.type.ordinal());
    dest.writeString(this.content);
    dest.writeByte(this.complete ? (byte) 1 : (byte) 0);
  }

  public Type getType() {
    return type;
  }

  public String getContent() {
    return content;
  }

  public boolean isComplete() {
    return complete;
  }

  private static MsgBlock genMsgBlockFromType(Parcel source, Type type) {
    switch (type) {
      case PUBLIC_KEY:
        return new PublicKeyMsgBlock(source, type);

      case DECRYPT_ERROR:
        return new DecryptErrorMsgBlock(source, type);

      default:
        return new BaseMsgBlock(source, type);
    }
  }

  public enum Type {
    UNKNOWN,

    @SerializedName("plainText")
    PLAIN_TEXT,

    @SerializedName("decryptedText")
    DECRYPTED_TEXT,

    @SerializedName("encryptedMsg")
    ENCRYPTED_MSG,

    @SerializedName("publicKey")
    PUBLIC_KEY,

    @SerializedName("signedMsg")
    SIGNED_MSG,

    @SerializedName("encryptedMsgLink")
    ENCRYPTED_MSG_LINK,

    @SerializedName("attestPacket")
    ATTEST_PACKET,

    @SerializedName("cryptupVerification")
    VERIFICATION,

    @SerializedName("privateKey")
    PRIVATE_KEY,

    @SerializedName("plainAtt")
    PLAIN_ATT,

    @SerializedName("encryptedAtt")
    ENCRYPTED_ATT,

    @SerializedName("decryptedAtt")
    DECRYPTED_ATT,

    @SerializedName("encryptedAttLink")
    ENCRYPTED_ATT_LINK,

    @SerializedName("plainHtml")
    PLAIN_HTML,

    @SerializedName("decryptedHtml")
    DECRYPTED_HTML,

    @SerializedName("decryptErr")
    DECRYPT_ERROR
  }
}
