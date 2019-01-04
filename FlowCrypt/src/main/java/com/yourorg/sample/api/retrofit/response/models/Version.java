package com.yourorg.sample.api.retrofit.response.models;

import android.os.Parcel;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.yourorg.sample.api.retrofit.response.base.BaseResponse;

/**
 * @author DenBond7
 */
public class Version extends BaseResponse {
  public static final Creator<Version> CREATOR = new Creator<Version>() {
    @Override
    public Version createFromParcel(Parcel source) {
      return new Version(source);
    }

    @Override
    public Version[] newArray(int size) {
      return new Version[size];
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

  public Version() {
  }

  protected Version(Parcel in) {
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
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
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

  @Override
  public String toString() {
    return "Version{" +
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
}
