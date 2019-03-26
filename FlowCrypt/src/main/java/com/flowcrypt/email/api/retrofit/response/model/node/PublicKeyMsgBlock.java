/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.response.model.node;

import android.os.Parcel;

import com.google.gson.annotations.Expose;

/**
 * It's a variant of {@link MsgBlock} which describes a public key.
 *
 * @author Denis Bondarenko
 * Date: 3/25/19
 * Time: 2:35 PM
 * E-mail: DenBond7@gmail.com
 */
public class PublicKeyMsgBlock extends MsgBlock {

  public static final Creator<PublicKeyMsgBlock> CREATOR = new Creator<PublicKeyMsgBlock>() {
    @Override
    public PublicKeyMsgBlock createFromParcel(Parcel source) {
      return new PublicKeyMsgBlock(source, Type.PUBLIC_KEY);
    }

    @Override
    public PublicKeyMsgBlock[] newArray(int size) {
      return new PublicKeyMsgBlock[size];
    }
  };

  @Expose
  private NodeKeyDetails keyDetails;

  protected PublicKeyMsgBlock(Parcel in, Type type) {
    super(in, type);
    this.keyDetails = in.readParcelable(NodeKeyDetails.class.getClassLoader());
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    super.writeToParcel(dest, flags);
    dest.writeParcelable(this.keyDetails, flags);
  }

  public NodeKeyDetails getKeyDetails() {
    return keyDetails;
  }
}
