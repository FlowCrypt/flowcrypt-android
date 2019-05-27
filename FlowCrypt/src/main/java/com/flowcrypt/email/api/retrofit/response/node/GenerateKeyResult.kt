/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.response.node;

import android.os.Parcel;

import com.flowcrypt.email.api.retrofit.response.model.node.NodeKeyDetails;
import com.google.gson.annotations.Expose;

/**
 * It's a result for "generateKey" requests.
 *
 * @author Denis Bondarenko
 * Date: 4/1/19
 * Time: 9:44 AM
 * E-mail: DenBond7@gmail.com
 */
public class GenerateKeyResult extends BaseNodeResult {

  public static final Creator<GenerateKeyResult> CREATOR = new Creator<GenerateKeyResult>() {
    @Override
    public GenerateKeyResult createFromParcel(Parcel source) {
      return new GenerateKeyResult(source);
    }

    @Override
    public GenerateKeyResult[] newArray(int size) {
      return new GenerateKeyResult[size];
    }
  };

  @Expose
  private NodeKeyDetails key;

  public GenerateKeyResult() {
  }

  protected GenerateKeyResult(Parcel in) {
    super(in);
    this.key = in.readParcelable(NodeKeyDetails.class.getClassLoader());
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    super.writeToParcel(dest, flags);
    dest.writeParcelable(this.key, flags);
  }

  public NodeKeyDetails getKey() {
    return key;
  }
}
