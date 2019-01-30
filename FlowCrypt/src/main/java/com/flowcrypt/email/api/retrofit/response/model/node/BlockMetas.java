/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.response.model.node;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.Expose;

/**
 * @author Denis Bondarenko
 * Date: 1/11/19
 * Time: 4:08 PM
 * E-mail: DenBond7@gmail.com
 */
public class BlockMetas implements Parcelable {
  public static final Creator<BlockMetas> CREATOR = new Creator<BlockMetas>() {
    @Override
    public BlockMetas createFromParcel(Parcel source) {
      return new BlockMetas(source);
    }

    @Override
    public BlockMetas[] newArray(int size) {
      return new BlockMetas[size];
    }
  };

  @Expose
  private String type;

  @Expose
  private int length;

  public BlockMetas(String type, int length) {
    this.type = type;
    this.length = length;
  }

  protected BlockMetas(Parcel in) {
    this.type = in.readString();
    this.length = in.readInt();
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeString(this.type);
    dest.writeInt(this.length);
  }

  public String getType() {
    return type;
  }

  public int getLength() {
    return length;
  }
}
