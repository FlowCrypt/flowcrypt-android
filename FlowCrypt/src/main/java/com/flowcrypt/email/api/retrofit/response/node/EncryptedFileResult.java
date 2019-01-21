/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.response.node;

import android.os.Parcel;

/**
 * It's a result for "encryptFile" requests.
 *
 * @author Denis Bondarenko
 * Date: 1/15/19
 * Time: 8:59 AM
 * E-mail: DenBond7@gmail.com
 */
public class EncryptedFileResult extends BaseNodeResult {
  public static final Creator<EncryptedFileResult> CREATOR = new Creator<EncryptedFileResult>() {
    @Override
    public EncryptedFileResult createFromParcel(Parcel source) {
      return new EncryptedFileResult(source);
    }

    @Override
    public EncryptedFileResult[] newArray(int size) {
      return new EncryptedFileResult[size];
    }
  };


  public EncryptedFileResult() {
  }

  protected EncryptedFileResult(Parcel in) {
    super(in);
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    super.writeToParcel(dest, flags);
  }

  public byte[] getEncryptedBytes() {
    return getData();
  }
}
