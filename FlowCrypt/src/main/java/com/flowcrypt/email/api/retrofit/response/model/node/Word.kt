/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.response.model.node;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.Expose;

/**
 * @author Denis Bondarenko
 * Date: 4/1/19
 * Time: 3:31 PM
 * E-mail: DenBond7@gmail.com
 */
public class Word implements Parcelable {
  public static final Creator<Word> CREATOR = new Creator<Word>() {
    @Override
    public Word createFromParcel(Parcel source) {
      return new Word(source);
    }

    @Override
    public Word[] newArray(int size) {
      return new Word[size];
    }
  };

  @Expose
  private String match;

  @Expose
  private String word;

  @Expose
  private int bar;

  @Expose
  private String color;

  @Expose
  private boolean pass;

  public Word() {
  }

  protected Word(Parcel in) {
    this.match = in.readString();
    this.word = in.readString();
    this.bar = in.readInt();
    this.color = in.readString();
    this.pass = in.readByte() != 0;
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeString(this.match);
    dest.writeString(this.word);
    dest.writeInt(this.bar);
    dest.writeString(this.color);
    dest.writeByte(this.pass ? (byte) 1 : (byte) 0);
  }

  public String getMatch() {
    return match;
  }

  public String getWord() {
    return word;
  }

  public int getBar() {
    return bar;
  }

  public String getColor() {
    return color;
  }

  public boolean isPass() {
    return pass;
  }
}
