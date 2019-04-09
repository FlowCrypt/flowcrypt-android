/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.response.node;

import android.os.Parcel;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * It's a result for "composeEmail" requests.
 *
 * @author Denis Bondarenko
 * Date: 3/27/19
 * Time: 3:00 PM
 * E-mail: DenBond7@gmail.com
 */
public class ComposeEmailResult extends BaseNodeResult {
  public static final Creator<ComposeEmailResult> CREATOR = new Creator<ComposeEmailResult>() {
    @Override
    public ComposeEmailResult createFromParcel(Parcel source) {
      return new ComposeEmailResult(source);
    }

    @Override
    public ComposeEmailResult[] newArray(int size) {
      return new ComposeEmailResult[size];
    }
  };

  public ComposeEmailResult() {
  }

  protected ComposeEmailResult(Parcel in) {
    super(in);
  }

  public final String getMimeMsg() {
    byte[] bytes = getData();

    if (bytes == null) {
      return "";
    }
    try {
      return IOUtils.toString(bytes, StandardCharsets.UTF_8.displayName());
    } catch (IOException e) {
      e.printStackTrace();
    }
    return "";
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    super.writeToParcel(dest, flags);
  }
}
