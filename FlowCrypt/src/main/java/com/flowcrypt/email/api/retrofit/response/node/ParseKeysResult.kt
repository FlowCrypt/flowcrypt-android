/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.response.node;

import android.os.Parcel;

import com.flowcrypt.email.api.retrofit.response.model.node.NodeKeyDetails;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * It's a result for "parseKeys" requests.
 *
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
  @SerializedName("keyDetails")
  private List<NodeKeyDetails> nodeKeyDetails;

  public ParseKeysResult() {
  }

  protected ParseKeysResult(Parcel in) {
    super(in);
    this.format = in.readString();
    this.nodeKeyDetails = in.createTypedArrayList(NodeKeyDetails.CREATOR);
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    super.writeToParcel(dest, flags);
    dest.writeString(this.format);
    dest.writeTypedList(this.nodeKeyDetails);
  }

  public String getFormat() {
    return format;
  }

  public List<NodeKeyDetails> getNodeKeyDetails() {
    return nodeKeyDetails;
  }
}
