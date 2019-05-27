/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.response.node;

import android.os.Parcel;

import com.google.gson.annotations.Expose;

/**
 * It's a result for "encryptKey" requests.
 *
 * @author Denis Bondarenko
 * Date: 2/18/19
 * Time: 9:28 AM
 * E-mail: DenBond7@gmail.com
 */
public class EncryptKeyResult extends BaseNodeResult {
  public static final Creator<DecryptKeyResult> CREATOR = new Creator<DecryptKeyResult>() {
    @Override
    public DecryptKeyResult createFromParcel(Parcel source) {
      return new DecryptKeyResult(source);
    }

    @Override
    public DecryptKeyResult[] newArray(int size) {
      return new DecryptKeyResult[size];
    }
  };

  @Expose
  private String encryptedKey;

  public EncryptKeyResult() {
  }

  protected EncryptKeyResult(Parcel in) {
    super(in);
    this.encryptedKey = in.readString();
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    super.writeToParcel(dest, flags);
    dest.writeString(this.encryptedKey);
  }

  public String getEncryptedKey() {
    return encryptedKey;
  }
}
