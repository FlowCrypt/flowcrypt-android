/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.response.model.node;

import android.os.Parcel;

import com.google.gson.annotations.Expose;

/**
 * @author Denis Bondarenko
 * Date: 3/26/19
 * Time: 3:02 PM
 * E-mail: DenBond7@gmail.com
 */
public class DecryptErrorMsgBlock extends MsgBlock {
  public static final Creator<DecryptErrorMsgBlock> CREATOR = new Creator<DecryptErrorMsgBlock>() {
    @Override
    public DecryptErrorMsgBlock createFromParcel(Parcel source) {
      return new DecryptErrorMsgBlock(source, Type.DECRYPT_ERROR);
    }

    @Override
    public DecryptErrorMsgBlock[] newArray(int size) {
      return new DecryptErrorMsgBlock[size];
    }
  };

  @Expose
  private DecryptError decryptErr;

  public DecryptErrorMsgBlock() {
  }

  protected DecryptErrorMsgBlock(Parcel in, Type type) {
    super(in, type);
    this.decryptErr = in.readParcelable(DecryptError.class.getClassLoader());
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    super.writeToParcel(dest, flags);
    dest.writeParcelable(this.decryptErr, flags);
  }

  public DecryptError getError() {
    return decryptErr;
  }
}
