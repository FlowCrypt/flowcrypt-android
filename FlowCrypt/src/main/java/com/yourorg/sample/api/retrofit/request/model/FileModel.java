package com.yourorg.sample.api.retrofit.request.model;

import android.os.Parcel;

import com.google.gson.annotations.Expose;

import java.util.ArrayList;

/**
 * @author Denis Bondarenko
 * Date: 12/21/18
 * Time: 9:22 AM
 * E-mail: DenBond7@gmail.com
 */
public class FileModel extends Pubkeys {
  public static final Creator<FileModel> CREATOR = new Creator<FileModel>() {
    @Override
    public FileModel createFromParcel(Parcel source) {
      return new FileModel(source);
    }

    @Override
    public FileModel[] newArray(int size) {
      return new FileModel[size];
    }
  };
  @Expose
  private String name;

  public FileModel(ArrayList<String> pubKeys, String name) {
    super(pubKeys);
    this.name = name;
  }

  public FileModel(String[] pubKeys, String name) {
    super(pubKeys);
    this.name = name;
  }

  protected FileModel(Parcel in) {
    super(in);
    this.name = in.readString();
  }

  public String getName() {
    return name;
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    super.writeToParcel(dest, flags);
    dest.writeString(this.name);
  }
}
