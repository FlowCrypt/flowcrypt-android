/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.response.node;

import android.os.Parcel;

import com.google.gson.annotations.Expose;

/**
 * It's a result for "decryptFile" requests.
 *
 * @author Denis Bondarenko
 * Date: 1/15/19
 * Time: 4:37 PM
 * E-mail: DenBond7@gmail.com
 */
public class DecryptedFileResult extends BaseNodeResult {
  public static final Creator<DecryptedFileResult> CREATOR = new Creator<DecryptedFileResult>() {
    @Override
    public DecryptedFileResult createFromParcel(Parcel source) {
      return new DecryptedFileResult(source);
    }

    @Override
    public DecryptedFileResult[] newArray(int size) {
      return new DecryptedFileResult[size];
    }
  };

  @Expose
  private boolean success;

  @Expose
  private String name;

  public DecryptedFileResult() {
  }

  protected DecryptedFileResult(Parcel in) {
    super(in);
    this.success = in.readByte() != 0;
    this.name = in.readString();
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    super.writeToParcel(dest, flags);
    dest.writeByte(this.success ? (byte) 1 : (byte) 0);
    dest.writeString(this.name);
  }

  public byte[] getDecryptedBytes() {
    return getData();
  }

  public boolean isSuccess() {
    return success;
  }

  public String getName() {
    return name;
  }
}
