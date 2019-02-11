/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.response.model.node;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * @author Denis Bondarenko
 * Date: 2/11/19
 * Time: 1:23 PM
 * E-mail: DenBond7@gmail.com
 */
public class KeyDetails implements Parcelable {

  public static final Creator<KeyDetails> CREATOR = new Creator<KeyDetails>() {
    @Override
    public KeyDetails createFromParcel(Parcel source) {
      return new KeyDetails(source);
    }

    @Override
    public KeyDetails[] newArray(int size) {
      return new KeyDetails[size];
    }
  };

  @Expose
  @SerializedName("private")
  private String privateKey;

  @Expose
  @SerializedName("public")
  private String publicKey;

  @Expose
  private List<String> users;

  @Expose
  private List<KeyId> ids;

  @Expose
  private long created;

  @Expose
  private Algo algo;

  public KeyDetails() {
  }

  protected KeyDetails(Parcel in) {
    this.privateKey = in.readString();
    this.publicKey = in.readString();
    this.users = in.createStringArrayList();
    this.ids = in.createTypedArrayList(KeyId.CREATOR);
    this.created = in.readLong();
    this.algo = in.readParcelable(Algo.class.getClassLoader());
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeString(this.privateKey);
    dest.writeString(this.publicKey);
    dest.writeStringList(this.users);
    dest.writeTypedList(this.ids);
    dest.writeLong(this.created);
    dest.writeParcelable(this.algo, flags);
  }

  public String getPrivateKey() {
    return privateKey;
  }

  public String getPublicKey() {
    return publicKey;
  }

  public List<String> getUsers() {
    return users;
  }

  public List<KeyId> getIds() {
    return ids;
  }

  public long getCreated() {
    return created;
  }

  public Algo getAlgo() {
    return algo;
  }
}
