/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.response.node;

import android.os.Parcel;

import com.flowcrypt.email.api.retrofit.response.model.node.Word;
import com.google.gson.annotations.Expose;

/**
 * It's a result for "zxcvbnStrengthBar" requests.
 *
 * @author Denis Bondarenko
 * Date: 4/1/19
 * Time: 3:06 PM
 * E-mail: DenBond7@gmail.com
 */
public class ZxcvbnStrengthBarResult extends BaseNodeResult {
  public static final Creator<ZxcvbnStrengthBarResult> CREATOR = new Creator<ZxcvbnStrengthBarResult>() {
    @Override
    public ZxcvbnStrengthBarResult createFromParcel(Parcel source) {
      return new ZxcvbnStrengthBarResult(source);
    }

    @Override
    public ZxcvbnStrengthBarResult[] newArray(int size) {
      return new ZxcvbnStrengthBarResult[size];
    }
  };
  @Expose
  private Word word;

  @Expose
  private long seconds;

  @Expose
  private String time;

  public ZxcvbnStrengthBarResult() {
  }

  protected ZxcvbnStrengthBarResult(Parcel in) {
    super(in);
    this.word = in.readParcelable(Word.class.getClassLoader());
    this.seconds = in.readLong();
    this.time = in.readString();
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    super.writeToParcel(dest, flags);
    dest.writeParcelable(this.word, flags);
    dest.writeLong(this.seconds);
    dest.writeString(this.time);
  }

  public String getTime() {
    return time;
  }

  public Word getWord() {
    return word;
  }

  public long getSeconds() {
    return seconds;
  }
}
