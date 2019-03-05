/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.response.node;

import android.os.Parcel;

import com.google.gson.annotations.Expose;

/**
 * It's a result for "gmailBackupSearch" requests.
 *
 * @author Denis Bondarenko
 * Date: 2/11/19
 * Time: 9:55 AM
 * E-mail: DenBond7@gmail.com
 */
public class GmailBackupSearchResult extends BaseNodeResult {
  public static final Creator<GmailBackupSearchResult> CREATOR = new Creator<GmailBackupSearchResult>() {
    @Override
    public GmailBackupSearchResult createFromParcel(Parcel source) {
      return new GmailBackupSearchResult(source);
    }

    @Override
    public GmailBackupSearchResult[] newArray(int size) {
      return new GmailBackupSearchResult[size];
    }
  };
  @Expose
  private String query;


  public GmailBackupSearchResult() {
  }

  protected GmailBackupSearchResult(Parcel in) {
    super(in);
    this.query = in.readString();
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    super.writeToParcel(dest, flags);
    dest.writeString(this.query);
  }

  public String getQuery() {
    return query;
  }
}
