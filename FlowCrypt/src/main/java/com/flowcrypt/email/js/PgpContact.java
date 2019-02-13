/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.js;

import android.os.Parcel;
import android.os.Parcelable;

import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.V8Array;
import com.eclipsesource.v8.V8Object;

public class PgpContact implements Parcelable {

  public static final Creator<PgpContact> CREATOR = new Creator<PgpContact>() {
    @Override
    public PgpContact createFromParcel(Parcel source) {
      return new PgpContact(source);
    }

    @Override
    public PgpContact[] newArray(int size) {
      return new PgpContact[size];
    }
  };

  private String email;
  private String name;
  private String pubkey;
  private boolean hasPgp;
  private String client;
  private boolean attested;
  private String fingerprint;
  private String longid;
  private String keywords;
  private int lastUse;

  public PgpContact(String email, String name, String pubkey, Boolean hasPgp, String client,
                    Boolean attested, String fingerprint, String longid, String keywords,
                    Integer lastUse) {
    this.email = email;
    this.name = name;
    this.pubkey = pubkey;
    this.hasPgp = hasPgp;
    this.client = client;
    this.attested = attested;
    this.fingerprint = fingerprint;
    this.longid = longid;
    this.keywords = keywords;
    this.lastUse = lastUse;
  }

  public PgpContact(String email, String name) {
    this.email = email;
    this.name = name;
    this.pubkey = null;
    this.hasPgp = false;
    this.client = null;
    this.attested = false;
    this.fingerprint = null;
    this.longid = null;
    this.keywords = null;
    this.lastUse = 0;
  }

  protected PgpContact(Parcel in) {
    this.email = in.readString();
    this.name = in.readString();
    this.pubkey = in.readString();
    this.hasPgp = in.readByte() != 0;
    this.client = in.readString();
    this.attested = in.readByte() != 0;
    this.fingerprint = in.readString();
    this.longid = in.readString();
    this.keywords = in.readString();
    this.lastUse = in.readInt();
  }

  public static String arrayAsMime(PgpContact[] contacts) {
    String stringified = "";
    for (Integer i = 0; i < contacts.length; i++) {
      stringified += contacts[i].getMime() + (i < contacts.length - 1 ? "," : "");
    }
    return stringified;
  }

  public static V8Array arrayAsV8UserIds(V8 v8, PgpContact[] contacts) {
    V8Array userIds = new V8Array(v8);
    for (PgpContact contact : contacts) {
      userIds.push(new V8Object(v8).add("name", contact.getName()).add("email", contact.getEmail()));
    }
    return userIds;
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeString(this.email);
    dest.writeString(this.name);
    dest.writeString(this.pubkey);
    dest.writeByte(this.hasPgp ? (byte) 1 : (byte) 0);
    dest.writeString(this.client);
    dest.writeByte(this.attested ? (byte) 1 : (byte) 0);
    dest.writeString(this.fingerprint);
    dest.writeString(this.longid);
    dest.writeString(this.keywords);
    dest.writeInt(this.lastUse);
  }

  public String getEmail() {
    return this.email;
  }

  public String getName() {
    return name;
  }

  public String getPubkey() {
    return pubkey;
  }

  public void setPubkey(String pubkey) {
    this.pubkey = pubkey;
    if (pubkey != null) {
      this.hasPgp = true;
    }
  }

  public boolean getHasPgp() {
    return hasPgp;
  }

  public String getClient() {
    return client;
  }

  public void setClient(String client) {
    this.client = client;
  }

  public boolean getAttested() {
    return attested;
  }

  public void setAttested(boolean attested) {
    this.attested = attested;
  }

  public String getKeywords() {
    return keywords;
  }

  public String getLongid() {
    return longid;
  }

  public String getFingerprint() {
    return fingerprint;
  }

  public int getLastUse() {
    return lastUse;
  }

  public String getMime() {
    return MimeAddress.stringify(name, email);
  }
}
