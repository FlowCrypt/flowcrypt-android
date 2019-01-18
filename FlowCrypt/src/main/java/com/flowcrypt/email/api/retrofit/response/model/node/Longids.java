package com.flowcrypt.email.api.retrofit.response.model.node;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.Expose;

import java.util.List;

/**
 * @author Denis Bondarenko
 * Date: 1/18/19
 * Time: 10:05 AM
 * E-mail: DenBond7@gmail.com
 */
public class Longids implements Parcelable {
  public static final Creator<Longids> CREATOR = new Creator<Longids>() {
    @Override
    public Longids createFromParcel(Parcel source) {
      return new Longids(source);
    }

    @Override
    public Longids[] newArray(int size) {
      return new Longids[size];
    }
  };
  @Expose
  private List<String> message;

  @Expose
  private List<String> matching;

  @Expose
  private List<String> chosen;

  @Expose
  private List<String> needPassphrase;

  public Longids() {
  }

  protected Longids(Parcel in) {
    this.message = in.createStringArrayList();
    this.matching = in.createStringArrayList();
    this.chosen = in.createStringArrayList();
    this.needPassphrase = in.createStringArrayList();
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeStringList(this.message);
    dest.writeStringList(this.matching);
    dest.writeStringList(this.chosen);
    dest.writeStringList(this.needPassphrase);
  }

  public List<String> getMessage() {
    return message;
  }

  public List<String> getMatching() {
    return matching;
  }

  public List<String> getChosen() {
    return chosen;
  }

  public List<String> getNeedPassphrase() {
    return needPassphrase;
  }
}
