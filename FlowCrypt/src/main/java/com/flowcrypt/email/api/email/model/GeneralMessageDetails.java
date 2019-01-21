/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.model;

import android.os.Parcel;
import android.os.Parcelable;

import com.flowcrypt.email.database.MessageState;

import java.util.Arrays;
import java.util.Objects;

import javax.mail.internet.InternetAddress;

/**
 * Simple POJO class which describe a general message details.
 *
 * @author DenBond7
 * Date: 28.04.2017
 * Time: 11:51
 * E-mail: DenBond7@gmail.com
 */

public class GeneralMessageDetails implements Parcelable {

  public static final Creator<GeneralMessageDetails> CREATOR = new Creator<GeneralMessageDetails>() {
    @Override
    public GeneralMessageDetails createFromParcel(Parcel source) {
      return new GeneralMessageDetails(source);
    }

    @Override
    public GeneralMessageDetails[] newArray(int size) {
      return new GeneralMessageDetails[size];
    }
  };
  private String email;
  private String label;
  private int uid;
  private long receivedDateInMillisecond;
  private long sentDateInMillisecond;
  private InternetAddress[] from;
  private InternetAddress[] to;
  private InternetAddress[] cc;
  private String subject;
  private String[] flags;
  private String rawMsgWithoutAtts;
  private boolean hasAtts;
  private boolean isEncrypted;
  private MessageState msgState;
  private String attsDir;
  private String errorMsg;

  public GeneralMessageDetails() {
  }

  protected GeneralMessageDetails(Parcel in) {
    this.email = in.readString();
    this.label = in.readString();
    this.uid = in.readInt();
    this.receivedDateInMillisecond = in.readLong();
    this.sentDateInMillisecond = in.readLong();
    this.from = (InternetAddress[]) in.readSerializable();
    this.to = (InternetAddress[]) in.readSerializable();
    this.cc = (InternetAddress[]) in.readSerializable();
    this.subject = in.readString();
    this.flags = in.createStringArray();
    this.rawMsgWithoutAtts = in.readString();
    this.hasAtts = in.readByte() != 0;
    this.isEncrypted = in.readByte() != 0;
    int tmpMsgState = in.readInt();
    this.msgState = tmpMsgState == -1 ? null : MessageState.values()[tmpMsgState];
    this.attsDir = in.readString();
    this.errorMsg = in.readString();
  }

  @Override
  public String toString() {
    return "GeneralMessageDetails{" +
        "email='" + email + '\'' +
        ", label='" + label + '\'' +
        ", uid=" + uid +
        ", receivedDateInMillisecond=" + receivedDateInMillisecond +
        ", sentDateInMillisecond=" + sentDateInMillisecond +
        ", from=" + Arrays.toString(from) +
        ", to=" + Arrays.toString(to) +
        ", cc=" + Arrays.toString(cc) +
        ", subject='" + subject + '\'' +
        ", flags=" + Arrays.toString(flags) +
        ", rawMessageWithoutAtts='" + rawMsgWithoutAtts + '\'' +
        ", hasAtts=" + hasAtts +
        ", isEncrypted=" + isEncrypted +
        ", msgState=" + msgState +
        ", attsDir=" + attsDir +
        '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    GeneralMessageDetails that = (GeneralMessageDetails) o;
    return uid == that.uid &&
        receivedDateInMillisecond == that.receivedDateInMillisecond &&
        sentDateInMillisecond == that.sentDateInMillisecond &&
        hasAtts == that.hasAtts &&
        isEncrypted == that.isEncrypted &&
        Objects.equals(email, that.email) &&
        Objects.equals(label, that.label) &&
        Arrays.equals(from, that.from) &&
        Arrays.equals(to, that.to) &&
        Arrays.equals(cc, that.cc) &&
        Objects.equals(subject, that.subject) &&
        Arrays.equals(flags, that.flags) &&
        Objects.equals(rawMsgWithoutAtts, that.rawMsgWithoutAtts) &&
        msgState == that.msgState &&
        Objects.equals(attsDir, that.attsDir) &&
        Objects.equals(errorMsg, that.errorMsg);
  }

  @Override
  public int hashCode() {
    int result = Objects.hash(email, label, uid, receivedDateInMillisecond, sentDateInMillisecond, subject,
        rawMsgWithoutAtts, hasAtts, isEncrypted, msgState, attsDir,
        errorMsg);
    result = 31 * result + Arrays.hashCode(from);
    result = 31 * result + Arrays.hashCode(to);
    result = 31 * result + Arrays.hashCode(cc);
    result = 31 * result + Arrays.hashCode(flags);
    return result;
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeString(this.email);
    dest.writeString(this.label);
    dest.writeInt(this.uid);
    dest.writeLong(this.receivedDateInMillisecond);
    dest.writeLong(this.sentDateInMillisecond);
    dest.writeSerializable(this.from);
    dest.writeSerializable(this.to);
    dest.writeSerializable(this.cc);
    dest.writeString(this.subject);
    dest.writeStringArray(this.flags);
    dest.writeString(this.rawMsgWithoutAtts);
    dest.writeByte(this.hasAtts ? (byte) 1 : (byte) 0);
    dest.writeByte(this.isEncrypted ? (byte) 1 : (byte) 0);
    dest.writeInt(this.msgState == null ? -1 : this.msgState.ordinal());
    dest.writeString(this.attsDir);
    dest.writeString(this.errorMsg);
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public int getUid() {
    return uid;
  }

  public void setUid(int uid) {
    this.uid = uid;
  }

  public long getReceivedDate() {
    return receivedDateInMillisecond;
  }

  public void setReceivedDateInMillisecond(long receivedDateInMillisecond) {
    this.receivedDateInMillisecond = receivedDateInMillisecond;
  }

  public long getSentDate() {
    return sentDateInMillisecond;
  }

  public void setSentDateInMillisecond(long sentDateInMillisecond) {
    this.sentDateInMillisecond = sentDateInMillisecond;
  }

  public InternetAddress[] getFrom() {
    return from;
  }

  public void setFrom(InternetAddress[] from) {
    this.from = from;
  }

  public InternetAddress[] getTo() {
    return to;
  }

  public void setTo(InternetAddress[] to) {
    this.to = to;
  }

  public InternetAddress[] getCc() {
    return cc;
  }

  public void setCc(InternetAddress[] cc) {
    this.cc = cc;
  }

  public String getSubject() {
    return subject;
  }

  public void setSubject(String subject) {
    this.subject = subject;
  }

  public String[] getFlags() {
    return flags;
  }

  public void setFlags(String[] flags) {
    this.flags = flags;
  }

  public boolean isSeen() {
    return flags != null && Arrays.asList(flags).contains(MessageFlag.SEEN);
  }

  public String getRawMsgWithoutAtts() {
    return rawMsgWithoutAtts;
  }

  public void setRawMsgWithoutAtts(String rawMsgWithoutAtts) {
    this.rawMsgWithoutAtts = rawMsgWithoutAtts;
  }

  public boolean hasAtts() {
    return hasAtts;
  }

  public void setHasAtts(boolean hasAtts) {
    this.hasAtts = hasAtts;
  }

  public boolean isEncrypted() {
    return isEncrypted;
  }

  public void setEncrypted(boolean encrypted) {
    isEncrypted = encrypted;
  }

  public MessageState getMsgState() {
    return msgState;
  }

  public void setMsgState(MessageState msgState) {
    this.msgState = msgState;
  }

  public String getAttsDir() {
    return attsDir;
  }

  public void setAttsDir(String attsDir) {
    this.attsDir = attsDir;
  }

  public String getErrorMsg() {
    return errorMsg;
  }

  public void setErrorMsg(String errorMsg) {
    this.errorMsg = errorMsg;
  }
}
