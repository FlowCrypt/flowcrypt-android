/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.model;

import android.os.Parcel;
import android.os.Parcelable;

import com.flowcrypt.email.js.PgpContact;

/**
 * This class describes information about some public key.
 *
 * @author Denis Bondarenko
 * Date: 13.05.2018
 * Time: 10:22
 * E-mail: DenBond7@gmail.com
 */
public class PublicKeyInfo implements Parcelable {
  public static final Parcelable.Creator<PublicKeyInfo> CREATOR = new Parcelable.Creator<PublicKeyInfo>() {
    @Override
    public PublicKeyInfo createFromParcel(Parcel source) {
      return new PublicKeyInfo(source);
    }

    @Override
    public PublicKeyInfo[] newArray(int size) {
      return new PublicKeyInfo[size];
    }
  };

  private String keyWords;
  private String fingerprint;
  private String keyOwner;
  private String longId;
  private PgpContact pgpContact;
  private String publicKey;


  public PublicKeyInfo(String keyWords, String fingerprint, String keyOwner,
                       String longId, PgpContact pgpContact, String publicKey) {
    this.keyWords = keyWords;
    this.fingerprint = fingerprint;
    this.keyOwner = keyOwner;
    this.longId = longId;
    this.pgpContact = pgpContact;
    this.publicKey = publicKey;
  }

  protected PublicKeyInfo(Parcel in) {
    this.keyWords = in.readString();
    this.fingerprint = in.readString();
    this.keyOwner = in.readString();
    this.longId = in.readString();
    this.pgpContact = in.readParcelable(PgpContact.class.getClassLoader());
    this.publicKey = in.readString();
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeString(this.keyWords);
    dest.writeString(this.fingerprint);
    dest.writeString(this.keyOwner);
    dest.writeString(this.longId);
    dest.writeParcelable(this.pgpContact, flags);
    dest.writeString(this.publicKey);
  }

  public String getKeyWords() {
    return keyWords;
  }

  public String getFingerprint() {
    return fingerprint;
  }

  public String getKeyOwner() {
    return keyOwner;
  }

  public String getLongId() {
    return longId;
  }

  public PgpContact getPgpContact() {
    return pgpContact;
  }

  public void setPgpContact(PgpContact pgpContact) {
    this.pgpContact = pgpContact;
  }

  public String getPublicKey() {
    return publicKey;
  }

  public boolean isPgpContactExists() {
    return pgpContact != null;
  }

  public boolean isPgpContactCanBeUpdated() {
    return pgpContact != null && (pgpContact.getLongid() == null || !pgpContact.getLongid().equals(longId));
  }
}
