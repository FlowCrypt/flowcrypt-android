package com.flowcrypt.email.api.retrofit.request.model.node;

import android.os.Parcel;
import android.os.Parcelable;

import com.flowcrypt.email.node.results.PgpKeyInfo;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author DenBond7
 */
public class DecryptModel implements Parcelable {

  public static final Creator<DecryptModel> CREATOR = new Creator<DecryptModel>() {
    @Override
    public DecryptModel createFromParcel(Parcel source) {
      return new DecryptModel(source);
    }

    @Override
    public DecryptModel[] newArray(int size) {
      return new DecryptModel[size];
    }
  };
  @SerializedName("keys")
  @Expose
  private List<PrivateKeyInfo> privateKeyInfo;
  @SerializedName("passphrases")
  @Expose
  private List<String> passphrases;
  @Expose
  private String msgPwd;


  public DecryptModel(PgpKeyInfo[] prvKeys, String[] passphrases, String msgPwd) {
    this.privateKeyInfo = new ArrayList<>();

    for (PgpKeyInfo pgpKeyInfo : prvKeys) {
      privateKeyInfo.add(new PrivateKeyInfo(pgpKeyInfo.getPrivate(), pgpKeyInfo.getLongid()));
    }

    this.passphrases = Arrays.asList(passphrases);
    this.msgPwd = msgPwd;
  }

  protected DecryptModel(Parcel in) {
    this.privateKeyInfo = in.createTypedArrayList(PrivateKeyInfo.CREATOR);
    this.passphrases = in.createStringArrayList();
    this.msgPwd = in.readString();
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeTypedList(this.privateKeyInfo);
    dest.writeStringList(this.passphrases);
    dest.writeString(this.msgPwd);
  }
}
