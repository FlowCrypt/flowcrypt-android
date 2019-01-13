package com.flowcrypt.email.api.retrofit.response.node;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * It's a base realization which contains a common logic for all results.
 *
 * @author Denis Bondarenko
 * Date: 1/9/19
 * Time: 5:51 PM
 * E-mail: DenBond7@gmail.com
 */
public class BaseNodeResult implements BaseNodeResponse, Parcelable {
  public static final Creator<BaseNodeResult> CREATOR = new Creator<BaseNodeResult>() {
    @Override
    public BaseNodeResult createFromParcel(Parcel source) {
      return new BaseNodeResult(source);
    }

    @Override
    public BaseNodeResult[] newArray(int size) {
      return new BaseNodeResult[size];
    }
  };
  protected byte[] data;

  BaseNodeResult() {
  }

  protected BaseNodeResult(Parcel in) {
    this.data = in.createByteArray();
  }

  final byte[] getData() {
    return data;
  }

  @Override
  public void setData(byte[] data) {
    this.data = data;
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeByteArray(this.data);
  }
}
