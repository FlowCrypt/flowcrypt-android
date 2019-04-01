/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.response.node;

import android.os.Parcel;
import android.os.Parcelable;

import com.flowcrypt.email.api.retrofit.response.model.node.Error;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import org.apache.commons.io.IOUtils;

import java.io.BufferedInputStream;
import java.io.IOException;

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

  private long executionTime;

  @SerializedName("error")
  @Expose
  private Error error;

  BaseNodeResult() {
  }

  protected BaseNodeResult(Parcel in) {
    this.executionTime = in.readLong();
    this.error = in.readParcelable(Error.class.getClassLoader());
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
    dest.writeLong(this.executionTime);
    dest.writeParcelable(this.error, flags);
  }

  /**
   * By default we'll parse raw data as a byte array.
   *
   * @param bufferedInputStream The input stream from the server
   * @throws IOException An error can occur during parsing.
   */
  @Override
  public void handleRawData(BufferedInputStream bufferedInputStream) throws IOException {
    data = IOUtils.toByteArray(bufferedInputStream);
  }

  public long getExecutionTime() {
    return executionTime;
  }

  @Override
  public void setExecutionTime(long executionTime) {
    this.executionTime = executionTime;
  }

  public Error getError() {
    return error;
  }
}
