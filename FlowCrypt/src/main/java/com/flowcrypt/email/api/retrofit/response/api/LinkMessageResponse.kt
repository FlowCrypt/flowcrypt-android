/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.response.api;

import android.os.Parcel;

import com.flowcrypt.email.api.retrofit.response.base.ApiError;
import com.flowcrypt.email.api.retrofit.response.base.ApiResponse;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import org.jetbrains.annotations.NotNull;

/**
 * This class describes a response from the https://flowcrypt.com/api/link/message API.
 * <p>
 * <code>POST /initial/confirm
 * response(200): {
 * "url" (<type 'str'>, None)  # url of the message, or None if not found
 * "repliable" (True, False, None)  # this message may be available for a reply
 * }</code>
 *
 * @author Denis Bondarenko
 * Date: 13.07.2017
 * Time: 15:16
 * E-mail: DenBond7@gmail.com
 */

public class LinkMessageResponse implements ApiResponse {

  public static final Creator<LinkMessageResponse> CREATOR = new Creator<LinkMessageResponse>() {
    @Override
    public LinkMessageResponse createFromParcel(Parcel source) {
      return new LinkMessageResponse(source);
    }

    @Override
    public LinkMessageResponse[] newArray(int size) {
      return new LinkMessageResponse[size];
    }
  };

  @SerializedName("error")
  @Expose
  private ApiError apiError;

  @Expose
  private String url;

  @Expose
  private boolean deleted;

  @Expose
  private String expire;

  @Expose
  private boolean expired;

  @Expose
  private Boolean repliable;

  public LinkMessageResponse() {
  }

  protected LinkMessageResponse(Parcel in) {
    this.apiError = in.readParcelable(ApiError.class.getClassLoader());
    this.url = in.readString();
    this.deleted = in.readByte() != 0;
    this.expire = in.readString();
    this.expired = in.readByte() != 0;
    this.repliable = (Boolean) in.readValue(Boolean.class.getClassLoader());
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeParcelable(this.apiError, flags);
    dest.writeString(this.url);
    dest.writeByte(this.deleted ? (byte) 1 : (byte) 0);
    dest.writeString(this.expire);
    dest.writeByte(this.expired ? (byte) 1 : (byte) 0);
    dest.writeValue(this.repliable);
  }

  public String getUrl() {
    return url;
  }

  public boolean isDeleted() {
    return deleted;
  }

  public String getExpire() {
    return expire;
  }

  public boolean isExpired() {
    return expired;
  }

  public Boolean getRepliable() {
    return repliable;
  }

  @NotNull
  @Override
  public ApiError getApiError() {
    return apiError;
  }
}
