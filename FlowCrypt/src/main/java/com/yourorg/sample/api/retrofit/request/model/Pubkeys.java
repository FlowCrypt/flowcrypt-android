package com.yourorg.sample.api.retrofit.request.model;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.Expose;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author DenBond7
 */
public class Pubkeys implements Parcelable {

  public static final Creator<Pubkeys> CREATOR = new Creator<Pubkeys>() {
    @Override
    public Pubkeys createFromParcel(Parcel source) {
      return new Pubkeys(source);
    }

    @Override
    public Pubkeys[] newArray(int size) {
      return new Pubkeys[size];
    }
  };
  @Expose
  private List<String> pubKeys;

  public Pubkeys(ArrayList<String> pubKeys) {
    this.pubKeys = pubKeys;
  }


  public Pubkeys(String[] pubKeys) {
    this.pubKeys = Arrays.asList(pubKeys);
  }

  protected Pubkeys(Parcel in) {
    this.pubKeys = in.createStringArrayList();
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeStringList(this.pubKeys);
  }
}
