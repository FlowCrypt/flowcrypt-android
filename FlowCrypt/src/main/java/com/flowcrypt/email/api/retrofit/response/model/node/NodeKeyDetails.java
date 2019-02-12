/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.response.model.node;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import com.flowcrypt.email.js.PgpContact;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

/**
 * @author Denis Bondarenko
 * Date: 2/11/19
 * Time: 1:23 PM
 * E-mail: DenBond7@gmail.com
 */
public class NodeKeyDetails implements Parcelable {

  public static final Creator<NodeKeyDetails> CREATOR = new Creator<NodeKeyDetails>() {
    @Override
    public NodeKeyDetails createFromParcel(Parcel source) {
      return new NodeKeyDetails(source);
    }

    @Override
    public NodeKeyDetails[] newArray(int size) {
      return new NodeKeyDetails[size];
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

  public NodeKeyDetails() {
  }

  protected NodeKeyDetails(Parcel in) {
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

  public PgpContact getPgpContact() {
    KeyId keyId = ids.get(0);
    String email = null;
    String name = null;
    try {
      InternetAddress[] internetAddresses = InternetAddress.parse(users.get(0));
      email = internetAddresses[0].getAddress();
      name = internetAddresses[0].getPersonal();
    } catch (AddressException e) {
      e.printStackTrace();
    }

    return new PgpContact(email, name, publicKey, true, null, false,
        keyId.getFingerprint(), keyId.getLongId(), keyId.getKeywords(), 0);
  }

  public String getLongId() {
    return ids.get(0).getLongId();
  }

  public String getFingerprint() {
    return ids.get(0).getFingerprint();
  }

  public String getKeywords() {
    return ids.get(0).getKeywords();
  }

  public boolean isPrivate() {
    return !TextUtils.isEmpty(privateKey);
  }
}
