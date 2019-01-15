package com.flowcrypt.email.api.retrofit.response.node;

import android.os.Parcel;
import android.os.Parcelable;

import com.flowcrypt.email.api.retrofit.response.model.node.ServerError;
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

  private long time;

  @SerializedName("error")
  @Expose
  private ServerError serverError;

  BaseNodeResult() {
  }

  protected BaseNodeResult(Parcel in) {
    this.time = in.readLong();
    this.serverError = in.readParcelable(ServerError.class.getClassLoader());
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
    dest.writeLong(this.time);
    dest.writeParcelable(this.serverError, flags);
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

  public long getTime() {
    return time;
  }

  @Override
  public void setTime(long time) {
    this.time = time;
  }

  public ServerError getServerError() {
    return serverError;
  }
}
