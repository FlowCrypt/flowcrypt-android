/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.model;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import com.flowcrypt.email.api.email.EmailUtil;
import com.flowcrypt.email.util.RFC6068Parser;

import java.util.ArrayList;
import java.util.List;

/**
 * This class describes information about incoming extra info from the intent with one of next actions:
 * <ul>
 * <li>{@link Intent#ACTION_VIEW}</li>
 * <li>{@link Intent#ACTION_SENDTO}</li>
 * <li>{@link Intent#ACTION_SEND}</li>
 * <li>{@link Intent#ACTION_SEND_MULTIPLE}</li>
 * </ul>
 *
 * @author Denis Bondarenko
 * Date: 13.03.2018
 * Time: 16:16
 * E-mail: DenBond7@gmail.com
 */

public class ExtraActionInfo implements Parcelable {
  public static final Creator<ExtraActionInfo> CREATOR = new Creator<ExtraActionInfo>() {
    @Override
    public ExtraActionInfo createFromParcel(Parcel source) {
      return new ExtraActionInfo(source);
    }

    @Override
    public ExtraActionInfo[] newArray(int size) {
      return new ExtraActionInfo[size];
    }
  };
  private List<AttachmentInfo> attsList;
  private ArrayList<String> toAddresses;
  private ArrayList<String> ccAddresses;
  private ArrayList<String> bccAddresses;
  private String subject;
  private String body;

  public ExtraActionInfo() {
  }

  public ExtraActionInfo(List<AttachmentInfo> attsList, ArrayList<String> toAddresses, ArrayList<String> ccAddresses,
                         ArrayList<String> bccAddresses, String subject, String body) {
    this.attsList = attsList;
    this.toAddresses = toAddresses;
    this.ccAddresses = ccAddresses;
    this.bccAddresses = bccAddresses;
    this.subject = subject;
    this.body = body;
  }

  protected ExtraActionInfo(Parcel in) {
    this.attsList = in.createTypedArrayList(AttachmentInfo.CREATOR);
    this.toAddresses = in.createStringArrayList();
    this.ccAddresses = in.createStringArrayList();
    this.bccAddresses = in.createStringArrayList();
    this.subject = in.readString();
    this.body = in.readString();
  }

  /**
   * Parse an incoming information from the intent which has next actions:
   * <ul>
   * <li>{@link Intent#ACTION_VIEW}</li>
   * <li>{@link Intent#ACTION_SENDTO}</li>
   * <li>{@link Intent#ACTION_SEND}</li>
   * <li>{@link Intent#ACTION_SEND_MULTIPLE}</li>
   * </ul>
   *
   * @param intent An incoming intent.
   */
  public static ExtraActionInfo parseExtraActionInfo(Context context, Intent intent) {
    ExtraActionInfo info = null;

    //parse mailto: URI
    if (Intent.ACTION_VIEW.equals(intent.getAction()) || Intent.ACTION_SENDTO.equals(intent.getAction())) {
      if (intent.getData() != null) {
        Uri uri = intent.getData();
        if (RFC6068Parser.isMailTo(uri)) {
          info = RFC6068Parser.parse(uri);
        }
      }
    }

    if (info == null) {
      info = new ExtraActionInfo();
    }

    switch (intent.getAction()) {
      case Intent.ACTION_VIEW:
      case Intent.ACTION_SENDTO:
      case Intent.ACTION_SEND:
      case Intent.ACTION_SEND_MULTIPLE:

        CharSequence extraText = intent.getCharSequenceExtra(Intent.EXTRA_TEXT);
        // Only use EXTRA_TEXT if the body hasn't already been set by the mailto: URI
        if (extraText != null && TextUtils.isEmpty(info.getBody())) {
          info.setBody(extraText.toString());
        }

        String subj = intent.getStringExtra(Intent.EXTRA_SUBJECT);
        // Only use EXTRA_SUBJECT if the subject hasn't already been set by the mailto: URI
        if (subj != null && TextUtils.isEmpty(info.getSubject())) {
          info.setSubject(subj);
        }

        List<AttachmentInfo> attsList = new ArrayList<>();

        if (Intent.ACTION_SEND.equals(intent.getAction())) {
          Uri stream = intent.getParcelableExtra(Intent.EXTRA_STREAM);
          if (stream != null) {
            AttachmentInfo attachmentInfo = EmailUtil.getAttInfoFromUri(context, stream);
            attsList.add(attachmentInfo);
          }
        } else {
          List<Parcelable> uriList = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
          if (uriList != null) {
            for (Parcelable parcelable : uriList) {
              Uri uri = (Uri) parcelable;
              if (uri != null) {
                AttachmentInfo attachmentInfo = EmailUtil.getAttInfoFromUri(context, uri);
                attsList.add(attachmentInfo);
              }
            }
          }
        }

        info.setAtts(attsList);
        break;
    }

    return info;
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeTypedList(this.attsList);
    dest.writeStringList(this.toAddresses);
    dest.writeStringList(this.ccAddresses);
    dest.writeStringList(this.bccAddresses);
    dest.writeString(this.subject);
    dest.writeString(this.body);
  }

  public List<AttachmentInfo> getAtts() {
    return attsList;
  }

  public void setAtts(List<AttachmentInfo> attsList) {
    this.attsList = attsList;
  }

  public ArrayList<String> getToAddresses() {
    return toAddresses;
  }

  public void setToAddresses(ArrayList<String> toAddresses) {
    this.toAddresses = toAddresses;
  }

  public ArrayList<String> getCcAddresses() {
    return ccAddresses;
  }

  public void setCcAddresses(ArrayList<String> ccAddresses) {
    this.ccAddresses = ccAddresses;
  }

  public ArrayList<String> getBccAddresses() {
    return bccAddresses;
  }

  public void setBccAddresses(ArrayList<String> bccAddresses) {
    this.bccAddresses = bccAddresses;
  }

  public String getSubject() {
    return subject;
  }

  public void setSubject(String subject) {
    this.subject = subject;
  }

  public String getBody() {
    return body;
  }

  public void setBody(String body) {
    this.body = body;
  }

  public static class Builder {

    private List<AttachmentInfo> attachmentInfoList;
    private ArrayList<String> toAddresses;
    private ArrayList<String> ccAddresses;
    private ArrayList<String> bccAddresses;
    private String subject;
    private String body;
    private Parcel in;

    public Builder setAttInfoList(List<AttachmentInfo> attachmentInfoList) {
      this.attachmentInfoList = attachmentInfoList;
      return this;
    }

    public Builder setToAddresses(ArrayList<String> toAddresses) {
      this.toAddresses = toAddresses;
      return this;
    }

    public Builder setCcAddresses(ArrayList<String> ccAddresses) {
      this.ccAddresses = ccAddresses;
      return this;
    }

    public Builder setBccAddresses(ArrayList<String> bccAddresses) {
      this.bccAddresses = bccAddresses;
      return this;
    }

    public Builder setSubject(String subject) {
      this.subject = subject;
      return this;
    }

    public Builder setBody(String body) {
      this.body = body;
      return this;
    }

    public Builder setIn(Parcel in) {
      this.in = in;
      return this;
    }

    public ExtraActionInfo create() {
      return new ExtraActionInfo(attachmentInfoList, toAddresses, ccAddresses, bccAddresses, subject, body);
    }
  }
}
