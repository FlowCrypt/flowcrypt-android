/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.model;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Simple POJO class which describes a dialog item.
 *
 * @author Denis Bondarenko
 * Date: 01.08.2017
 * Time: 11:29
 * E-mail: DenBond7@gmail.com
 */

public class DialogItem implements Parcelable {
  public static final Creator<DialogItem> CREATOR = new
      Creator<DialogItem>() {
        @Override
        public DialogItem createFromParcel(Parcel source) {
          return new DialogItem(source);
        }

        @Override
        public DialogItem[] newArray(int size) {
          return new DialogItem[size];
        }
      };

  private int iconResId;
  private String title;
  private int id;

  public DialogItem() {
  }

  public DialogItem(int iconResId, String title, int id) {
    this.iconResId = iconResId;
    this.title = title;
    this.id = id;
  }

  protected DialogItem(Parcel in) {
    this.iconResId = in.readInt();
    this.title = in.readString();
    this.id = in.readInt();
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeInt(this.iconResId);
    dest.writeString(this.title);
    dest.writeInt(this.id);
  }

  public int getIconResourceId() {
    return iconResId;
  }

  public String getTitle() {
    return title;
  }

  public int getId() {
    return id;
  }
}
