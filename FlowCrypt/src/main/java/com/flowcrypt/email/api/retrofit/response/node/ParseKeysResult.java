/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.response.node;

import android.os.Parcel;

import com.flowcrypt.email.api.retrofit.response.model.node.KeyDetails;
import com.google.gson.annotations.Expose;

import java.util.List;

/**
 * @author Denis Bondarenko
 * Date: 2/11/19
 * Time: 12:01 PM
 * E-mail: DenBond7@gmail.com
 */
public class ParseKeysResult extends BaseNodeResult {
  public static final Creator<ParseKeysResult> CREATOR = new Creator<ParseKeysResult>() {
    @Override
    public ParseKeysResult createFromParcel(Parcel source) {
      return new ParseKeysResult(source);
    }

    @Override
    public ParseKeysResult[] newArray(int size) {
      return new ParseKeysResult[size];
    }
  };

  @Expose
  private String format;

  @Expose
  private List<KeyDetails> keyDetails;

  public ParseKeysResult() {
  }

  protected ParseKeysResult(Parcel in) {
    super(in);
    this.format = in.readString();
    this.keyDetails = in.createTypedArrayList(KeyDetails.CREATOR);
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    super.writeToParcel(dest, flags);
    dest.writeString(this.format);
    dest.writeTypedList(this.keyDetails);
  }

  public String getFormat() {
    return format;
  }

  public List<KeyDetails> getKeyDetails() {
    return keyDetails;
  }
}
