/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.response.node;

import android.os.Parcel;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * It's a result for "version" requests.
 *
 * @author Denis Bondarenko
 * Date: 1/11/19
 * Time: 11:42 AM
 * E-mail: DenBond7@gmail.com
 */
public class VersionResult extends BaseNodeResult {

  public static final Creator<VersionResult> CREATOR = new Creator<VersionResult>() {
    @Override
    public VersionResult createFromParcel(Parcel source) {
      return new VersionResult(source);
    }

    @Override
    public VersionResult[] newArray(int size) {
      return new VersionResult[size];
    }
  };

  @SerializedName("http_parser")
  @Expose
  private String httpParser;
  @SerializedName("mobile")
  @Expose
  private String mobile;
  @SerializedName("node")
  @Expose
  private String node;
  @SerializedName("v8")
  @Expose
  private String v8;
  @SerializedName("uv")
  @Expose
  private String uv;
  @SerializedName("zlib")
  @Expose
  private String zlib;
  @SerializedName("ares")
  @Expose
  private String ares;
  @SerializedName("modules")
  @Expose
  private String modules;
  @SerializedName("nghttp2")
  @Expose
  private String nghttp2;
  @SerializedName("openssl")
  @Expose
  private String openssl;

  public VersionResult() {
  }

  protected VersionResult(Parcel in) {
    super(in);
    this.httpParser = in.readString();
    this.mobile = in.readString();
    this.node = in.readString();
    this.v8 = in.readString();
    this.uv = in.readString();
    this.zlib = in.readString();
    this.ares = in.readString();
    this.modules = in.readString();
    this.nghttp2 = in.readString();
    this.openssl = in.readString();
  }

  @Override
  public String toString() {
    return "VersionResult{" +
        "httpParser='" + httpParser + '\'' +
        ", mobile='" + mobile + '\'' +
        ", node='" + node + '\'' +
        ", v8='" + v8 + '\'' +
        ", uv='" + uv + '\'' +
        ", zlib='" + zlib + '\'' +
        ", ares='" + ares + '\'' +
        ", modules='" + modules + '\'' +
        ", nghttp2='" + nghttp2 + '\'' +
        ", openssl='" + openssl + '\'' +
        "} " + super.toString();
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    super.writeToParcel(dest, flags);
    dest.writeString(this.httpParser);
    dest.writeString(this.mobile);
    dest.writeString(this.node);
    dest.writeString(this.v8);
    dest.writeString(this.uv);
    dest.writeString(this.zlib);
    dest.writeString(this.ares);
    dest.writeString(this.modules);
    dest.writeString(this.nghttp2);
    dest.writeString(this.openssl);
  }

  public String getHttpParser() {
    return httpParser;
  }

  public String getMobile() {
    return mobile;
  }

  public String getNode() {
    return node;
  }

  public String getV8() {
    return v8;
  }

  public String getUv() {
    return uv;
  }

  public String getZlib() {
    return zlib;
  }

  public String getAres() {
    return ares;
  }

  public String getModules() {
    return modules;
  }

  public String getNghttp2() {
    return nghttp2;
  }

  public String getOpenssl() {
    return openssl;
  }
}
